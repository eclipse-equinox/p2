/*******************************************************************************
 * Copyright (c) 2011 EclipseSource.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class CacheManagerTest extends TestCase {

	private static final int ONE_HOUR = 3600000;
	private URI repositoryLocation;
	private File contentXmlFile;
	private CacheManager cacheManager;
	private final String cachePrefix = "content"; //$NON-NLS-1$

	@Override
	protected void setUp() throws Exception {
		repositoryLocation = createRepistory();
		BundleContext bundle = Platform.getBundle("org.eclipse.equinox.p2.repository").getBundleContext();
		ServiceReference<IProvisioningAgentProvider> serviceReference = bundle.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider agentServiceFactory = bundle.getService(serviceReference);
		IProvisioningAgent agent = agentServiceFactory.createAgent(repositoryLocation);
		Transport transport = (Transport) agent.getService(Transport.SERVICE_NAME);
		cacheManager = new CacheManager(new AgentLocationMock(), transport);
	}

	@Override
	protected void tearDown() throws Exception {
		Path repositoryLocationPath = new Path(repositoryLocation.getPath());
		deleteFileOrDirectory(repositoryLocationPath.toFile());
	}

	public void testRepositoryDowngraded() throws ProvisionException, IOException {
		File cache = cacheManager.createCache(repositoryLocation, cachePrefix, new NullProgressMonitor());
		long lastModifiedInitial = cache.lastModified();
		// Do downgrade
		long lastModifiedContent = contentXmlFile.lastModified();
		contentXmlFile.setLastModified(lastModifiedContent - ONE_HOUR);

		File cache2 = cacheManager.createCache(repositoryLocation, cachePrefix, new NullProgressMonitor());

		assertFalse("Cache haven't been updated after repository downgrade", //$NON-NLS-1$
				lastModifiedInitial == cache2.lastModified());
	}

	public void testClientDifferentTimeZone() throws ProvisionException, IOException {
		File cache = cacheManager.createCache(repositoryLocation, cachePrefix, new NullProgressMonitor());
		long lastModifiedInitial = cache.lastModified();
		// do update
		contentXmlFile.setLastModified(contentXmlFile.lastModified() + ONE_HOUR / 2);
		// update client time zone to +1
		cache.setLastModified(lastModifiedInitial + 3600000);

		File cache2 = cacheManager.createCache(repositoryLocation, cachePrefix, new NullProgressMonitor());

		assertFalse("Cache haven't been updated after repository update", //$NON-NLS-1$
				lastModifiedInitial + ONE_HOUR == cache2.lastModified());
	}

	public void testRepositoryUpdate() throws ProvisionException, IOException {
		File cache = cacheManager.createCache(repositoryLocation, cachePrefix, new NullProgressMonitor());
		long lastModifiedInitial = cache.lastModified();
		// Do update
		long lastModifiedContent = contentXmlFile.lastModified();
		contentXmlFile.setLastModified(lastModifiedContent + ONE_HOUR);

		File cache2 = cacheManager.createCache(repositoryLocation, cachePrefix, new NullProgressMonitor());

		assertFalse("Cache haven't been updated after repository update", //$NON-NLS-1$
				lastModifiedInitial == cache2.lastModified());
	}

	private URI createRepistory() throws IOException {
		File repository = File.createTempFile("remoteFile", ""); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue(repository.delete());
		assertTrue(repository.mkdirs());
		IPath contentXmlPath = new Path(repository.getAbsolutePath()).append("content.xml"); //$NON-NLS-1$
		assertTrue(contentXmlPath.toFile().createNewFile());
		contentXmlFile = contentXmlPath.toFile();
		return repository.toURI();
	}

	private class AgentLocationMock implements IAgentLocation {

		public URI getDataArea(final String namespace) {
			return repositoryLocation;
		}

		public URI getRootLocation() {
			// ignore
			return null;
		}
	}

	private void deleteFileOrDirectory(final File path) {
		if (path.exists()) {
			if (path.isDirectory()) {
				handleDirectory(path);
			} else {
				assertTrue(path.delete());
			}
		}
	}

	private void handleDirectory(final File path) {
		File[] files = path.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				deleteFileOrDirectory(file);
			} else {
				assertTrue(file.delete());
			}
		}
	}

}
