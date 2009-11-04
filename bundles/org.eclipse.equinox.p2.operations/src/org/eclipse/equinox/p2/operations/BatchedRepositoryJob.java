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
package org.eclipse.equinox.p2.operations;

import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * Abstract class representing provisioning repository jobs
 * 
 * @since 2.0
 */
public abstract class BatchedRepositoryJob extends RepositoryJob {

	protected boolean notify = true;

	public BatchedRepositoryJob(String label, ProvisioningSession session, URI[] locations) {
		super(label, session, locations);
	}

	public boolean runInBackground() {
		return true;
	}

	public void runModal(IProgressMonitor monitor) throws ProvisionException {
		boolean batched = false;
		if (locations != null && locations.length > 1) {
			getSession().signalBatchOperationStart();
			batched = true;
		}
		try {
			doBatchedOperation(monitor);
		} finally {
			if (batched && notify)
				getSession().signalBatchOperationComplete(notify, null);
		}
	}

	protected abstract IStatus doBatchedOperation(IProgressMonitor monitor) throws ProvisionException;

	public void setNotify(boolean notify) {
		this.notify = notify;
	}

}
