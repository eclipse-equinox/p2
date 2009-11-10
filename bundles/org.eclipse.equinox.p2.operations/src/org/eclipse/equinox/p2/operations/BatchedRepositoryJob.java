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
import org.eclipse.core.runtime.*;
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

	public IStatus runModal(IProgressMonitor monitor) {
		boolean batched = false;
		if (locations != null && locations.length > 1) {
			getSession().signalBatchOperationStart();
			batched = true;
		}
		try {
			doBatchedOperation(monitor);
		} catch (ProvisionException e) {
			return getErrorStatus(null, e);
		} finally {
			if (batched && notify)
				getSession().signalBatchOperationComplete(notify, null);
		}
		return Status.OK_STATUS;
	}

	protected abstract IStatus doBatchedOperation(IProgressMonitor monitor) throws ProvisionException;

	public void setNotify(boolean notify) {
		this.notify = notify;
	}

}
