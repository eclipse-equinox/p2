/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * A wizard page that presents a check box list of IUs and allows the user
 * to select and deselect them.  Typically the first page in a provisioning
 * operation wizard, and usually it is the page used to report resolution errors
 * before advancing to resolution detail.
 * 
 * @since 3.5
 *
 */
public abstract class ResolutionStatusPage extends ProvisioningWizardPage {
	private static final String LIST_WEIGHT = "ListSashWeight"; //$NON-NLS-1$
	private static final String DETAILS_WEIGHT = "DetailsSashWeight"; //$NON-NLS-1$
	private static final String NAME_COLUMN_WIDTH = "NameColumnWidth"; //$NON-NLS-1$
	private static final String VERSION_COLUMN_WIDTH = "VersionColumnWidth"; //$NON-NLS-1$
	private static final String ID_COLUMN_WIDTH = "IDColumnWidth"; //$NON-NLS-1$
	private IUColumnConfig nameColumn, versionColumn, idColumn;

	/**
	 * @param pageName
	 */
	protected ResolutionStatusPage(String pageName, ProvisioningUI ui, ProvisioningOperationWizard wizard) {
		super(pageName, ui, wizard);
	}

	/**
	 * Update the caches associated with this page.
	 * @param root the new root, or <code>null</code> if the root should not be updated.
	 * @param resolvedOperation the new operation
	 */
	protected abstract void updateCaches(IUElementListRoot root, ProfileChangeOperation resolvedOperation);

	protected abstract boolean isCreated();

	protected abstract IUDetailsGroup getDetailsGroup();

	protected abstract IInstallableUnit getSelectedIU();

	protected abstract Object[] getSelectedElements();

	/**
	 * Update the status area of the wizard to report the results of the operation.
	 * 
	 * @param newRoot the root that describes the root IUs involved in creating the plan.
	 * 		This can be <code>null</code> if the root should not be updated.
	 * 
	 * @param op the ProfileChangeOperation that describes the operation
	 */
	public void updateStatus(IUElementListRoot newRoot, ProfileChangeOperation op) {
		IStatus currentStatus = getProvisioningWizard().getCurrentStatus();
		updateCaches(newRoot, op);

		int messageType = IMessageProvider.NONE;
		boolean pageComplete = op != null;
		if (currentStatus != null && !currentStatus.isOK()) {
			messageType = IMessageProvider.INFORMATION;
			int severity = currentStatus.getSeverity();
			if (severity == IStatus.ERROR) {
				messageType = IMessageProvider.ERROR;
				pageComplete = false;
				// Log errors for later support
				ProvUI.reportStatus(currentStatus, StatusManager.LOG);
			} else if (severity == IStatus.WARNING) {
				messageType = IMessageProvider.WARNING;
				// Log warnings for later support
				ProvUI.reportStatus(currentStatus, StatusManager.LOG);
			} else if (severity == IStatus.CANCEL) {
				pageComplete = shouldCompleteOnCancel();
			}
		}
		setPageComplete(pageComplete);
		if (!isCreated())
			return;

		setMessage(getMessageText(currentStatus), messageType);
		setDetailText(op);
	}

	protected boolean shouldCompleteOnCancel() {
		return true;
	}

	protected String getIUDescription(IInstallableUnit iu) {
		// Get the iu description in the default locale
		String description = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
		if (description == null)
			description = ""; //$NON-NLS-1$
		return description;
	}

	protected String getIUDescription(AvailableIUElement element) {
		IInstallableUnit iu = element.getIU();

		String description = getIUDescription(iu);

		if (isLocked(element)) {
			description = NLS.bind(ProvUIMessages.ResolutionStatusPage_ErrorIULocked, description);
		}
		return description;
	}

	protected String getIUDescription(Object element) {
		if (element instanceof AvailableIUElement) {
			return getIUDescription((AvailableIUElement) element);
		}
		IInstallableUnit selectedIU = ElementUtils.elementToIU(element);
		if (selectedIU != null) {
			return getIUDescription(selectedIU);
		}
		return ""; //$NON-NLS-1$
	}

	String getMessageText(IStatus currentStatus) {
		if (currentStatus == null || currentStatus.isOK())
			return getDescription();
		if (currentStatus.getSeverity() == IStatus.CANCEL)
			return ProvUIMessages.ResolutionWizardPage_Canceled;
		if (currentStatus.getSeverity() == IStatus.ERROR)
			return ProvUIMessages.ResolutionWizardPage_ErrorStatus;
		return ProvUIMessages.ResolutionWizardPage_WarningInfoStatus;
	}

