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

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;

/**
 * Operation that removes the colocated repositories with the given URLs. *
 * 
 * @since 3.4
 */
public class RemoveColocatedRepositoryOperation extends RepositoryOperation {

	private boolean removed = false;

	public RemoveColocatedRepositoryOperation(String label, URL[] repoURLs) {
		super(label, repoURLs);
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			ProvisioningUtil.removeMetadataRepository(urls[i], monitor);
			ProvisioningUtil.removeArtifactRepository(urls[i], monitor);
		}
		removed = true;
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canExecute()
	 */
	public boolean canExecute() {
		return !removed && super.canExecute();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canUndo()
	 */
	public boolean canUndo() {
		return removed && super.canUndo();
	}

	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			ProvisioningUtil.addMetadataRepository(urls[i]);
			ProvisioningUtil.addArtifactRepository(urls[i]);
		}
		removed = false;
		return okStatus();
	}
}
