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

import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.IUPropertyUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.jface.dialogs.IMessageProvider;
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
			detailsGroup.getDetailsArea().setText(detail);
			return;
		}

		// An IU is selected and we have resolved.  Look for information about the specific IU.
		if (selectedIU != null) {
			detail = resolvedOperation.getResolutionResult().getDetailedReport(new IInstallableUnit[] {selectedIU});
			if (detail != null) {
				detailsGroup.enablePropertyLink(false);
				detailsGroup.getDetailsArea().setText(detail);
				return;
			}
			// No specific error about this IU.  Show the overall error if it is in error.
			if (resolvedOperation.getResolutionResult().getSummaryStatus().getSeverity() == IStatus.ERROR) {
				detail = resolvedOperation.getResolutionResult().getSummaryReport();
				detailsGroup.enablePropertyLink(false);
				detailsGroup.getDetailsArea().setText(detail);
			}

			// The overall status is not an error, so we may as well just show info about this iu rather than everything.
			detailsGroup.enablePropertyLink(true);
			detailsGroup.getDetailsArea().setText(getIUDescription(selectedIU));
			return;
		}

		//No IU is selected, give the overall report
		detail = resolvedOperation.getResolutionResult().getSummaryReport();
		detailsGroup.enablePropertyLink(false);
		if (detail == null)
			detail = ""; //$NON-NLS-1$
		detailsGroup.getDetailsArea().setText(detail);
	}
}
