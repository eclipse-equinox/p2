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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.configurator.Configurator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.ApplyProfileChangesDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
	public static final String REVERT_COMMAND_LABEL = ProvUIMessages.RevertIUCommandLabel;
	public static final String REVERT_COMMAND_TOOLTIP = ProvUIMessages.RevertIUCommandTooltip;

	static ObjectUndoContext provisioningUndoContext;
	private static final int DEFAULT_COLUMN_WIDTH = 200;
	private static IUColumnConfig[] iuColumnConfig = new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, DEFAULT_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, DEFAULT_COLUMN_WIDTH)};

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

	public static IStatus handleException(Throwable t, String message) {
		if (message == null && t != null) {
			message = t.getMessage();
		}
		IStatus status = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, message, t);
		StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
		return status;
	}

	public static void reportStatus(IStatus status) {
		// TODO investigate why platform status manager is so ugly with INFO status
		if (status.getSeverity() == IStatus.INFO) {
			MessageDialog.openInformation(null, ProvUIMessages.ProvUI_InformationTitle, status.getMessage());
			return;
		}
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
									StatusManager.getManager().handle(status[0], StatusManager.SHOW);
								}
							} catch (ExecutionException e) {
								status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getMessage(), e);
								ProvUI.handleException(e.getCause(), null);
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
								ProvUI.handleException(e.getCause(), null);
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
									StatusManager.getManager().handle(status[0], StatusManager.SHOW);
								}
							} catch (ExecutionException e) {
								status[0] = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getMessage(), e);
								ProvUI.handleException(e.getCause(), null);
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
								ProvUI.handleException(e.getCause(), null);
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
	static IAdaptable getUIInfoAdapter(final Shell shell) {
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
}
