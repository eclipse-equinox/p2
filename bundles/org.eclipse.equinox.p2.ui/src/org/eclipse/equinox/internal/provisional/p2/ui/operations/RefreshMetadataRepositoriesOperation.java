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
public class RefreshMetadataRepositoriesOperation extends RepositoryOperation {

	/**
	 * @param label
	 * @param urls
	 */
	public RefreshMetadataRepositoriesOperation(String label, URL[] urls) {
		super(label, urls);
	}

	public RefreshMetadataRepositoriesOperation(String label, int flags) {
		super(label, new URL[0]);
		try {
			this.urls = ProvisioningUtil.getMetadataRepositories(flags);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null, StatusManager.LOG);
		}
	}

	protected IStatus doBatchedExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		ProvisioningUtil.refreshMetadataRepositories(urls, monitor);
		return Status.OK_STATUS;
	}

	protected IStatus doBatchedUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		// Should never happen
		return Status.CANCEL_STATUS;
	}

	public boolean canUndo() {
		return false;
	}

}
