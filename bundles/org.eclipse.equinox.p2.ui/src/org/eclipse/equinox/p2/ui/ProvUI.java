/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.ui;

import java.io.IOException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.configurator.Configurator;
import org.eclipse.equinox.internal.p2.ui.ApplyProfileChangesDialog;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.ui.viewers.IUColumnConfig;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Generic provisioning UI utility and policy methods.
 * 
 * @since 3.4
 */
public class ProvUI {

	// Public constants for common command and tooltip names
	public static final String INSTALL_COMMAND_LABEL = ProvUIMessages.InstallIUCommandLabel;
	public static final String INSTALL_COMMAND_TOOLTIP = ProvUIMessages.InstallIUCommandTooltip;
	public static final String UNINSTALL_COMMAND_LABEL = ProvUIMessages.UninstallIUCommandLabel;
	public static final String UNINSTALL_COMMAND_TOOLTIP = ProvUIMessages.UninstallIUCommandTooltip;
	public static final String UPDATE_COMMAND_LABEL = ProvUIMessages.UpdateIUCommandLabel;
	public static final String UPDATE_COMMAND_TOOLTIP = ProvUIMessages.UpdateIUCommandTooltip;
	public static final String ROLLBACK_COMMAND_LABEL = ProvUIMessages.RollbackIUCommandLabel;
	public static final String ROLLBACK_COMMAND_TOOLTIP = ProvUIMessages.RollbackIUCommandTooltip;

	private static IUColumnConfig[] iuColumnConfig = new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_ID), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION)};

	public static Shell getShell(IAdaptable uiInfo) {
		Shell shell;
		if (uiInfo != null) {
			shell = (Shell) uiInfo.getAdapter(Shell.class);
			if (shell != null) {
				return shell;
			}
		}
		// Get the default shell
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display.getActiveShell();
	}

	public static void handleException(Throwable t, String message) {
		if (message == null && t != null) {
			message = t.getMessage();
		}
		IStatus status = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, message, t);
		StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
	}

	public static void reportStatus(IStatus status) {
		StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
	}

	public static IUColumnConfig[] getIUColumnConfig() {
		return iuColumnConfig;
	}

	public static void setIUColumnConfig(IUColumnConfig[] columnConfig) {
		iuColumnConfig = columnConfig;
	}

	public static void requestRestart(boolean restartRequired, Shell shell) {
		int retCode = ApplyProfileChangesDialog.promptForRestart(shell, restartRequired);
		if (retCode == ApplyProfileChangesDialog.PROFILE_APPLYCHANGES) {
			Configurator configurator = (Configurator) ServiceHelper.getService(ProvUIActivator.getContext(), Configurator.class.getName());
			try {
				configurator.applyConfiguration();
			} catch (IOException e) {
				ProvUI.handleException(e, null);
			}
		} else if (retCode == ApplyProfileChangesDialog.PROFILE_RESTART) {
			PlatformUI.getWorkbench().restart();
		}
	}
}
