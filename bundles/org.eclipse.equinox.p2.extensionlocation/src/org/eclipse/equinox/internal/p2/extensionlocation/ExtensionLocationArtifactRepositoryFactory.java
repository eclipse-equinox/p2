/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.extensionlocation;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.osgi.util.NLS;

public class ExtensionLocationArtifactRepositoryFactory extends ArtifactRepositoryFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.ArtifactRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IArtifactRepository create(URI location, String name, String type, Map<String, String> properties) throws ProvisionException {
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
		final SimpleArtifactRepositoryFactory simpleFactory = new SimpleArtifactRepositoryFactory();
		simpleFactory.setAgent(getAgent());
		try {
			simpleFactory.load(repoLocation, 0, null);
			failed = true;
		} catch (ProvisionException e) {
			// expected
		}
		if (failed) {
			String msg = NLS.bind(Messages.repo_already_exists, location);
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_EXISTS, msg, null));
		}
		IFileArtifactRepository repo = (IFileArtifactRepository) simpleFactory.create(repoLocation, name, type, properties);
		return new ExtensionLocationArtifactRepository(getAgent(), location, repo, null);
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
			final SimpleArtifactRepositoryFactory simpleFactory = new SimpleArtifactRepositoryFactory();
			simpleFactory.setAgent(getAgent());
			IFileArtifactRepository repo = (IFileArtifactRepository) simpleFactory.load(repoLocation, flags, null);
			return new ExtensionLocationArtifactRepository(getAgent(), location, repo, monitor);
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
