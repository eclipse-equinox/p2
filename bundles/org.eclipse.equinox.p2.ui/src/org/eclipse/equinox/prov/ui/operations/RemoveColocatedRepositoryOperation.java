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
package org.eclipse.equinox.prov.ui.operations;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.prov.core.ProvisionException;
import org.eclipse.equinox.prov.core.repository.IRepositoryInfo;
import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.prov.ui.ColocatedRepositoryUtil;
import org.eclipse.equinox.prov.ui.ProvisioningUtil;

/**
 * Operation that removes the colocated repositories with the given URLs. *
 * 
 * @since 3.4
 */
public class RemoveColocatedRepositoryOperation extends RepositoryOperation {

	private boolean removed = false;

	public RemoveColocatedRepositoryOperation(String label, IMetadataRepository[] repos) {
		super(label, new URL[repos.length], new String[repos.length]);
		for (int i = 0; i < repos.length; i++) {
			urls[i] = ColocatedRepositoryUtil.makeColocatedRepositoryURL(repos[i].getLocation());
			names[i] = repos[i].getName();
		}
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			ProvisioningUtil.removeMetadataRepository(ColocatedRepositoryUtil.makeMetadataRepositoryURL(urls[i]), monitor, uiInfo);
			ProvisioningUtil.removeArtifactRepository(ColocatedRepositoryUtil.makeArtifactRepositoryURL(urls[i]), monitor, uiInfo);
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
			URL metadataURL = ColocatedRepositoryUtil.makeMetadataRepositoryURL(urls[i]);
			IRepositoryInfo repo = ProvisioningUtil.addMetadataRepository(metadataURL, monitor, uiInfo);
			if (repo == null) {
				return failureStatus();
			}
			if (names[i] != null) {
				ProvisioningUtil.setRepositoryName(repo, names[i]);
			}

			repo = ProvisioningUtil.addArtifactRepository(ColocatedRepositoryUtil.makeArtifactRepositoryURL(urls[i]), monitor, uiInfo);
			if (repo == null) {
				// remove the metadata repo we just added
				ProvisioningUtil.removeMetadataRepository(metadataURL, monitor, uiInfo);
				return failureStatus();
			}

		}
		removed = false;
		return okStatus();
	}
}
