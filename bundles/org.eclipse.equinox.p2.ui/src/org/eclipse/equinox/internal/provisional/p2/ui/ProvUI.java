/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.List;
import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.operations.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
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

	static ObjectUndoContext provisioningUndoContext;
	private static final int DEFAULT_COLUMN_WIDTH = 200;
	private static IUColumnConfig[] iuColumnConfig = new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, DEFAULT_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, DEFAULT_COLUMN_WIDTH)};

	/**
	 * List<URI> of repositories that have already been reported to the user as not found.
	 */
	private static final List reposNotFound = Collections.synchronizedList(new ArrayList());

	// These values rely on the command markup in org.eclipse.ui.ide that defines the update commands
	private static final String UPDATE_MANAGER_FIND_AND_INSTALL = "org.eclipse.ui.update.findAndInstallUpdates"; //$NON-NLS-1$
	private static final String UPDATE_MANAGER_MANAGE_CONFIGURATION = "org.eclipse.ui.update.manageConfiguration"; //$NON-NLS-1$
	// This value relies on the command markup in org.eclipse.ui 
	private static final String INSTALLATION_DIALOG = "org.eclipse.ui.help.installationDialog"; //$NON-NLS-1$

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

	public static IStatus handleException(Throwable t, String message, int style) {
		if (message == null && t != null) {
			message = t.getMessage();
		}
		IStatus status = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, message, t);
		StatusManager.getManager().handle(status, style);
		return status;
	}

	public static void reportNotFoundStatus(URI location, IStatus status, int style) {
		if (!hasNotFoundStatusBeenReported(location)) {
			reposNotFound.add(location);
			reportStatus(status, style);
		}
	}

	// This assumes that callers already checked whether it *should*
	// be reported so that we don't need to loop through the list
	// when the caller just has done so in order to know whether to report.
	public static void notFoundStatusReported(URI location) {
		reposNotFound.add(location);
	}

	// We don't check for things like case variants or end slash variants
	// because we know that the repository managers already did this.
	public static boolean hasNotFoundStatusBeenReported(URI location) {
		return reposNotFound.contains(location);
	}

	public static void clearRepositoriesNotFound() {
		reposNotFound.clear();
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
		return iuColumnConfig;
	}

	public static void setIUColumnConfig(IUColumnConfig[] columnConfig) {
		iuColumnConfig = columnConfig;
	}

	public static IUndoContext getProvisioningUndoContext() {
		if (provisioningUndoContext == null) {
			provisioningUndoContext = new ObjectUndoContext(new Object(), "Provisioning Undo Context"); //$NON-NLS-1$
			IOperationHistory opHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
			opHistory.addOperationApprover(getOperationApprover());
		}
		return provisioningUndoContext;
	}

	public static Object getAdapter(Object object, Class adapterType) {
		if (object == null)
			return null;
		if (adapterType.isInstance(object))
			return object;
		if (object instanceof IAdaptable)
			return ((IAdaptable) object).getAdapter(adapterType);
		return null;
	}

	/**
	 * Returns a shell that is appropriate to use as the parent
	 * for a modal dialog. This returns the existing modal dialog, if any,
	 * or a workbench window if no modal dialogs open. Returns <code>null</code>
	 * if there is no appropriate default parent.
	 * 
	 * This method is copied from ProgressManagerUtil#getDefaultParent()
	 */
	public static Shell getDefaultParentShell() {
		IWorkbench workbench = PlatformUI.getWorkbench();

		//look first for the topmost modal shell
		Shell shell = getDefaultParentShell(workbench.getDisplay().getShells());

		if (shell != null) {
			return shell;
		}

		//try the active workbench window
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window != null)
			return window.getShell();
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		if (windows.length > 0)
			return windows[0].getShell();
		//there is no modal shell and no active window, so just return a null parent shell
		return null;
	}

	/**
	 * Return the modal shell that is currently open. If there isn't one then
	 * return null.
	 * 
	 * @param shells shells to search for modal children
	 * @return the most specific modal child, or null if none
	 *
	 * This method is copied from ProgressManagerUtil#getDefaultParent()
	 */

	private static Shell getDefaultParentShell(Shell[] shells) {
		//first look for a modal shell
		for (int i = 0; i < shells.length; i++) {
			Shell shell = shells[i];

			// Check if this shell has a modal child
			Shell modalChild = getDefaultParentShell(shell.getShells());
			if (modalChild != null) {
				return modalChild;
			}

			// Do not worry about shells that will not block the user.
			if (shell.isVisible()) {
				int modal = SWT.APPLICATION_MODAL | SWT.SYSTEM_MODAL | SWT.PRIMARY_MODAL;
				if ((shell.getStyle() & modal) != 0) {
					return shell;
				}
			}
		}
		return null;
	}

	static IOperationApprover getOperationApprover() {
		return new IOperationApprover() {
			public IStatus proceedUndoing(final IUndoableOperation operation, IOperationHistory history, IAdaptable info) {
				final IStatus[] status = new IStatus[1];
				status[0] = Status.OK_STATUS;
				if (operation.hasContext(provisioningUndoContext) && operation instanceof IAdvancedUndoableOperation) {
					final IRunnableWithProgress runnable = new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) {
							try {
								status[0] = ((IAdvancedUndoableOperation) operation).computeUndoableStatus(monitor);
								if (!status[0].isOK()) {
									ProvUI.reportStatus(status[0], StatusManager.SHOW | StatusManager.LOG);
								}
							} catch (ExecutionException e) {
								status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getMessage(), e);
								ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
							}
						}
					};
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						public void run() {
							try {
								new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()).run(true, true, runnable);
							} catch (InterruptedException e) {
								// don't report thread interruption
							} catch (InvocationTargetException e) {
								status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getMessage(), e);
								ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
							}
						}
					});

				}
				return status[0];
			}

			public IStatus proceedRedoing(final IUndoableOperation operation, IOperationHistory history, IAdaptable info) {
				final IStatus[] status = new IStatus[1];
				status[0] = Status.OK_STATUS;
				if (operation.hasContext(provisioningUndoContext) && operation instanceof IAdvancedUndoableOperation) {
					final IRunnableWithProgress runnable = new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) {
							try {
								status[0] = ((IAdvancedUndoableOperation) operation).computeRedoableStatus(monitor);
								if (!status[0].isOK()) {
									ProvUI.reportStatus(status[0], StatusManager.SHOW);
								}
							} catch (ExecutionException e) {
								status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getMessage(), e);
								ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
							}
						}
					};
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
						public void run() {
							try {
								new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()).run(true, true, runnable);
							} catch (InterruptedException e) {
								// don't report thread interruption
							} catch (InvocationTargetException e) {
								status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getMessage(), e);
								ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
							}
						}
					});

				}
				return status[0];
			}

		};

	}

	/**
	 * Make an <code>IAdaptable</code> that adapts to the specified shell,
	 * suitable for passing for passing to any
	 * {@link org.eclipse.core.commands.operations.IUndoableOperation} or
	 * {@link org.eclipse.core.commands.operations.IOperationHistory} method
	 * that requires an {@link org.eclipse.core.runtime.IAdaptable}
	 * <code>uiInfo</code> parameter.
	 * 
	 * @param shell
	 *            the shell that should be returned by the IAdaptable when asked
	 *            to adapt a shell. If this parameter is <code>null</code>,
	 *            the returned shell will also be <code>null</code>.
	 * 
	 * @return an IAdaptable that will return the specified shell.
	 */
	public static IAdaptable getUIInfoAdapter(final Shell shell) {
		return new IAdaptable() {
			public Object getAdapter(Class clazz) {
				if (clazz == Shell.class) {
					return shell;
				}
				return null;
			}
		};
	}

	public static void addProvisioningListener(StructuredViewerProvisioningListener listener) {
		ProvUIActivator.getDefault().addProvisioningListener(listener);
	}

	public static void removeProvisioningListener(StructuredViewerProvisioningListener listener) {
		ProvUIActivator.getDefault().removeProvisioningListener(listener);
	}

	public static void startBatchOperation() {
		ProvUIActivator.getDefault().signalBatchOperationStart();
	}

	public static void endBatchOperation() {
		ProvUIActivator.getDefault().signalBatchOperationComplete();
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

	private static void reportFail(String message, Throwable t) {
		Status failStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, message, t);
		reportStatus(failStatus, StatusManager.BLOCK | StatusManager.LOG);

	}
}
