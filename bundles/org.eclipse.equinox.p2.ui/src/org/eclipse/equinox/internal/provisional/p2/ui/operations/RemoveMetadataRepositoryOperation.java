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

/**
 * Operation that removes the metadata repository with the given URL. *
 * 
 * @since 3.4
 */
public class RemoveMetadataRepositoryOperation extends RepositoryOperation {

	private boolean removed = false;

	public RemoveMetadataRepositoryOperation(String label, URL[] repoURLs) {
		super(label, repoURLs);
	}

	protected IStatus doBatchedExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			ProvisioningUtil.removeMetadataRepository(urls[i], monitor);
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

	protected IStatus doBatchedUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			ProvisioningUtil.addMetadataRepository(urls[i]);
		}
		removed = false;
		return okStatus();
	}
}
