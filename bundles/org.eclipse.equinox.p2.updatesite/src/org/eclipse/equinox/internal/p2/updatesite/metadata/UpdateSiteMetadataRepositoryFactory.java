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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.updatesite.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.p2.publisher.*;

public class UpdateSiteMetadataRepositoryFactory implements IMetadataRepositoryFactory {
	private static final String PROP_SITE_CHECKSUM = "site.checksum"; //$NON-NLS-1$

	public static URL getLocalRepositoryLocation(URL location) throws ProvisionException {
		URL localRepositoryURL = null;
		try {
			String stateDirName = Integer.toString(location.toExternalForm().hashCode());
			File bundleData = Activator.getBundleContext().getDataFile(null);
			File stateDir = new File(bundleData, stateDirName);
			localRepositoryURL = stateDir.toURL();
		} catch (MalformedURLException e) {
			throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, Messages.ErrorCreatingRepository, e));
		}
		return localRepositoryURL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory#create(java.net.URL, java.lang.String, java.lang.String, java.util.Map)
	 */
	public IMetadataRepository create(URL location, String name, String type, Map properties) {
		return null;
	}

	public IStatus validate(URL location, IProgressMonitor monitor) {
		try {
			UpdateSite.validate(location, monitor);
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	public IMetadataRepository load(URL location, IProgressMonitor monitor) throws ProvisionException {
		IMetadataRepository repository = loadRepository(location, monitor);
		initializeRepository(repository, location, monitor);
		return repository;
	}

	public IMetadataRepository loadRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		URL localRepositoryURL = getLocalRepositoryLocation(location);
		SimpleMetadataRepositoryFactory factory = new SimpleMetadataRepositoryFactory();
		try {
			return factory.load(localRepositoryURL, null);
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}
		String repositoryName = "update site: " + location.toExternalForm(); //$NON-NLS-1$
		return factory.create(localRepositoryURL, repositoryName, null, null);
	}

	public void initializeRepository(IMetadataRepository repository, URL location, IProgressMonitor monitor) throws ProvisionException {
		UpdateSite updateSite = UpdateSite.load(location, null);
		String savedChecksum = (String) repository.getProperties().get(PROP_SITE_CHECKSUM);
		if (savedChecksum != null && savedChecksum.equals(updateSite.getChecksum()))
			return;
		repository.setProperty(PROP_SITE_CHECKSUM, updateSite.getChecksum());
		repository.removeAll();
		generateMetadata(updateSite, repository);
	}

	private void generateMetadata(UpdateSite updateSite, IMetadataRepository repository) {
		PublisherInfo info = new PublisherInfo();
		info.setMetadataRepository(repository);
		IPublisherAction[] actions = new IPublisherAction[] {new RemoteUpdateSiteAction(updateSite)};
		Publisher publisher = new Publisher(info);
		IStatus result = publisher.publish(actions);
	}

}
