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
 * Operation that removes the colocated repositories with the given URLs. *
 * 
 * @since 3.4
 */
public class RemoveColocatedRepositoryOperation extends RepositoryOperation {

	private boolean removed = false;

	public RemoveColocatedRepositoryOperation(String label, URI[] repoLocations) {
		super(label, repoLocations);
	}

	protected IStatus doBatchedExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < locations.length; i++) {
			ProvisioningUtil.removeMetadataRepository(locations[i], monitor);
			ProvisioningUtil.removeArtifactRepository(locations[i], monitor);
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
		for (int i = 0; i < locations.length; i++) {
			ProvisioningUtil.addMetadataRepository(locations[i]);
			ProvisioningUtil.addArtifactRepository(locations[i]);
		}
		removed = false;
		return okStatus();
	}
}
