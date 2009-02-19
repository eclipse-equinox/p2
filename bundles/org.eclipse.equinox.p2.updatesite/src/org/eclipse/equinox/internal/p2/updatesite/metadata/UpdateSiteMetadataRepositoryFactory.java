/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ray Braithwood (ray@genuitec.com) - fix for bug 220605
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import java.io.File;
import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.internal.p2.updatesite.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.MetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.p2.publisher.*;

public class UpdateSiteMetadataRepositoryFactory extends MetadataRepositoryFactory {
	private static final String PROP_SITE_CHECKSUM = "site.checksum"; //$NON-NLS-1$

	public static URI getLocalRepositoryLocation(URI location) throws ProvisionException {
		String stateDirName = Integer.toString(location.hashCode());
		File bundleData = Activator.getBundleContext().getDataFile(null);
		File stateDir = new File(bundleData, stateDirName);
		return stateDir.toURI();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.MetadataRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IMetadataRepository create(URI location, String name, String type, Map properties) {
		return null;
	}

	public IStatus validate(URI location, IProgressMonitor monitor) {
		try {
			UpdateSite.validate(location, monitor);
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	public IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		//return null if the caller wanted a modifiable repo
		if ((flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) > 0) {
			return null;
		}

		IMetadataRepository repository = loadRepository(location, monitor);
		initializeRepository(repository, location, monitor);
		return new UpdateSiteMetadataRepository(location, repository);
	}

	public IMetadataRepository loadRepository(URI location, IProgressMonitor monitor) throws ProvisionException {
		URI localRepositoryURL = getLocalRepositoryLocation(location);
		SimpleMetadataRepositoryFactory factory = new SimpleMetadataRepositoryFactory();
		try {
			return factory.load(localRepositoryURL, 0, monitor);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		String repositoryName = "update site: " + location; //$NON-NLS-1$
		return factory.create(localRepositoryURL, repositoryName, null, null);
	}

	public void initializeRepository(IMetadataRepository repository, URI location, IProgressMonitor monitor) throws ProvisionException {
		UpdateSite updateSite = UpdateSite.load(location, monitor);
		String savedChecksum = (String) repository.getProperties().get(PROP_SITE_CHECKSUM);
		if (savedChecksum != null && savedChecksum.equals(updateSite.getChecksum()))
			return;
		repository.setProperty(PROP_SITE_CHECKSUM, updateSite.getChecksum());
		repository.removeAll();
		IStatus status = generateMetadata(updateSite, repository, monitor);
		//site references should be published on load
		if (repository instanceof LocalMetadataRepository)
			((LocalMetadataRepository) repository).publishRepositoryReferences();
		if (monitor.isCanceled())
			throw new OperationCanceledException();
		if (!status.isOK())
			throw new ProvisionException(status);
	}

	private IStatus generateMetadata(UpdateSite updateSite, IMetadataRepository repository, IProgressMonitor monitor) {
		PublisherInfo info = new PublisherInfo();
		info.setMetadataRepository(repository);
		IPublisherAction[] actions = new IPublisherAction[] {new RemoteUpdateSiteAction(updateSite)};
		Publisher publisher = new Publisher(info);
		return publisher.publish(actions, monitor);
	}

}
