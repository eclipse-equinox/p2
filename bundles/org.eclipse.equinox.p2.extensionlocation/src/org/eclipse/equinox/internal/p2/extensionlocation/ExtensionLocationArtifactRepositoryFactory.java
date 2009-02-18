/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.extensionlocation;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.ArtifactRepositoryFactory;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.SimpleArtifactRepositoryFactory;
import org.eclipse.osgi.util.NLS;

public class ExtensionLocationArtifactRepositoryFactory extends ArtifactRepositoryFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.ArtifactRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IArtifactRepository create(URI location, String name, String type, Map properties) throws ProvisionException {
		// TODO proper progress monitoring
		IStatus status = validate(location, null);
		if (!status.isOK())
			throw new ProvisionException(status);
		URI repoLocation = ExtensionLocationArtifactRepository.getLocalRepositoryLocation(location);
		// unexpected
		if (repoLocation == null)
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, Messages.failed_create_local_artifact_repository));
		// make sure that we aren't trying to create a repo at a location
		// where one already exists
		boolean failed = false;
		try {
			new SimpleArtifactRepositoryFactory().load(repoLocation, 0, null);
			failed = true;
		} catch (ProvisionException e) {
			// expected
		}
		if (failed) {
			String msg = NLS.bind(Messages.repo_already_exists, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_EXISTS, msg, null));
		}
		IFileArtifactRepository repo = (IFileArtifactRepository) new SimpleArtifactRepositoryFactory().create(repoLocation, name, type, properties);
		return new ExtensionLocationArtifactRepository(location, repo, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.ArtifactRepositoryFactory#load(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IArtifactRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		//return null if the caller wanted a modifiable repo
		if ((flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) > 0) {
			return null;
		}

		// TODO proper progress monitoring
		IStatus status = validate(location, null);
		if (!status.isOK())
			throw new ProvisionException(status);
		URI repoLocation = ExtensionLocationArtifactRepository.getLocalRepositoryLocation(location);
		// unexpected
		if (repoLocation == null)
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, Messages.failed_create_local_artifact_repository));
		// TODO proper progress monitoring
		try {
			IFileArtifactRepository repo = (IFileArtifactRepository) new SimpleArtifactRepositoryFactory().load(repoLocation, flags, null);
			return new ExtensionLocationArtifactRepository(location, repo, monitor);
		} catch (ProvisionException e) {
			return create(location, Activator.getRepositoryName(location), ExtensionLocationArtifactRepository.TYPE, null);
		}
	}

	public IStatus validate(URI location, IProgressMonitor monitor) {
		try {
			ExtensionLocationArtifactRepository.validate(location, monitor);
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

}
