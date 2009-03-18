/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import java.util.Iterator;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * This class analyzes a profile change request and the resultant provisioning plan,
 * and reports problems in a way that can be communicated to a user.
 * 
 * @since 3.5
 */
public class PlanAnalyzer {

	public static IStatus getStatus(int statusCode, IInstallableUnit affectedIU) {
		switch (statusCode) {
			case IStatusCodes.PROFILE_CHANGE_ALTERED :
				return new MultiStatus(ProvUIActivator.PLUGIN_ID, statusCode, ProvUIMessages.PlanStatusHelper_RequestAltered, null);
			case IStatusCodes.ALTERED_IMPLIED_UPDATE :
				return new Status(IStatus.INFO, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_ImpliedUpdate, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_IGNORED_IMPLIED_UPDATE :
				return new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanAnalyzer_LockedImpliedUpdate0, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_IGNORED_IMPLIED_DOWNGRADE :
				return new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_IgnoringImpliedDowngrade, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_IGNORED_ALREADY_INSTALLED :
				return new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_AlreadyInstalled, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_PARTIAL_INSTALL :
				return new Status(IStatus.INFO, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanAnalyzer_PartialInstall, getIUString(affectedIU)), null);
			case IStatusCodes.ALTERED_PARTIAL_UNINSTALL :
				return new Status(IStatus.INFO, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanAnalyzer_PartialUninstall, getIUString(affectedIU)), null);
			case IStatusCodes.UNEXPECTED_NOTHING_TO_DO :
				return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_NothingToDo, getIUString(affectedIU)), null);
			case IStatusCodes.NOTHING_TO_UPDATE :
				return new Status(IStatus.INFO, ProvUIActivator.PLUGIN_ID, statusCode, ProvUIMessages.UpdateOperation_NothingToUpdate, null);
			case IStatusCodes.OPERATION_ALREADY_IN_PROGRESS :
				return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, statusCode, ProvUIMessages.PlanStatusHelper_AnotherOperationInProgress, null);
			default :
				return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_UnexpectedError, new Integer(statusCode), getIUString(affectedIU)), null);
		}
	}

	public static MultiStatus getProfileChangeAlteredStatus() {
		return (MultiStatus) getStatus(IStatusCodes.PROFILE_CHANGE_ALTERED, null);
	}

	public static ResolutionResult computeResolutionResult(ProfileChangeRequest originalRequest, ProvisioningPlan plan, MultiStatus originalStatus) {
		Assert.isNotNull(originalRequest);
		Assert.isNotNull(plan);
		Assert.isNotNull(originalStatus);

		ResolutionResult report = new ResolutionResult();

		// If the plan was canceled, no further analysis is needed
		if (plan.getStatus().getSeverity() == IStatus.CANCEL) {
			report.addSummaryStatus(plan.getStatus());
			return report;
		}

		// If the plan requires install handler support, we want to open the old update UI and
		// cancel this operation
		if (UpdateManagerCompatibility.requiresInstallHandlerSupport(plan)) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				public void run() {
					Shell shell = ProvUI.getDefaultParentShell();
					MessageDialog dialog = new MessageDialog(shell, ProvUIMessages.PlanStatusHelper_UpdateManagerPromptTitle, null, ProvUIMessages.PlanStatusHelper_PromptForUpdateManagerUI, MessageDialog.WARNING, new String[] {ProvUIMessages.PlanStatusHelper_Launch, IDialogConstants.CANCEL_LABEL}, 0);
					if (dialog.open() == 0)
						BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
							public void run() {
								UpdateManagerCompatibility.openInstaller();
							}
						});
				}
			});
			report.addSummaryStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.PlanStatusHelper_RequiresUpdateManager));
		}

		if (nothingToDo(originalRequest)) {
			report.addSummaryStatus(getStatus(IStatusCodes.UNEXPECTED_NOTHING_TO_DO, null));
			IStatus[] details = originalStatus.getChildren();
			for (int i = 0; i < details.length; i++)
				report.addSummaryStatus(details[i]);
			return report;
		}

		// If there was already some status supplied before resolution, this should get included
		// with the report.  For example, this might contain information about the profile request
		// being altered before resolution began.
		if (originalStatus != null && originalStatus.getChildren().length > 0) {
			report.addSummaryStatus(originalStatus);
		}

		// If the overall plan had a non-OK status, capture that in the report.
		if (!plan.getStatus().isOK())
			report.addSummaryStatus(plan.getStatus());

		// Now we compare what was requested with what is going to happen.
		// In the long run, when a RequestStatus can provide actual explanation/status
		// about failures, we might want to add this information to the overall status.
		// As it stands now, if the provisioning plan is in error, that info is more detailed
		// than the request status.  So we will only add request status info to the overall
		// status when the overall status is not in error.
		if (plan.getStatus().getSeverity() != IStatus.ERROR) {
			IInstallableUnit[] iusAdded = originalRequest.getAddedInstallableUnits();
			for (int i = 0; i < iusAdded.length; i++) {
				RequestStatus rs = plan.getRequestStatus(iusAdded[i]);
				if (rs.getSeverity() == IStatus.ERROR) {
					// This is a serious error so it must also appear in the overall status
					IStatus fail = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, IStatusCodes.ALTERED_IGNORED_INSTALL_REQUEST, NLS.bind(ProvUIMessages.PlanAnalyzer_IgnoringInstall, getIUString(iusAdded[i])), null);
					report.addStatus(iusAdded[i], fail);
					report.addSummaryStatus(fail);
				}
			}
			IInstallableUnit[] iusRemoved = originalRequest.getRemovedInstallableUnits();
			for (int i = 0; i < iusRemoved.length; i++) {
				RequestStatus rs = plan.getRequestStatus(iusRemoved[i]);
				if (rs.getSeverity() == IStatus.ERROR) {
					// TODO see https://bugs.eclipse.org/bugs/show_bug.cgi?id=255984
					// We are making assumptions here about why the planner chose to ignore an uninstall.
					// Assume it could not be uninstalled because of some other dependency, yet the planner did not view
					// this as an error.  So we inform the user that we can only uninstall parts of it.  The root property will be
					// removed per the original change request.
					IStatus fail = new Status(IStatus.INFO, ProvUIActivator.PLUGIN_ID, IStatusCodes.ALTERED_PARTIAL_UNINSTALL, NLS.bind(ProvUIMessages.PlanAnalyzer_PartialUninstall, getIUString(iusRemoved[i])), null);
					report.addStatus(iusRemoved[i], fail);
					report.addSummaryStatus(fail);
				}
			}
		}

		// Now process the side effects
		Map sideEffects = plan.getSideEffectChanges();
		Iterator iusWithSideEffects = sideEffects.keySet().iterator();
		while (iusWithSideEffects.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) iusWithSideEffects.next();
			RequestStatus rs = (RequestStatus) sideEffects.get(iu);
			if (rs.getInitialRequestType() == RequestStatus.ADDED) {
				report.addStatus(iu, new Status(rs.getSeverity(), ProvUIActivator.PLUGIN_ID, IStatusCodes.ALTERED_SIDE_EFFECT_INSTALL, NLS.bind(ProvUIMessages.PlanAnalyzer_SideEffectInstall, getIUString(iu)), null));
			} else {
				report.addStatus(iu, new Status(rs.getSeverity(), ProvUIActivator.PLUGIN_ID, IStatusCodes.ALTERED_SIDE_EFFECT_REMOVE, NLS.bind(ProvUIMessages.PlanAnalyzer_SideEffectUninstall, getIUString(iu)), null));
			}
		}

		return report;

	}

	private static String getIUString(IInstallableUnit iu) {
		if (iu == null)
			return ProvUIMessages.PlanStatusHelper_Items;
		// Get the iu name in the default locale
		String name = IUPropertyUtils.getIUProperty(iu, IInstallableUnit.PROP_NAME);
		if (name != null)
			return name;
		return iu.getId();
	}

	private static boolean nothingToDo(ProfileChangeRequest request) {
		return request.getAddedInstallableUnits().length == 0 && request.getRemovedInstallableUnits().length == 0 && request.getInstallableUnitProfilePropertiesToAdd().size() == 0 && request.getInstallableUnitProfilePropertiesToRemove().size() == 0;
	}
}
