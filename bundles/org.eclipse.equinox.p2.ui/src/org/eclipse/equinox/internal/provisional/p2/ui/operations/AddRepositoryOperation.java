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
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;

/**
 * Abstract class representing an operation that adds repositories,
 * using an optional nickname.
 * 
 * @since 3.5
 */
public abstract class AddRepositoryOperation extends RepositoryOperation {

	protected String[] nicknames;

	public AddRepositoryOperation(String label, URI[] locations) {
		super(label, locations);
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
			ProvUI.startBatchOperation();
			batched = true;
		}
		IStatus status = doBatchedExecute(monitor);
		if (nicknames != null) {
			for (int i = 0; i < nicknames.length; i++) {
				setNickname(locations[i], nicknames[i]);
			}
		}
		if (batched)
			ProvUI.endBatchOperation(notify);
		return status;
	}

	protected abstract void setNickname(URI location, String nickname) throws ProvisionException;

	protected abstract IStatus doBatchedExecute(IProgressMonitor monitor) throws ProvisionException;

	public void setNotify(boolean notify) {
		this.notify = notify;
	}

}
