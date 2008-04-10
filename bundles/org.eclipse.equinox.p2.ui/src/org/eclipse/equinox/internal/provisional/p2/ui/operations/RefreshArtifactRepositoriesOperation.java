/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.4
 *
 */
public class RefreshArtifactRepositoriesOperation extends RepositoryOperation {

	/**
	 * @param label
	 * @param urls
	 */
	public RefreshArtifactRepositoriesOperation(String label, URL[] urls) {
		super(label, urls);
	}

	public RefreshArtifactRepositoriesOperation(String label, int flags) {
		super(label, new URL[0]);
		try {
			this.urls = ProvisioningUtil.getArtifactRepositories(flags);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null, StatusManager.LOG);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.operations.UndoableProvisioningOperation#doExecute(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 */
	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		ProvisioningUtil.refreshArtifactRepositories(urls, monitor);
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.operations.UndoableProvisioningOperation#doUndo(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 */
	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		// Should never happen
		return Status.CANCEL_STATUS;
	}

	public boolean canUndo() {
		return false;
	}

}
