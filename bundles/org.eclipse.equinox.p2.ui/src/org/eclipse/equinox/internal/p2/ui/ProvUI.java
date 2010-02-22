/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.ILayoutConstants;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.internal.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.CategoryQuery;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
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
	public static final String REVERT_COMMAND_LABEL = ProvUIMessages.RevertIUCommandLabel;
	public static final String REVERT_COMMAND_TOOLTIP = ProvUIMessages.RevertIUCommandTooltip;

	private static IUColumnConfig[] columnConfig;
	private static QueryProvider queryProvider;

	// These values rely on the command markup in org.eclipse.ui.ide that defines the update commands
	private static final String UPDATE_MANAGER_FIND_AND_INSTALL = "org.eclipse.ui.update.findAndInstallUpdates"; //$NON-NLS-1$
	private static final String UPDATE_MANAGER_MANAGE_CONFIGURATION = "org.eclipse.ui.update.manageConfiguration"; //$NON-NLS-1$
	// This value relies on the command markup in org.eclipse.ui 
	private static final String INSTALLATION_DIALOG = "org.eclipse.ui.help.installationDialog"; //$NON-NLS-1$

	public static IStatus handleException(Throwable t, String message, int style) {
		if (message == null && t != null) {
			message = t.getMessage();
		}
		IStatus status = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, message, t);
		StatusManager.getManager().handle(status, style);
		return status;
	}

	public static void reportStatus(IStatus status, int style) {
		// workaround for
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=211933
		// Note we'd rather have a proper looking dialog than get the 
		// blocking right.
		if ((style & StatusManager.BLOCK) == StatusManager.BLOCK || (style & StatusManager.SHOW) == StatusManager.SHOW) {
			if (status.getSeverity() == IStatus.INFO) {
				MessageDialog.openInformation(ProvUI.getDefaultParentShell(), ProvUIMessages.ProvUI_InformationTitle, status.getMessage());
				// unset the dialog bits
				style = style & ~StatusManager.BLOCK;
				style = style & ~StatusManager.SHOW;
				// unset logging for statuses that should never be logged.
				// Ideally the caller would do this but this bug keeps coming back.
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=274074
				if (status.getCode() == ProvisioningSession.STATUS_NOTHING_TO_UPDATE)
					style = 0;
			} else if (status.getSeverity() == IStatus.WARNING) {
				MessageDialog.openWarning(ProvUI.getDefaultParentShell(), ProvUIMessages.ProvUI_WarningTitle, status.getMessage());
				// unset the dialog bits
				style = style & ~StatusManager.BLOCK;
				style = style & ~StatusManager.SHOW;
			}
		}
		if (style != 0)
			StatusManager.getManager().handle(status, style);
	}

	public static IUColumnConfig[] getIUColumnConfig() {
		if (columnConfig == null)
			columnConfig = new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, ILayoutConstants.DEFAULT_COLUMN_WIDTH)};
		return columnConfig;

	}

	public static IUViewQueryContext getQueryContext(Policy policy) {
		IUViewQueryContext queryContext = new IUViewQueryContext(policy.getGroupByCategory() ? IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY : IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		queryContext.setShowLatestVersionsOnly(policy.getShowLatestVersionsOnly());
		queryContext.setShowInstallChildren(policy.getShowDrilldownRequirements());
		queryContext.setShowProvisioningPlanChildren(policy.getShowDrilldownRequirements());
		queryContext.setUseCategories(policy.getGroupByCategory());
		return queryContext;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getAdapter(Object object, Class<T> adapterType) {
		if (object == null)
			return null;
		if (adapterType.isInstance(object))
			// Ideally, we would use Class.cast here but it was introduced in Java 1.5
			return (T) object;
		if (object instanceof IAdaptable)
			// Ideally, we would use Class.cast here but it was introduced in Java 1.5
			return (T) ((IAdaptable) object).getAdapter(adapterType);
		return null;
	}

	/**
	 * Returns a shell that is appropriate to use as the parent
	 * for a modal dialog. 
	 */
	public static Shell getDefaultParentShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}

	public static void addProvisioningListener(ProvUIProvisioningListener listener) {
		ProvUIActivator.getDefault().addProvisioningListener(listener);
	}

	public static void removeProvisioningListener(ProvUIProvisioningListener listener) {
		ProvUIActivator.getDefault().removeProvisioningListener(listener);
	}

	public static void openUpdateManagerInstaller(Event event) {
		runCommand(UPDATE_MANAGER_FIND_AND_INSTALL, ProvUIMessages.UpdateManagerCompatibility_UnableToOpenFindAndInstall, event);
	}

	public static void openUpdateManagerConfigurationManager(Event event) {
		runCommand(UPDATE_MANAGER_MANAGE_CONFIGURATION, ProvUIMessages.UpdateManagerCompatibility_UnableToOpenManageConfiguration, event);
	}

	public static void openInstallationDialog(Event event) {
		runCommand(INSTALLATION_DIALOG, ProvUIMessages.ProvUI_InstallDialogError, event);
	}

	public static QueryProvider getQueryProvider() {
		if (queryProvider == null)
			queryProvider = new QueryProvider(ProvUIActivator.getDefault().getProvisioningUI());
		return queryProvider;
	}

	private static void runCommand(String commandId, String errorMessage, Event event) {
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = commandService.getCommand(commandId);
		if (!command.isDefined()) {
			return;
		}
		IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
		try {
			handlerService.executeCommand(commandId, event);
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

	public static boolean isCategory(IInstallableUnit iu) {
		return CategoryQuery.isCategory(iu);
	}

	private static void reportFail(String message, Throwable t) {
		Status failStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, message, t);
		reportStatus(failStatus, StatusManager.BLOCK | StatusManager.LOG);
	}

	/**
	 * For testing only
	 * @noreference
	 * @param provider
	 */
	public static void setQueryProvider(QueryProvider provider) {
		queryProvider = provider;
	}
}
