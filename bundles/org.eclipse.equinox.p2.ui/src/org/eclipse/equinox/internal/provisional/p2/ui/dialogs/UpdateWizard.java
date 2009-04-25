/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.model.Updates;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Composite;

/**
 * @since 3.4
 */
public class UpdateWizard extends WizardWithLicenses {
	IInstallableUnit[] iusToReplace;
	QueryableMetadataRepositoryManager manager;
	SelectableIUsPage mainPage;
	boolean skipSelectionsPage = false;

	public static IInstallableUnit[] getIUsToReplace(Object[] replacementElements) {
		Set iusToReplace = new HashSet();
		for (int i = 0; i < replacementElements.length; i++) {
			if (replacementElements[i] instanceof AvailableUpdateElement) {
				iusToReplace.add(((AvailableUpdateElement) replacementElements[i]).getIUToBeUpdated());
			}
		}
		return (IInstallableUnit[]) iusToReplace.toArray(new IInstallableUnit[iusToReplace.size()]);
	}

	public static IInstallableUnit[] getReplacementIUs(Object[] replacementElements) {
		Set replacements = new HashSet();
		for (int i = 0; i < replacementElements.length; i++) {
			if (replacementElements[i] instanceof AvailableUpdateElement) {
				replacements.add(((AvailableUpdateElement) replacementElements[i]).getIU());
			}
		}
		return (IInstallableUnit[]) replacements.toArray(new IInstallableUnit[replacements.size()]);
	}

	/**
	 * Create a profile change request that represents an update of the specified IUs to their latest versions.
	 * If an element root and selection container are provided, update those elements so that a wizard could
	 * be opened on them to reflect the profile change request.
	 * 
	 * @param iusToUpdate
	 * @param profileId
	 * @param root
	 * @param initialSelections
	 * @param monitor
	 * @return the profile change request describing an update, or null if there is nothing to update.
	 */
	public static ProfileChangeRequest createProfileChangeRequest(IInstallableUnit[] iusToUpdate, String profileId, IUElementListRoot root, Collection initialSelections, IProgressMonitor monitor) {
		// Here we create a profile change request by finding the latest version available for any replacement.
		// We have to consider the scenario where the only updates available are patches, in which case the original
		// IU should not be removed as part of the update.
		ArrayList toBeUpdated = new ArrayList();
		HashMap latestReplacements = new HashMap();
		ArrayList allReplacements = new ArrayList();
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.ProfileChangeRequestBuildingRequest, 100 * iusToUpdate.length);
		for (int i = 0; i < iusToUpdate.length; i++) {
			ElementQueryDescriptor descriptor = Policy.getDefault().getQueryProvider().getQueryDescriptor(new Updates(profileId, new IInstallableUnit[] {iusToUpdate[i]}));
			Iterator iter = descriptor.performQuery(sub).iterator();
			if (iter.hasNext())
				toBeUpdated.add(iusToUpdate[i]);
			ArrayList currentReplacements = new ArrayList();
			while (iter.hasNext()) {
				IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
				if (iu != null) {
					AvailableUpdateElement element = new AvailableUpdateElement(root, iu, iusToUpdate[i], profileId, true);
					currentReplacements.add(element);
					allReplacements.add(element);
				}
			}
			// This loop gathers the latest version of all replacements by id.
			// It's possible that there are multiple latest replacements if patches (which have a different id than the original)
			// are involved.
			Set idsSeen = new HashSet();
			for (int j = 0; j < currentReplacements.size(); j++) {
				AvailableUpdateElement replacementElement = (AvailableUpdateElement) currentReplacements.get(j);
				idsSeen.add(replacementElement.getIU().getId());
				AvailableUpdateElement latestElement = (AvailableUpdateElement) latestReplacements.get(replacementElement.getIU().getId());
				IInstallableUnit latestIU = latestElement == null ? null : latestElement.getIU();
				if (latestIU == null || replacementElement.getIU().getVersion().compareTo(latestIU.getVersion()) > 0)
					latestReplacements.put(replacementElement.getIU().getId(), replacementElement);
			}
			// If there is a true update available, ignore any other ids seen (patches).
			// We know there are only patches available if there is no update with the same id as the replacement.
			if (idsSeen.contains(iusToUpdate[i].getId()) && idsSeen.size() > 1) {
				Iterator replacementIds = idsSeen.iterator();
				while (replacementIds.hasNext()) {
					String id = (String) replacementIds.next();
					if (id != iusToUpdate[i].getId())
						latestReplacements.remove(id);
				}
			}
			sub.worked(100);
		}
		if (root != null)
			root.setChildren(allReplacements.toArray());

