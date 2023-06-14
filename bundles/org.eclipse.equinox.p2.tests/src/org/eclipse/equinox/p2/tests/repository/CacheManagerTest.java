/*******************************************************************************
 * Copyright (c) 2011, 2017 EclipseSource.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.internal.p2.repository.Messages;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.util.NLS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class CacheManagerTest {

	private static final int ONE_HOUR = 3600000;
	URI repositoryLocation;
	private File contentXmlFile;
	private CacheManager cacheManager;
	private final String cachePrefix = "content"; //$NON-NLS-1$

	@Before
	public void setUp() throws Exception {
		repositoryLocation = createRepistory();
		BundleContext bundle = Platform.getBundle("org.eclipse.equinox.p2.repository").getBundleContext();
		ServiceReference<IProvisioningAgentProvider> serviceReference = bundle.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider agentServiceFactory = bundle.getService(serviceReference);
		IProvisioningAgent agent = agentServiceFactory.createAgent(repositoryLocation);
		Transport transport = agent.getService(Transport.class);
		cacheManager = new CacheManager(new AgentLocationMock(), transport);
	}

	@After
	public void tearDown() throws Exception {
		deleteFileOrDirectory(new File(repositoryLocation));
	}

	@Test
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

	@Test
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

	@Test
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

	/**
	 * https://github.com/eclipse-equinox/p2/issues/257
	 */
	@Test
	public void testStatusCodeMustBeRepositoryNotFoundInCaseOfFileNotFound() throws IOException, ProvisionException {
		CacheManager cacheManagerWithStubTransport = new CacheManager(new AgentLocationMock(), new Transport() {

			@Override
			public InputStream stream(URI toDownload, IProgressMonitor monitor)
					throws FileNotFoundException, CoreException, AuthenticationFailedException {
				throw new UnsupportedOperationException();
			}

			@Override
			public long getLastModified(URI toDownload, IProgressMonitor monitor)
					throws CoreException, FileNotFoundException, AuthenticationFailedException {
				throw new FileNotFoundException();
			}

			@Override
			public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
				throw new UnsupportedOperationException();
			}
		});

		URI uriNonExistent = URI.create("https://foo.bar/nonexistent/content.xml.xz");
		try {
			cacheManagerWithStubTransport.createCacheFromFile(uriNonExistent, new NullProgressMonitor());
			fail(FileNotFoundException.class.getName() + " expected");
		} catch (FileNotFoundException e) {
			assertEquals(NLS.bind(Messages.CacheManager_Repository_not_found, uriNonExistent.toString()),
					e.getMessage());
		}
	}

	private URI createRepistory() throws IOException {
		File repository = File.createTempFile("remoteFile", ""); //$NON-NLS-1$//$NON-NLS-2$
		repository.deleteOnExit();
		assertTrue(repository.delete());
		assertTrue(repository.mkdirs());
		File contentXmlPath = new File(repository, "content.xml"); //$NON-NLS-1$
		assertTrue(contentXmlPath.createNewFile());
		contentXmlFile = contentXmlPath;
		return repository.toURI();
	}

	class AgentLocationMock implements IAgentLocation {

		@Override
		public URI getDataArea(final String namespace) {
			return repositoryLocation;
		}

		@Override
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
