/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * An object that queries a particular set of artifact repositories.
 */
public class QueryableArtifactRepositoryManager extends QueryableRepositoryManager<IArtifactKey> {

	public QueryableArtifactRepositoryManager(ProvisioningUI ui, boolean includeDisabledRepos) {
		super(ui, includeDisabledRepos);
	}

	protected IArtifactRepositoryManager getRepositoryManager() {
		return ProvUI.getArtifactRepositoryManager(getSession());
	}

	protected IArtifactRepository doLoadRepository(IRepositoryManager<IArtifactKey> manager, URI location, IProgressMonitor monitor) throws ProvisionException {
		return ui.loadArtifactRepository(location, false, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.QueryableRepositoryManager#getRepositoryFlags(org.eclipse.equinox.p2.ui.RepositoryManipulator)
	 */
	protected int getRepositoryFlags(RepositoryTracker repositoryManipulator) {
		return repositoryManipulator.getArtifactRepositoryFlags();
	}

	protected IArtifactRepository getRepository(IRepositoryManager<IArtifactKey> manager, URI location) {
		// note the use of ArtifactRepositoryManager (the concrete implementation).
		if (manager instanceof ArtifactRepositoryManager) {
			return ((ArtifactRepositoryManager) manager).getRepository(location);
		}
		return null;
	}
}
