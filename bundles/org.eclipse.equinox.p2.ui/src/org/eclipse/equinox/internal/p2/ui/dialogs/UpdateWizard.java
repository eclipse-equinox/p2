/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *     Sonatype, Inc. - ongoing development
 *     Red Hat, Inc. - support for remediation page
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * @since 3.4
 */
public class UpdateWizard extends WizardWithLicenses {
	IInstallableUnit[] iusToReplace;
	boolean skipSelectionsPage = false;
	IUElementListRoot firstPageRoot;
	Update[] initialSelections;

	public static Collection<IInstallableUnit> getIUsToReplace(Object[] elements) {
		Set<IInstallableUnit> iusToReplace = new HashSet<>();
		for (Object element : elements) {
			if (element instanceof AvailableUpdateElement) {
				iusToReplace.add(((AvailableUpdateElement) element).getIUToBeUpdated());
			}
		}
		return iusToReplace;
	}

	public static IInstallableUnit[] getReplacementIUs(Object[] elements) {
		Set<IInstallableUnit> replacements = new HashSet<>();
		for (Object element : elements) {
			if (element instanceof AvailableUpdateElement) {
				replacements.add(((AvailableUpdateElement) element).getIU());
			}
		}
		return replacements.toArray(new IInstallableUnit[replacements.size()]);
	}

	public static Update[] makeUpdatesFromElements(Object[] elements) {
		Set<Update> updates = new HashSet<>();
		for (Object element : elements) {
			if (element instanceof AvailableUpdateElement) {
				updates.add(((AvailableUpdateElement) element).getUpdate());
			}
		}
		return updates.toArray(new Update[updates.size()]);
	}

