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

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;

/**
 * Abstract class representing provisioning repository operations
 * 
 * @since 3.4
 */
abstract class RepositoryOperation extends UndoableProvisioningOperation {

	URL[] urls;

	RepositoryOperation(String label, URL[] urls) {
		super(label);
		this.urls = urls;
	}

	public boolean canExecute() {
		return urls != null;
	}

	public boolean canUndo() {
		return urls != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation#getAffectedObjects()
	 */
	public Object[] getAffectedObjects() {
		return urls;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation#runInBackground()
	 */
	public boolean runInBackground() {
		return true;
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		boolean batched = false;
		if (urls != null && urls.length > 1) {
			ProvUI.startBatchOperation();
			batched = true;
		}
		IStatus status = doBatchedExecute(monitor, uiInfo);
		if (batched)
			ProvUI.endBatchOperation();
		return status;
	}

	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		boolean batched = false;
		if (urls != null && urls.length > 1) {
			ProvUI.startBatchOperation();
			batched = true;
		}
		IStatus status = doBatchedUndo(monitor, uiInfo);
		if (batched)
			ProvUI.endBatchOperation();
		return status;
	}

	protected abstract IStatus doBatchedExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException;

	protected abstract IStatus doBatchedUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException;

}
