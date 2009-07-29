/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.IUPropertyUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.IUColumnConfig;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
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
	protected String profileId;

	/**
	 * @param pageName
	 */
	protected ResolutionStatusPage(String pageName, String profileId) {
		super(pageName);
		this.profileId = profileId;
	}

	protected abstract void updateCaches(IUElementListRoot root, PlannerResolutionOperation resolvedOperation);

	protected abstract boolean isCreated();

	protected abstract IUDetailsGroup getDetailsGroup();

	protected abstract IInstallableUnit getSelectedIU();

	/**
	 * Update the status area of the wizard to report the results of the operation.
	 * 
	 * @param newRoot the root that describes the root IUs involved in creating the plan
	 * @param op the PlannerResolutionOperation that describes the plan that was created.  
	 * Should not be <code>null</code>, but subclasses can be more forgiving.
	 */
	public void updateStatus(IUElementListRoot newRoot, PlannerResolutionOperation op) {
		Assert.isNotNull(op);
		updateCaches(newRoot, op);

		IStatus currentStatus;
		int messageType = IMessageProvider.NONE;
		boolean pageComplete = true;
		currentStatus = op.getResolutionResult().getSummaryStatus();
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
			}
		}
		setPageComplete(pageComplete);
		if (!isCreated())
			return;

		setMessage(getMessageText(currentStatus), messageType);
		setDetailText(op);
	}

	protected String getIUDescription(IInstallableUnit iu) {
		// Get the iu description in the default locale
		String description = IUPropertyUtils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION);
		if (description == null)
			description = ""; //$NON-NLS-1$
		return description;
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

	void setDetailText(PlannerResolutionOperation resolvedOperation) {
		String detail = null;
		IInstallableUnit selectedIU = getSelectedIU();
		IUDetailsGroup detailsGroup = getDetailsGroup();
		// We either haven't resolved, or we failed to resolve and reported some error
		// while doing so.  Since the specific error was already reported, the description
		// text can be used for the selected IU.
		if (resolvedOperation == null) {
			if (selectedIU != null) {
				detail = getIUDescription(selectedIU);
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
			detail = resolvedOperation.getResolutionResult().getDetailedReport(new IInstallableUnit[] {selectedIU});
			if (detail != null) {
				detailsGroup.enablePropertyLink(false);
				detailsGroup.setDetailText(detail);
				return;
			}
			// No specific error about this IU.  Show the overall error if it is in error.
			if (resolvedOperation.getResolutionResult().getSummaryStatus().getSeverity() == IStatus.ERROR) {
				detail = resolvedOperation.getResolutionResult().getSummaryReport();
				detailsGroup.enablePropertyLink(false);
				detailsGroup.setDetailText(detail);
				return;
			}

			// The overall status is not an error, so we may as well just show info about this iu rather than everything.
			detailsGroup.enablePropertyLink(true);
			detailsGroup.setDetailText(getIUDescription(selectedIU));
			return;
		}

		//No IU is selected, give the overall report
		detail = resolvedOperation.getResolutionResult().getSummaryReport();
		detailsGroup.enablePropertyLink(false);
		if (detail == null)
			detail = ""; //$NON-NLS-1$
		detailsGroup.setDetailText(detail);
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
}
