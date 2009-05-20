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
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;

/**
 * Abstract class representing provisioning repository operations
 * 
 * @since 3.4
 */
public abstract class RepositoryOperation extends ProvisioningOperation {

	protected URI[] locations;
	protected boolean notify = true;

	public RepositoryOperation(String label, URI[] urls) {
		super(label);
		this.locations = urls;
	}

	public boolean runInBackground() {
		return true;
	}

	protected IStatus doExecute(IProgressMonitor monitor) throws ProvisionException {
		boolean batched = false;
		if (locations != null && locations.length > 1) {
			ProvUI.startBatchOperation();
			batched = true;
		}
		IStatus status = doBatchedExecute(monitor);
		if (batched && notify)
			ProvUI.endBatchOperation(notify);
		return status;
	}

	protected abstract IStatus doBatchedExecute(IProgressMonitor monitor) throws ProvisionException;

	public void setNotify(boolean notify) {
		this.notify = notify;
	}

}
