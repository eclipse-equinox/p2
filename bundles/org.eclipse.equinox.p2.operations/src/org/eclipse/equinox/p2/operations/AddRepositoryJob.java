/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * Abstract class representing an operation that adds repositories,
 * using an optional nickname.
 * 
 * @since 2.0
 */
public abstract class AddRepositoryJob extends BatchedRepositoryJob {

	protected String[] nicknames;

	public AddRepositoryJob(String label, ProvisioningSession session, URI[] locations) {
		super(label, session, locations);
	}

	public void setNicknames(String[] nicknames) {
		Assert.isLegal(nicknames != null && nicknames.length == locations.length);
		this.nicknames = nicknames;
	}

	public boolean runInBackground() {
		return true;
	}

	protected IStatus doExecute(IProgressMonitor monitor) throws ProvisionException {
		boolean batched = false;
		if (locations != null && locations.length > 1) {
			getSession().signalBatchOperationStart();
			batched = true;
		}
		IStatus status = doBatchedOperation(monitor);
		if (nicknames != null) {
			for (int i = 0; i < nicknames.length; i++) {
				setNickname(locations[i], nicknames[i]);
			}
		}
		if (batched)
			getSession().signalBatchOperationComplete(notify, null);
		return status;
	}

	protected abstract void setNickname(URI location, String nickname) throws ProvisionException;

	protected abstract IStatus doBatchedOperation(IProgressMonitor monitor) throws ProvisionException;

	public void setNotify(boolean notify) {
		this.notify = notify;
	}

}
