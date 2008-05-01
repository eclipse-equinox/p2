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

import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;

public class ExtensionLocationMetadataRepositoryFactory implements IMetadataRepositoryFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IMetadataRepository create(URL location, String name, String type, Map properties) throws ProvisionException {
		// TODO proper progress monitoring
		IStatus status = validate(location, null);
		if (!status.isOK())
			throw new ProvisionException(status);
		URL repoLocation = ExtensionLocationMetadataRepository.getLocalRepositoryLocation(location);
		// unexpected
		if (repoLocation == null)
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, Messages.failed_create_local_artifact_repository));
		// ensure that we aren't trying to create a repository at a location
		// where one already exists
		try {
			new SimpleMetadataRepositoryFactory().load(repoLocation, null);
			String msg = NLS.bind(Messages.repo_already_exists, location.toExternalForm());
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_EXISTS, msg, null));
		} catch (ProvisionException e) {
			// expected
		}
		IMetadataRepository repository = new SimpleMetadataRepositoryFactory().create(repoLocation, name, null, properties);
		return new ExtensionLocationMetadataRepository(location, repository, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory#load(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IMetadataRepository load(URL location, IProgressMonitor monitor) throws ProvisionException {
		// TODO proper progress monitoring
		IStatus status = validate(location, null);
		if (!status.isOK())
			throw new ProvisionException(status);
		URL repoLocation = ExtensionLocationMetadataRepository.getLocalRepositoryLocation(location);
		// unexpected
		if (repoLocation == null)
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, Messages.failed_create_local_artifact_repository));
		// TODO proper progress monitoring
		try {
			IMetadataRepository repository = new SimpleMetadataRepositoryFactory().load(repoLocation, null);
			return new ExtensionLocationMetadataRepository(location, repository, monitor);
		} catch (ProvisionException e) {
			return create(location, "Extension location repository: " + location.toExternalForm(), ExtensionLocationMetadataRepository.TYPE, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory#validate(java.net.URL, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus validate(URL location, IProgressMonitor monitor) {
		try {
			ExtensionLocationMetadataRepository.validate(location, monitor);
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

}
