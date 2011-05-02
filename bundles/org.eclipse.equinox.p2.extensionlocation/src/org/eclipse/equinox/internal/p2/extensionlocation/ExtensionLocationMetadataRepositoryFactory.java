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
import org.eclipse.equinox.internal.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;

public class ExtensionLocationMetadataRepositoryFactory extends MetadataRepositoryFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IMetadataRepository create(URI location, String name, String type, Map<String, String> properties) throws ProvisionException {
		// TODO proper progress monitoring
		IStatus status = validate(location, null);
		if (!status.isOK())
			throw new ProvisionException(status);
		URI repoLocation = ExtensionLocationMetadataRepository.getLocalRepositoryLocation(location);
		// unexpected
		if (repoLocation == null)
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, Messages.failed_create_local_artifact_repository));
		// ensure that we aren't trying to create a repository at a location
		// where one already exists
		boolean failed = false;
		final SimpleMetadataRepositoryFactory simpleFactory = new SimpleMetadataRepositoryFactory();
		simpleFactory.setAgent(getAgent());
		try {
			simpleFactory.load(repoLocation, 0, null);
			failed = true;
		} catch (ProvisionException e) {
			// expected
		}
		if (failed) {
			String msg = NLS.bind(Messages.repo_already_exists, location.toString());
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_EXISTS, msg, null));
		}
		IMetadataRepository repository = simpleFactory.create(repoLocation, name, null, properties);
		return new ExtensionLocationMetadataRepository(getAgent(), location, repository, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory#load(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		//return null if the caller wanted a modifiable repo
		if ((flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) > 0) {
			return null;
		}

		// TODO proper progress monitoring
		IStatus status = validate(location, null);
		if (!status.isOK())
			throw new ProvisionException(status);
		URI repoLocation = ExtensionLocationMetadataRepository.getLocalRepositoryLocation(location);
		// unexpected
		if (repoLocation == null)
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, Messages.failed_create_local_artifact_repository));
		// TODO proper progress monitoring
		try {
			final SimpleMetadataRepositoryFactory simpleFactory = new SimpleMetadataRepositoryFactory();
			simpleFactory.setAgent(getAgent());
			IMetadataRepository repository = simpleFactory.load(repoLocation, flags, null);
			return new ExtensionLocationMetadataRepository(getAgent(), location, repository, monitor);
		} catch (ProvisionException e) {
			return create(location, Activator.getRepositoryName(location), ExtensionLocationMetadataRepository.TYPE, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory#validate(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus validate(URI location, IProgressMonitor monitor) {
		try {
			ExtensionLocationMetadataRepository.validate(location, monitor);
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

}
