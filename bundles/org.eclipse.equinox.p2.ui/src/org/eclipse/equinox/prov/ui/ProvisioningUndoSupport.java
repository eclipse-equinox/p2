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

package org.eclipse.equinox.prov.ui;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Utility methods for clients using undo
 * 
 * @since 3.4
 */
public class ProvisioningUndoSupport {
	static ObjectUndoContext provisioningUndoContext;

	/**
	 * Return the undo context that should be used for operations involving
	 * provisioning.
	 * 
	 * @return the provisioning undo context
	 */
	public static IUndoContext getProvisioningUndoContext() {
		if (provisioningUndoContext == null) {
			provisioningUndoContext = new ObjectUndoContext(new Object(), "Provisioning Undo Context"); //$NON-NLS-1$
			IOperationHistory opHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
			opHistory.addOperationApprover(getOperationApprover());
		}
		return provisioningUndoContext;
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
}
