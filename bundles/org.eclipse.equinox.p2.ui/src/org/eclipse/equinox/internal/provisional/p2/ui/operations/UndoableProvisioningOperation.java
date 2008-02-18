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
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.osgi.util.NLS;

/**
 * Abstract class representing an undoable provisioning operations
 * 
 * @since 3.4
 */
abstract class UndoableProvisioningOperation extends ProvisioningOperation implements IUndoableOperation, IAdvancedUndoableOperation2 {

	boolean quietCompute = false;
	List contexts = new ArrayList();

	UndoableProvisioningOperation(String label) {
		super(label);
		addContext(ProvUI.getProvisioningUndoContext());
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

	public boolean canExecute() {
		return true;
	}

	public boolean canRedo() {
		return canExecute();
	}

	public boolean canUndo() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation#computeRedoableStatus(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus computeRedoableStatus(IProgressMonitor monitor) {
		return computeExecutionStatus(monitor);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation#computeUndoableStatus(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus computeUndoableStatus(IProgressMonitor monitor) {
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation2#computeExecutionStatus(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus computeExecutionStatus(IProgressMonitor monitor) {
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation2#setQuietCompute(boolean)
	 */
	public void setQuietCompute(boolean quiet) {
		quietCompute = quiet;
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
	 * @see org.eclipse.core.commands.operations.IUndoableOperation#addContext(org.eclipse.core.commands.operations.IUndoContext)
	 * 
	 * <p> Subclasses may override this method. </p>
	 */
	public void addContext(IUndoContext context) {
		if (!contexts.contains(context)) {
			contexts.add(context);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.IUndoableOperation#removeContext(org.eclipse.core.commands.operations.IUndoContext)
	 *      <p> Default implementation. Subclasses may override this method.
	 *      </p>
	 */

	public void removeContext(IUndoContext context) {
		contexts.remove(context);
	}

	public final IUndoContext[] getContexts() {
		return (IUndoContext[]) contexts.toArray(new IUndoContext[contexts.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.IUndoableOperation#hasContext(org.eclipse.core.commands.operations.IUndoContext)
	 */
	public final boolean hasContext(IUndoContext context) {
		Assert.isNotNull(context);
		for (int i = 0; i < contexts.size(); i++) {
			IUndoContext otherContext = (IUndoContext) contexts.get(i);
			// have to check both ways because one context may be more general
			// in its matching rules than another.
			if (context.matches(otherContext) || otherContext.matches(context)) {
				return true;
			}
		}
		return false;
	}

	protected IStatus okStatus() {
		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.IUndoableOperation#dispose()
	 *      <p> Default implementation. Subclasses may override this method.
	 *      </p>
	 */
	public void dispose() {
		// nothing to dispose.
	}
}