	void setDetailText(ProfileChangeOperation resolvedOperation) {
		String detail = null;
		Object selectedElement = getSelectedElement();
		IInstallableUnit selectedIU = ElementUtils.elementToIU(selectedElement);
		IUDetailsGroup detailsGroup = getDetailsGroup();

		// We either haven't resolved, or we failed to resolve and reported some error
		// while doing so.  
		if (resolvedOperation == null || !resolvedOperation.hasResolved() || getProvisioningWizard().statusOverridesOperation()) {
			// See if the wizard status knows something more about it.
			IStatus currentStatus = getProvisioningWizard().getCurrentStatus();
			if (!currentStatus.isOK()) {
				detail = currentStatus.getMessage();
				detailsGroup.enablePropertyLink(false);
			} else if (selectedIU != null) {
				detail = getIUDescription(selectedElement);
				detailsGroup.enablePropertyLink(true);
			} else {
				detail = ""; //$NON-NLS-1$
				detailsGroup.enablePropertyLink(false);
			}
			detailsGroup.setDetailText(detail);
			return;
		}

		// An IU is selected and we have resolved.  Look for information about the specific IU.
		if (selectedIU != null) {
			detail = resolvedOperation.getResolutionDetails(selectedIU);
			if (detail != null) {
				detailsGroup.enablePropertyLink(false);
				detailsGroup.setDetailText(detail);
				return;
			}
			// No specific error about this IU.  Show the overall error if it is in error.
			if (resolvedOperation.getResolutionResult().getSeverity() == IStatus.ERROR) {
				detail = resolvedOperation.getResolutionDetails();
				if (detail != null) {
					detailsGroup.enablePropertyLink(false);
					detailsGroup.setDetailText(detail);
					return;
				}
			}

			// The overall status is not an error, or else there was no explanatory text for an error.
			// We may as well just show info about this iu.
			detailsGroup.enablePropertyLink(true);
			detailsGroup.setDetailText(getIUDescription(selectedElement));
			return;
		}

		//No IU is selected, give the overall report
		detail = resolvedOperation.getResolutionDetails();
		detailsGroup.enablePropertyLink(false);
		if (detail == null)
			detail = ""; //$NON-NLS-1$
		detailsGroup.setDetailText(detail);
	}

	private Object getSelectedElement() {
		Object[] elements = getSelectedElements();
		if (elements.length == 0)
			return null;
		return elements[0];
	}

	protected abstract String getDialogSettingsName();

	protected abstract SashForm getSashForm();

	private IUColumnConfig getNameColumn() {
		return nameColumn;
	}

	private IUColumnConfig getVersionColumn() {
		return versionColumn;
	}

	private IUColumnConfig getIdColumn() {
		return idColumn;
	}

	protected abstract int getColumnWidth(int index);

	private int getNameColumnWidth() {
		return getColumnWidth(0);
	}

	private int getVersionColumnWidth() {
		return getColumnWidth(1);
	}

	private int getIdColumnWidth() {
		return getColumnWidth(2);
	}

	protected int[] getSashWeights() {
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsName());
		if (section != null) {
			try {
				int[] weights = new int[2];
				if (section.get(LIST_WEIGHT) != null) {
					weights[0] = section.getInt(LIST_WEIGHT);
					if (section.get(DETAILS_WEIGHT) != null) {
						weights[1] = section.getInt(DETAILS_WEIGHT);
						return weights;
					}
				}
			} catch (NumberFormatException e) {
				// Ignore if there actually was a value that didn't parse.  
			}
		}
		return ILayoutConstants.IUS_TO_DETAILS_WEIGHTS;
	}

	protected void getColumnWidthsFromSettings() {
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsName());
		if (section != null) {
			try {
				if (section.get(NAME_COLUMN_WIDTH) != null)
					getNameColumn().setWidthInPixels(section.getInt(NAME_COLUMN_WIDTH));
				if (section.get(VERSION_COLUMN_WIDTH) != null)
					getVersionColumn().setWidthInPixels(section.getInt(VERSION_COLUMN_WIDTH));
				if (section.get(ID_COLUMN_WIDTH) != null)
					getIdColumn().setWidthInPixels(section.getInt(ID_COLUMN_WIDTH));
			} catch (NumberFormatException e) {
				// Ignore if there actually was a value that didn't parse.  
			}
		}
	}

	public void saveBoundsRelatedSettings() {
		if (!isCreated())
			return;
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsName());
		if (section == null) {
			section = settings.addNewSection(getDialogSettingsName());
		}
		section.put(NAME_COLUMN_WIDTH, getNameColumnWidth());
		section.put(VERSION_COLUMN_WIDTH, getVersionColumnWidth());
		section.put(ID_COLUMN_WIDTH, getIdColumnWidth());
		int[] weights = getSashForm().getWeights();
		section.put(LIST_WEIGHT, weights[0]);
		section.put(DETAILS_WEIGHT, weights[1]);
	}

	protected IUColumnConfig[] getColumnConfig() {
		// We intentionally use the IU's id as one of the columns, because
		// resolution errors are reported by ID.
		nameColumn = new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH);
		versionColumn = new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH);
		idColumn = new IUColumnConfig(ProvUIMessages.ProvUI_IdColumnTitle, IUColumnConfig.COLUMN_ID, ILayoutConstants.DEFAULT_COLUMN_WIDTH);
		getColumnWidthsFromSettings();
		return new IUColumnConfig[] {nameColumn, versionColumn, idColumn};
	}

	private boolean isLocked(AvailableIUElement elementToBeUpdated) {
		if (elementToBeUpdated instanceof AvailableUpdateElement)
			return ((AvailableUpdateElement) elementToBeUpdated).isLockedForUpdate();
		return false;
	}
}
