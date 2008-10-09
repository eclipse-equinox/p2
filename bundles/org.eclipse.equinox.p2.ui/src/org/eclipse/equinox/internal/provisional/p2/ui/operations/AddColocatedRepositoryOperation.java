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

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * Operation that adds colocated artifact and metadata repositories
 * given a URL.
 * 
 * @since 3.4
 */
public class AddColocatedRepositoryOperation extends RepositoryOperation {

	boolean added = false;

	public AddColocatedRepositoryOperation(String label, URI url) {
		super(label, new URI[] {url});
	}

	public AddColocatedRepositoryOperation(String label, URI[] urls) {
		super(label, urls);
	}

	protected IStatus doBatchedExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < locations.length; i++) {
			ProvisioningUtil.addMetadataRepository(locations[i]);
			ProvisioningUtil.addArtifactRepository(locations[i]);
		}
		added = true;
		return okStatus();
	}

	protected IStatus doBatchedUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < locations.length; i++) {
			ProvisioningUtil.removeMetadataRepository(locations[i], monitor);
			ProvisioningUtil.removeArtifactRepository(locations[i], monitor);
		}
		added = false;
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canExecute()
	 */
	public boolean canExecute() {
		return super.canExecute() && !added;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canUndo()
	 */
	public boolean canUndo() {
		return super.canUndo() && added;
	}
}
