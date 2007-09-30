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
package org.eclipse.equinox.prov.ui.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.prov.core.ProvisionException;
import org.eclipse.equinox.prov.ui.ProvUIActivator;
import org.eclipse.equinox.prov.ui.ProvisioningUndoSupport;
import org.eclipse.equinox.prov.ui.internal.ProvUIMessages;
import org.eclipse.osgi.util.NLS;

/**
 * Abstract class representing provisioning operations
 * 
 * @since 3.4
 */

public abstract class ProvisioningOperation extends AbstractOperation implements IAdvancedUndoableOperation, IAdvancedUndoableOperation2 {

	/*
	 * Specifies whether any user prompting is appropriate while computing
	 * status.
	 */
	protected boolean quietCompute = false;

	public ProvisioningOperation(String label) {
		super(label);
		addContext(ProvisioningUndoSupport.getProvisioningUndoContext());
	}

	/**
	 * 
	 */
	public IStatus execute(IProgressMonitor monitor, final IAdaptable uiInfo) throws ExecutionException {
		IStatus status;
		try {
			status = doExecute(monitor, uiInfo);
		} catch (final ProvisionException e) {
			throw new ExecutionException(NLS.bind(ProvUIMessages.ProvisioningOperation_ExecuteErrorTitle, getLabel()), e);
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		}
		return status;
	}

	/**
	 * 
	 */
	public IStatus redo(IProgressMonitor monitor, final IAdaptable uiInfo) throws ExecutionException {
		IStatus status;
		try {
			status = doExecute(monitor, uiInfo);
		} catch (final ProvisionException e) {
			throw new ExecutionException(NLS.bind(ProvUIMessages.ProvisioningOperation_RedoErrorTitle, getLabel()), e);
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		}
		return status;
	}

	/**
	 * 
	 */
	public IStatus undo(IProgressMonitor monitor, final IAdaptable uiInfo) throws ExecutionException {
		IStatus status;
		try {
			status = doUndo(monitor, uiInfo);
		} catch (final ProvisionException e) {
			throw new ExecutionException(NLS.bind(ProvUIMessages.ProvisioningOperation_UndoErrorTitle, getLabel()), e);
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		}
		return status;
	}

	/**
	 * Perform the specific work involved in undoing this operation.
	 * 
	 * @param monitor
	 *            the progress monitor to use for the operation
	 * @param uiInfo
	 *            the IAdaptable (or <code>null</code>) provided by the
	 *            caller in order to supply UI information for prompting the
	 *            user if necessary. When this parameter is not
	 *            <code>null</code>, it contains an adapter for the
	 *            org.eclipse.swt.widgets.Shell.class
	 * @throws ProvisionException
	 *             propagates any ProvisionException thrown
	 */
	protected abstract IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException;

	/**
	 * Perform the specific work involved in executing this operation.
	 * 
	 * @param monitor
	 *            the progress monitor to use for the operation
	 * @param uiInfo
	 *            the IAdaptable (or <code>null</code>) provided by the
	 *            caller in order to supply UI information for prompting the
	 *            user if necessary. When this parameter is not
	 *            <code>null</code>, it contains an adapter for the
	 *            org.eclipse.swt.widgets.Shell.class
	 * @throws ProvisionException
	 *             propagates any ProvisionException thrown
	 * 
	 */
	protected abstract IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canRedo()
	 */
	public boolean canRedo() {
		return canExecute();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation#aboutToNotify(org.eclipse.core.commands.operations.OperationHistoryEvent)
	 */
	public void aboutToNotify(OperationHistoryEvent event) {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation#getAffectedObjects()
	 */
	public Object[] getAffectedObjects() {
		return new Object[0];
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation#computeRedoableStatus(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus computeRedoableStatus(IProgressMonitor monitor) throws ExecutionException {
		return computeExecutionStatus(monitor);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation#computeUndoableStatus(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus computeUndoableStatus(IProgressMonitor monitor) throws ExecutionException {
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation2#computeExecutionStatus(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus computeExecutionStatus(IProgressMonitor monitor) throws ExecutionException {
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation2#runInBackground()
	 */
	public boolean runInBackground() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation2#setQuietCompute(boolean)
	 */
	public void setQuietCompute(boolean quiet) {
		quietCompute = quiet;
	}

	protected IStatus okStatus() {
		return Status.OK_STATUS;
	}

	protected IStatus failureStatus() {
		return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, getLabel());
	}

}
