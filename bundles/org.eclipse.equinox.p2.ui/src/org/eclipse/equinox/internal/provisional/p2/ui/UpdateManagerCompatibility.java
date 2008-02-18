/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui;

import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.provisional.p2.engine.Operand;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Utility methods involving compatibility with the Eclipse Update Manager.
 * 
 * @since 3.4
 *
 */
public class UpdateManagerCompatibility {

	// This value was copied from MetadataGeneratorHelper.  Must be the same.
	private static final String ECLIPSE_INSTALL_HANDLER_PROP = "org.eclipse.update.installHandler"; //$NON-NLS-1$
	// These values rely on the command markup in org.eclipse.ui.ide that defines the update commands
	private static final String UPDATE_MANAGER_FIND_AND_INSTALL = "org.eclipse.ui.update.findAndInstallUpdates"; //$NON-NLS-1$
	private static final String UPDATE_MANAGER_MANAGE_CONFIGURATION = "org.eclipse.ui.update.manageConfiguration"; //$NON-NLS-1$

	public static boolean requiresInstallHandlerSupport(ProvisioningPlan plan) {
		Operand[] operands = plan.getOperands();
		for (int i = 0; i < operands.length; i++) {
			if (operands[i] instanceof InstallableUnitOperand) {
				IInstallableUnit iu = ((InstallableUnitOperand) operands[i]).second();
				if (iu != null && iu.getProperty(ECLIPSE_INSTALL_HANDLER_PROP) != null)
					return true;
			}
		}
		return false;

	}

	/**
	 * Open the old UpdateManager installer UI using the specified shell. 
	 * We do not call the UpdateManagerUI class directly because we want to be able to be configured 
	 * without requiring those plug-ins.  Instead, we invoke a known command.
	 */
	public static void openInstaller() {
		runCommand(UPDATE_MANAGER_FIND_AND_INSTALL, ProvUIMessages.UpdateManagerCompatibility_UnableToOpenFindAndInstall);
	}

	/**
	 * Open the old UpdateManager configuration manager UI using the specified shell. 
	 * We do not call the UpdateManagerUI class directly because we want to be able to be configured 
	 * without requiring those plug-ins.  Instead, we invoke a known command.
	 */
	public static void openConfigurationManager() {
		runCommand(UPDATE_MANAGER_MANAGE_CONFIGURATION, ProvUIMessages.UpdateManagerCompatibility_UnableToOpenManageConfiguration);
	}

	private static void runCommand(String commandId, String errorMessage) {
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = commandService.getCommand(commandId);
		if (!command.isDefined()) {
			return;
		}
		IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
		try {
			handlerService.executeCommand(commandId, null);
		} catch (ExecutionException e) {
			reportFail(errorMessage, e);
		} catch (NotDefinedException e) {
			reportFail(errorMessage, e);
		} catch (NotEnabledException e) {
			reportFail(errorMessage, e);
		} catch (NotHandledException e) {
			reportFail(errorMessage, e);
		}
	}

	private static void reportFail(String message, Throwable t) {
		Status failStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, message, t);
		ProvUI.reportStatus(failStatus, StatusManager.BLOCK | StatusManager.LOG);

	}
}