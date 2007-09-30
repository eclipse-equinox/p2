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
import org.eclipse.equinox.prov.ui.ColocatedRepositoryUtil;
import org.eclipse.equinox.prov.ui.ProvisioningUtil;

/**
 * Operation that adds colocated artifact and metadata repositories
 * given a URL.
 * 
 * @since 3.4
 */
public class AddColocatedRepositoryOperation extends RepositoryOperation {

	boolean added = false;

	public AddColocatedRepositoryOperation(String label, URL url, String name) {
		super(label, new URL[] {url}, new String[] {name});
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			URL metadataRepoURL = ColocatedRepositoryUtil.makeMetadataRepositoryURL(urls[i]);
			IRepositoryInfo repo = ProvisioningUtil.addMetadataRepository(metadataRepoURL, monitor, uiInfo);
			if (repo == null) {
				return failureStatus();
			}
			if (names[i] != null) {
				ProvisioningUtil.setRepositoryName(repo, names[i]);
			}
			repo = ProvisioningUtil.addArtifactRepository(ColocatedRepositoryUtil.makeArtifactRepositoryURL(urls[i]), monitor, uiInfo);
			if (repo == null) {
				// remove the metadata repo we just added
				ProvisioningUtil.removeMetadataRepository(metadataRepoURL, monitor, uiInfo);
				return failureStatus();
			}
		}
		added = true;
		return okStatus();
	}

	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			ProvisioningUtil.removeMetadataRepository(ColocatedRepositoryUtil.makeMetadataRepositoryURL(urls[i]), monitor, uiInfo);
			ProvisioningUtil.removeArtifactRepository(ColocatedRepositoryUtil.makeArtifactRepositoryURL(urls[i]), monitor, uiInfo);
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