	/**
	 * Open an update wizard. For update wizards, the operation must have been
	 * resolved in advanced. This prevents searching for updates in the UI thread.
	 *
	 * @param ui                the provisioning UI
	 * @param operation         the update operation. Must already be resolved!
	 * @param initialSelections initial selections for the wizard (can be null)
	 * @param preloadJob        a job that has been used to preload metadata
	 *                          repositories (can be null)
	 */
	public UpdateWizard(ProvisioningUI ui, UpdateOperation operation, Object[] initialSelections,
			LoadMetadataRepositoryJob preloadJob) {
		super(ui, operation, initialSelections, preloadJob);
		this.initialSelections = (Update[]) initialSelections;
		Assert.isLegal(operation.hasResolved(), "Cannot create an update wizard on an unresolved operation"); //$NON-NLS-1$
		setWindowTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		setDefaultPageImageDescriptor(ProvUIImages.getImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE));
	}

	private boolean isLocked(IProfile profile, IInstallableUnit iuToBeUpdated) {
		return Boolean.parseBoolean(profile.getInstallableUnitProperty(iuToBeUpdated, IProfile.PROP_PROFILE_LOCKED_IU));
	}

	public void deselectLockedIUs() {
		IProfileRegistry profileRegistry = ui.getSession().getProvisioningAgent().getService(IProfileRegistry.class);
		IProfile profile = profileRegistry.getProfile(ui.getProfileId());

		ArrayList<Update> newSelection = new ArrayList<>(initialSelections.length);
		for (Update initialSelection : initialSelections) {
			if (!isLocked(profile, initialSelection.toUpdate)) {
				newSelection.add(initialSelection);
			}
		}

		((UpdateOperation) operation).setSelectedUpdates(newSelection.toArray(new Update[newSelection.size()]));
		recomputePlan(getContainer());
	}

	@Override
	protected ISelectableIUsPage createMainPage(IUElementListRoot input, Object[] selections) {
		mainPage = new SelectableIUsPage(ui, this, getAllPossibleUpdatesRoot(), selections);
		mainPage.setTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
		mainPage.setDescription(ProvUIMessages.UpdateAction_UpdatesAvailableMessage);
		((SelectableIUsPage) mainPage).updateStatus(getAllPossibleUpdatesRoot(), operation);
		return mainPage;
	}

	@Override
	protected ResolutionResultsWizardPage createResolutionPage() {
		return new UpdateWizardPage(ui, this, root, (UpdateOperation) operation);
	}

	@Override
	protected void initializeResolutionModelElements(Object[] selectedElements) {
		if (selectedElements == null) {
			return;
		}
		root = new IUElementListRoot(ui);
		if (operation instanceof RemediationOperation) {
			AvailableIUElement[] elements = ElementUtils
					.requestToElement(((RemediationOperation) operation).getCurrentRemedy(), false);
			root.setChildren(elements);
			// planSelections = elements;
		} else {
			ArrayList<AvailableUpdateElement> list = new ArrayList<>(selectedElements.length);
			ArrayList<AvailableUpdateElement> selected = new ArrayList<>(selectedElements.length);
			for (Object selectedElement : selectedElements) {
				if (selectedElement instanceof AvailableUpdateElement) {
					AvailableUpdateElement element = (AvailableUpdateElement) selectedElement;
					AvailableUpdateElement newElement = new AvailableUpdateElement(root, element.getIU(),
							element.getIUToBeUpdated(), getProfileId(), shouldShowProvisioningPlanChildren());
					list.add(newElement);
					selected.add(newElement);
				} else if (selectedElement instanceof Update) {
					Update update = (Update) selectedElement;
					AvailableUpdateElement newElement = new AvailableUpdateElement(root, update.replacement,
							update.toUpdate, getProfileId(), shouldShowProvisioningPlanChildren());
					list.add(newElement);
					selected.add(newElement);
				}
			}
			root.setChildren(list.toArray());
			planSelections = selected.toArray();
		}
	}

	@Override
	protected IResolutionErrorReportingPage createErrorReportingPage() {
		return (SelectableIUsPage) mainPage;
	}

	public void setSkipSelectionsPage(boolean skipSelectionsPage) {
		this.skipSelectionsPage = skipSelectionsPage;
	}

	@Override
	public IWizardPage getStartingPage() {
		if (skipSelectionsPage) {
			// TODO see https://bugs.eclipse.org/bugs/show_bug.cgi?id=276963
			IWizardPage page = getNextPage(mainPage);
			if (page != null) {
				return page;
			}
		}
		return mainPage;
	}

	@Override
	protected ProfileChangeOperation getProfileChangeOperation(Object[] elements) {
		if (operation == null) {
			operation = new UpdateOperation(ui.getSession(), getIUsToReplace(elements));
			operation.setProfileId(getProfileId());
			// operation.setRootMarkerKey(getRootMarkerKey());
		} else {
			((UpdateOperation) operation).setSelectedUpdates(makeUpdatesFromElements(elements));
		}
		return operation;
	}

	private IUElementListRoot getAllPossibleUpdatesRoot() {
		if (firstPageRoot == null) {
			firstPageRoot = new IUElementListRoot(ui);
			if (operation != null && operation instanceof UpdateOperation) {
				Update[] updates;
				if (getPolicy().getShowLatestVersionsOnly()) {
					updates = ((UpdateOperation) operation).getSelectedUpdates();
				} else {
					updates = ((UpdateOperation) operation).getPossibleUpdates();
				}
				ArrayList<AvailableUpdateElement> allPossible = new ArrayList<>(updates.length);
				for (Update update : updates) {
					AvailableUpdateElement newElement = new AvailableUpdateElement(firstPageRoot, update.replacement,
							update.toUpdate, getProfileId(), shouldShowProvisioningPlanChildren());
					allPossible.add(newElement);
				}
				firstPageRoot.setChildren(allPossible.toArray());
			}
		}
		return firstPageRoot;
	}

	@Override
	protected RemediationPage createRemediationPage() {
		remediationPage = new RemediationPage(ui, this, root, operation);
		return remediationPage;
	}

}
