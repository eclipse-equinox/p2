/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Hamdan Msheik) - Bug 396420 - Control Install dialog through preference customization
 *     Red Hat Inc. - Bug 460967
 *     Mickael Istria (Red Hat Inc.) - 483644 Improve "No updates found" dialog
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.ILayoutConstants;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.internal.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

	/**
	 * A constant indicating that there was nothing to size (there
	 * was no valid plan that could be used to compute
	 * size).
	 */
	public static final long SIZE_NOTAPPLICABLE = -3L;
	/**
	 * Indicates that the size is unavailable (an
	 * attempt was made to compute size but it failed)
	 */
	public static final long SIZE_UNAVAILABLE = -2L;
	/**
	 * Indicates that the size is currently unknown
	 */
	public static final long SIZE_UNKNOWN = -1L;

	private static IUColumnConfig[] columnConfig;

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
				final MessageDialogWithLink dialog = new MessageDialogWithLink(ProvUI.getDefaultParentShell(), ProvUIMessages.ProvUI_InformationTitle, null, status.getMessage(), MessageDialog.INFORMATION, 0, IDialogConstants.OK_LABEL);
				if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
					dialog.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							ProvisioningUI.getDefaultUI().manipulateRepositories(dialog.getShell());
						}
					});
				}
				dialog.open();
				// unset the dialog bits
				style = style & ~StatusManager.BLOCK;
				style = style & ~StatusManager.SHOW;
				// unset logging for statuses that should never be logged.
				// Ideally the caller would do this but this bug keeps coming back.
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=274074
				if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE)
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

	//Factory method returning a new instance of a IUViewQueryContext
	public static IUViewQueryContext getQueryContext(Policy policy) {
		IUViewQueryContext queryContext = new IUViewQueryContext(policy.getGroupByCategory() ? IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY : IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		queryContext.setShowInstallChildren(policy.getShowDrilldownRequirements());
		queryContext.setShowProvisioningPlanChildren(policy.getShowDrilldownRequirements());

		//among other things the 4 calls below are used to control the available software dialog (AvailableIUPage)
		queryContext.setShowLatestVersionsOnly(policy.getShowLatestVersionsOnly());
		queryContext.setHideAlreadyInstalled(policy.getHideAlreadyInstalled());
		queryContext.setUseCategories(policy.getGroupByCategory());
		queryContext.setFilterOnEnv(policy.getFilterOnEnv());
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
			return ((IAdaptable) object).getAdapter(adapterType);
		return null;
	}

	/**
	 * Returns a shell that is appropriate to use as the parent
	 * for a modal dialog. 
	 */
	public static Shell getDefaultParentShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
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

	public static boolean isUpdateManagerInstallerPresent() {
		ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = commandService.getCommand(UPDATE_MANAGER_FIND_AND_INSTALL);
		return command.isDefined();
	}

	private static void runCommand(String commandId, String errorMessage, Event event) {
		ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = commandService.getCommand(commandId);
		if (!command.isDefined()) {
			return;
		}
		IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
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
		return QueryUtil.isCategory(iu);
	}

	private static void reportFail(String message, Throwable t) {
		Status failStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, message, t);
		reportStatus(failStatus, StatusManager.BLOCK | StatusManager.LOG);
	}

	/**
	 * Get sizing information about the specified plan.
	 * 
	 * @param engine the engine 
	 * @param plan the provisioning plan
	 * @param context the provisioning context to be used for the sizing
	 * @param monitor the progress monitor
	 * 
	 * @return a long integer describing the disk size required for the provisioning plan.
	 * 
	 * @see #SIZE_UNKNOWN
	 * @see #SIZE_UNAVAILABLE
	 * @see #SIZE_NOTAPPLICABLE
	 */
	public static long getSize(IEngine engine, IProvisioningPlan plan, ProvisioningContext context, IProgressMonitor monitor) {
		// If there is nothing to size, return 0
		if (plan == null)
			return SIZE_NOTAPPLICABLE;
		if (countPlanElements(plan) == 0)
			return 0;
		long installPlanSize = 0;
		SubMonitor mon = SubMonitor.convert(monitor, 300);
		if (plan.getInstallerPlan() != null) {
			ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
			IStatus status = engine.perform(plan.getInstallerPlan(), sizingPhaseSet, mon.newChild(100));
			if (status.isOK())
				installPlanSize = sizingPhaseSet.getDiskSize();
		} else {
			mon.worked(100);
		}
		ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
		IStatus status = engine.perform(plan, sizingPhaseSet, mon.newChild(200));
		if (status.isOK())
			return installPlanSize + sizingPhaseSet.getDiskSize();
		return SIZE_UNAVAILABLE;
	}

	private static int countPlanElements(IProvisioningPlan plan) {
		return QueryUtil.compoundQueryable(plan.getAdditions(), plan.getRemovals()).query(QueryUtil.createIUAnyQuery(), null).toUnmodifiableSet().size();
	}

	/**
	 * Return the artifact repository manager for the given session
	 * @return the repository manager
	 */
	public static IArtifactRepositoryManager getArtifactRepositoryManager(ProvisioningSession session) {
		return (IArtifactRepositoryManager) session.getProvisioningAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	/**
	 * Return the metadata repository manager for the given session
	 * @return the repository manager
	 */
	public static IMetadataRepositoryManager getMetadataRepositoryManager(ProvisioningSession session) {
		return (IMetadataRepositoryManager) session.getProvisioningAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
	}

	/**
	 * Return the profile registry for the given session
	 * @return the profile registry
	 */
	public static IProfileRegistry getProfileRegistry(ProvisioningSession session) {
		return (IProfileRegistry) session.getProvisioningAgent().getService(IProfileRegistry.SERVICE_NAME);
	}

	/**
	 * Return the provisioning engine for the given session
	 * @return the provisioning engine
	 */
	public static IEngine getEngine(ProvisioningSession session) {
		return (IEngine) session.getProvisioningAgent().getService(IEngine.SERVICE_NAME);
	}

	/**
	 * Return the provisioning event bus used for dispatching events.
	 * @return the event bus
	 */
	public static IProvisioningEventBus getProvisioningEventBus(ProvisioningSession session) {
		return (IProvisioningEventBus) session.getProvisioningAgent().getService(IProvisioningEventBus.SERVICE_NAME);
	}

}
