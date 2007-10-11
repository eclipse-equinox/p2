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
package org.eclipse.equinox.p2.ui.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.ui.ProvUIActivator;
import org.eclipse.osgi.util.NLS;

/**
 * Abstract class representing provisioning operations
 * 
 * @since 3.4
 */

public abstract class ProvisioningOperation {

	private String label;

	public ProvisioningOperation(String label) {
		this.label = label;
	}

	/**
	 * 
	 */
	public IStatus execute(IProgressMonitor monitor, final IAdaptable uiInfo) throws ExecutionException {
		IStatus status;
		try {
			status = doExecute(monitor, uiInfo);
		} catch (final ProvisionException e) {
			throw new ExecutionException(NLS.bind(ProvUIMessages.ProvisioningOperation_ExecuteErrorTitle, label, e));
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		}
		return status;
	}

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

	protected IStatus okStatus() {
		return Status.OK_STATUS;
	}

	protected IStatus failureStatus() {
		return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, label);
	}

	public String getLabel() {
		return label;
	}

}