		if (initialSelections != null)
			initialSelections.addAll(latestReplacements.values());

		if (toBeUpdated.size() <= 0) {
			sub.done();
			return null;
		}

		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(profileId);
		Iterator iter = toBeUpdated.iterator();
		while (iter.hasNext()) {
			IInstallableUnit iuToBeUpdated = (IInstallableUnit) iter.next();
			// Only remove it if there is a replacement with the same id.  
			if (latestReplacements.containsKey(iuToBeUpdated.getId()))
				request.removeInstallableUnits(new IInstallableUnit[] {iuToBeUpdated});
		}
		iter = latestReplacements.values().iterator();
		while (iter.hasNext())
			request.addInstallableUnits(new IInstallableUnit[] {((AvailableUpdateElement) iter.next()).getIU()});
		sub.done();
		return request;
	}

	public UpdateWizard(Policy policy, String profileId, IUElementListRoot root, Object[] initialSelections, PlannerResolutionOperation initialResolution, QueryableMetadataRepositoryManager manager) {
		super(policy, profileId, root, initialSelections, initialResolution);
		setWindowTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE));
		this.manager = manager;
	}

	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		mainPage = new SelectableIUsPage(policy, input, selections, profileId);
		mainPage.setTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		mainPage.setDescription(ProvUIMessages.UpdateAction_UpdatesAvailableMessage);
		mainPage.updateStatus(input, resolutionOperation);
		return mainPage;
	}

	protected ResolutionResultsWizardPage createResolutionPage() {
		return new UpdateWizardPage(policy, root, profileId, resolutionOperation);
	}

	protected IUElementListRoot makeResolutionElementRoot(Object[] selectedElements) {
		IUElementListRoot elementRoot = new IUElementListRoot();
		ArrayList list = new ArrayList(selectedElements.length);
		for (int i = 0; i < selectedElements.length; i++) {
			if (selectedElements[i] instanceof AvailableUpdateElement) {
				AvailableUpdateElement element = (AvailableUpdateElement) selectedElements[i];
				AvailableUpdateElement newElement = new AvailableUpdateElement(elementRoot, element.getIU(), element.getIUToBeUpdated(), profileId, policy.getQueryContext().getShowProvisioningPlanChildren());
				list.add(newElement);
			}
		}
		elementRoot.setChildren(list.toArray());
		return elementRoot;
	}

	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		if (manager != null)
			// async exec since we are in the middle of opening
			pageContainer.getDisplay().asyncExec(new Runnable() {
				public void run() {
					manager.reportAccumulatedStatus();
				}
			});
	}

	protected ProfileChangeRequest computeProfileChangeRequest(Object[] selectedElements, MultiStatus additionalStatus, IProgressMonitor monitor) {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(profileId);
		request.removeInstallableUnits(getIUsToReplace(selectedElements));
		request.addInstallableUnits(getReplacementIUs(selectedElements));
		return request;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard#getErrorReportingPage()
	 */
	protected IResolutionErrorReportingPage getErrorReportingPage() {
		return mainPage;
	}

	public void setSkipSelectionsPage(boolean skipSelectionsPage) {
		this.skipSelectionsPage = skipSelectionsPage;
	}

	public IWizardPage getStartingPage() {
		if (skipSelectionsPage) {
			return getNextPage(mainPage);
		}
		return mainPage;
	}
}
