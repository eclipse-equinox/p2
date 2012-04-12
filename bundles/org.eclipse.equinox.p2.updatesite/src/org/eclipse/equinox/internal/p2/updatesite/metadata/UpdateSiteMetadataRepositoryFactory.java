/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ray Braithwood (ray@genuitec.com) - fix for bug 220605
 *     Code 9 - ongoing development
 *     Sonatype, Inc. - transport split
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.metadata;

import java.io.File;
import java.net.*;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.p2.updatesite.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.osgi.util.NLS;

public class UpdateSiteMetadataRepositoryFactory extends MetadataRepositoryFactory {
	private static final String PROP_SITE_CHECKSUM = "site.checksum"; //$NON-NLS-1$

	public static URI getLocalRepositoryLocation(URI location) {
		String stateDirName = Integer.toString(location.hashCode());
		File bundleData = Activator.getBundleContext().getDataFile(null);
		File stateDir = new File(bundleData, stateDirName);
		return stateDir.toURI();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IMetadataRepository create(URI location, String name, String type, Map<String, String> properties) {
		return null;
	}

	public IMetadataRepository load(URI location, int flags, IProgressMonitor monitor) throws ProvisionException {
		//return null if the caller wanted a modifiable repo
		if ((flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE) > 0) {
			return null;
		}
		if (!isURL(location)) {
			return null;
		}

		IMetadataRepository repository = loadRepository(location, monitor);
		try {
			initializeRepository(repository, location, monitor);
		} catch (Exception e) {
			resetCache(repository);
			if (e instanceof ProvisionException)
				throw (ProvisionException) e;
			if (e instanceof OperationCanceledException)
				throw (OperationCanceledException) e;
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Unexpected_exception, location.toString()), e));
		}
		return new UpdateSiteMetadataRepository(location, repository);
	}

	private static boolean isURL(URI location) {
		try {
			new URL(location.toASCIIString());
		} catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	private void resetCache(IMetadataRepository repository) {
		repository.setProperty(PROP_SITE_CHECKSUM, "0"); //$NON-NLS-1$
		repository.removeAll();
	}

	public IMetadataRepository loadRepository(URI location, IProgressMonitor monitor) {
		URI localRepositoryURL = getLocalRepositoryLocation(location);
		SimpleMetadataRepositoryFactory factory = new SimpleMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		try {
			return factory.load(localRepositoryURL, 0, monitor);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		String repositoryName = "update site: " + location; //$NON-NLS-1$
		return factory.create(localRepositoryURL, repositoryName, null, null);
	}

	public void initializeRepository(IMetadataRepository repository, URI location, IProgressMonitor monitor) throws ProvisionException {
		UpdateSite updateSite = UpdateSite.load(location, (Transport) getAgent().getService(Transport.SERVICE_NAME), monitor);
		String savedChecksum = repository.getProperties().get(PROP_SITE_CHECKSUM);
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
		IPublisherAction[] actions = new IPublisherAction[] {new RemoteUpdateSiteAction(updateSite, null)};
		Publisher publisher = new Publisher(info);
		return publisher.publish(actions, monitor);
	}

}
