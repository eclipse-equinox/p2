/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.provisional.p2.engine.Operand;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * This class defines commonly used status objects for communicating issues
 * about provisioning plans to the user and analyzes a proposed provisioning plan
 * to find common problems and report them in a way that makes sense to a user.
 * 
 * @since 3.4
 */
public class PlanStatusHelper {

	public static IStatus getStatus(int statusCode, IInstallableUnit affectedIU) {
		switch (statusCode) {
			case IStatusCodes.IMPLIED_UPDATE :
				return new Status(IStatus.INFO, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_ImpliedUpdate, getIUString(affectedIU)), null);
			case IStatusCodes.IGNORED_IMPLIED_DOWNGRADE :
				return new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_IgnoringImpliedDowngrade, getIUString(affectedIU)), null);
			case IStatusCodes.IGNORED_ALREADY_INSTALLED :
				return new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_AlreadyInstalled, getIUString(affectedIU)), null);
			case IStatusCodes.UNEXPECTED_NOTHING_TO_DO :
				return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_NothingToDo, getIUString(affectedIU)), null);
			case IStatusCodes.PROFILE_CHANGE_ALTERED :
				return new MultiStatus(ProvUIActivator.PLUGIN_ID, statusCode, ProvUIMessages.PlanStatusHelper_RequestAltered, null);
			default :
				return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, statusCode, NLS.bind(ProvUIMessages.PlanStatusHelper_UnexpectedError, new Integer(statusCode), getIUString(affectedIU)), null);
		}
	}

	public static MultiStatus getProfileChangeAlteredStatus() {
		return (MultiStatus) getStatus(IStatusCodes.PROFILE_CHANGE_ALTERED, null);
	}

	public static IStatus computeStatus(ProvisioningPlan plan, IInstallableUnit[] ius) {
		if (plan == null)
			return getStatus(IStatusCodes.UNEXPECTED_NOTHING_TO_DO, null);
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
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.PlanStatusHelper_RequiresUpdateManager);
		}

		// If the plan is ok or cancel, no further analysis is needed
		if (plan.getStatus().isOK() || plan.getStatus().getSeverity() == IStatus.CANCEL)
			return plan.getStatus();

		// If this is a status we have already checked, don't bother doing so again.
		if (plan.getStatus().getCode() == IStatusCodes.UNEXPECTED_NOTHING_TO_DO)
			return plan.getStatus();

		// If the plan has no IU operands and some were expected, then nothing will happen.
		if (ius != null && ius.length > 0) {
			boolean iusInPlan = false;
			Operand[] operands = plan.getOperands();
			for (int i = 0; i < operands.length; i++)
				if (operands[i] instanceof InstallableUnitOperand) {
					iusInPlan = true;
					break;
				}
			if (!iusInPlan) {
				MultiStatus status = new MultiStatus(ProvUIActivator.PLUGIN_ID, IStatusCodes.UNEXPECTED_NOTHING_TO_DO, ProvUIMessages.PlanStatusHelper_NothingToDo, null);
				status.add(getStatus(IStatusCodes.UNEXPECTED_NOTHING_TO_DO, null));
				if (plan.getStatus() != null)
					status.merge(plan.getStatus());
				return status;
			}
		}
		// We have no further interpretation.
		return plan.getStatus();

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
}
