/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerHelper;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.model.Updates;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
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
	 * Create a profile change request that represents an update of the specified IUs to their latest versions,
	 * unless otherwise specified by the initial selections.  If an element root and selection container are provided, 
	 * update those elements so that a wizard could be opened on them to reflect the profile change request.
	 * 
	 * @param iusToUpdate
	 * @param profileId
	 * @param root
	 * @param initialSelections
	 * @param monitor
	 * @return the profile change request describing an update, or null if there is nothing to update.
	 */
	public static ProfileChangeRequest createProfileChangeRequest(IInstallableUnit[] iusToUpdate, String profileId, IUElementListRoot root, Collection initialSelections, IProgressMonitor monitor) {
		// Here we create a profile change request by finding the latest version available for any replacement, unless
		// otherwise specified in the selections.
		// We have to consider the scenario where the only updates available are patches, in which case the original
		// IU should not be removed as part of the update.
		Set toBeUpdated = new HashSet();
		HashSet elementsToPlan = new HashSet();
		ArrayList allReplacements = new ArrayList();
		IProfile profile;
		try {
			profile = ProvisioningUtil.getProfile(profileId);
			if (profile == null)
				return null;
		} catch (ProvisionException e) {
			return null;
		}
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.ProfileChangeRequestBuildingRequest, 100 * iusToUpdate.length);
		for (int i = 0; i < iusToUpdate.length; i++) {
			boolean selectionSpecified = false;
			ElementQueryDescriptor descriptor = Policy.getDefault().getQueryProvider().getQueryDescriptor(new Updates(profileId, new IInstallableUnit[] {iusToUpdate[i]}));
			Iterator iter = descriptor.performQuery(sub).iterator();
			ArrayList currentReplacements = new ArrayList();
			while (iter.hasNext()) {
				IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
				// If there is already a selected element representing an update for this iu, then we won't need
				// to look for the latest.
				if (iu != null) {
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=273967
					// In the case of patches, it's possible that a patch is returned as an available update
					// even though it is already installed, because we are querying each IU for updates individually.
					// For now, we ignore any proposed update that is already installed.
					Collector alreadyInstalled = profile.query(new InstallableUnitQuery(iu), new Collector(), null);
					if (alreadyInstalled.isEmpty()) {
						toBeUpdated.add(iusToUpdate[i]);
						AvailableUpdateElement element = new AvailableUpdateElement(root, iu, iusToUpdate[i], profileId, true);
						currentReplacements.add(element);
						allReplacements.add(element);
						if (initialSelections != null && initialSelections.contains(element)) {
							elementsToPlan.add(element);
							selectionSpecified = true;
						}
					}
				}
			}
			if (!selectionSpecified) {
				// If no selection was specified, we must figure out the latest version to apply.
				// The rules are that a true update will always win over a patch, but if only
				// patches are available, they should all be selected.
				// We first gather the latest versions of everything proposed.
				// Patches are keyed by their id because they are unique and should not be compared to
				// each other.  Updates are keyed by the IU they are updating so we can compare the
				// versions and select the latest one
				HashMap latestVersions = new HashMap();
				boolean foundUpdate = false;
				boolean foundPatch = false;
				for (int j = 0; j < currentReplacements.size(); j++) {
					AvailableUpdateElement replacementElement = (AvailableUpdateElement) currentReplacements.get(j);
					String key;
					if (Boolean.toString(true).equals(replacementElement.getIU().getProperty(IInstallableUnit.PROP_TYPE_PATCH))) {
						foundPatch = true;
						key = replacementElement.getIU().getId();
					} else {
						foundUpdate = true;
						key = replacementElement.getIUToBeUpdated().getId();
					}
					AvailableUpdateElement latestElement = (AvailableUpdateElement) latestVersions.get(key);
					IInstallableUnit latestIU = latestElement == null ? null : latestElement.getIU();
					if (latestIU == null || replacementElement.getIU().getVersion().compareTo(latestIU.getVersion()) > 0)
						latestVersions.put(key, replacementElement);
				}
				// If there is a true update available, ignore any patches found
				// Patches are keyed by their own id
				if (foundPatch && foundUpdate) {
					Set keys = new HashSet();
					keys.addAll(latestVersions.keySet());
					Iterator keyIter = keys.iterator();
					while (keyIter.hasNext()) {
						String id = (String) keyIter.next();
						// Get rid of things keyed by a different id.  We've already made sure
						// that updates with a different id are keyed under the original id
						if (!id.equals(iusToUpdate[i].getId())) {
							latestVersions.remove(id);
						}
					}
				}
				elementsToPlan.addAll(latestVersions.values());
			}
			sub.worked(100);
		}
		if (root != null)
			root.setChildren(allReplacements.toArray());

		if (toBeUpdated.size() <= 0 || elementsToPlan.isEmpty()) {
			sub.done();
			return null;
		}

		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(profileId);
		Iterator iter = elementsToPlan.iterator();
		while (iter.hasNext()) {
			AvailableUpdateElement element = (AvailableUpdateElement) iter.next();
			IInstallableUnit theUpdate = element.getIU();
			if (initialSelections != null) {
				if (!initialSelections.contains(element))
					initialSelections.add(element);
			}
			request.addInstallableUnits(new IInstallableUnit[] {theUpdate});
			request.setInstallableUnitProfileProperty(theUpdate, IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
			if (Boolean.toString(true).equals(theUpdate.getProperty(IInstallableUnit.PROP_TYPE_PATCH))) {
				request.setInstallableUnitInclusionRules(theUpdate, PlannerHelper.createOptionalInclusionRule(theUpdate));
			} else {
				request.removeInstallableUnits(new IInstallableUnit[] {element.getIUToBeUpdated()});
			}

		}
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
		ArrayList initialSelections = new ArrayList();
		initialSelections.addAll(Arrays.asList(selectedElements));
		return createProfileChangeRequest(getIUsToReplace(selectedElements), profileId, null, initialSelections, monitor);
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
			// TODO see https://bugs.eclipse.org/bugs/show_bug.cgi?id=276963
			IWizardPage page = getNextPage(mainPage);
			if (page != null)
				return page;
		}
		return mainPage;
	}
}
