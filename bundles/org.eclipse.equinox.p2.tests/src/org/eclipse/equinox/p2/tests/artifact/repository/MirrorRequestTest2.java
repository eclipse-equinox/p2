/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorSelector;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.p2.transport.ecf.RepositoryTransport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.testserver.helper.AbstractTestServerClientCase;

public class MirrorRequestTest2 extends AbstractTestServerClientCase {

	private SimpleArtifactRepository sourceRepository;
	private File targetLocation;
	private SimpleArtifactRepository targetRepository;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		targetLocation = File.createTempFile("target", ".repo");
		targetLocation.delete();
		targetLocation.mkdirs();
		targetRepository = new SimpleArtifactRepository(getAgent(), "TargetRepo", targetLocation.toURI(), null);

		URI location = URI.create(getBaseURL() + "/mirrorrequest");

		IArtifactRepositoryManager mgr = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		sourceRepository = (SimpleArtifactRepository) mgr.loadRepository(location, null);
	}

	public void tearDown() throws Exception {
		IArtifactRepositoryManager mgr = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		mgr.removeRepository(targetLocation.toURI());
		AbstractProvisioningTest.delete(targetLocation);
		super.tearDown();
	}

	public void testRetryMirrorAfterTimeout() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		// call test
		IArtifactKey key = new ArtifactKey("test.txt", "HelloWorldText", Version.parseVersion("1.0.0"));
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, null, (Transport) getAgent().getService(Transport.SERVICE_NAME));
		MirrorRepo mirrorRepo = new MirrorRepo(sourceRepository);
		Field field = sourceRepository.getClass().getDeclaredField("mirrors");
		field.setAccessible(true);
		field.set(sourceRepository, new MirrorSelector(mirrorRepo, (Transport) getAgent().getService(Transport.SERVICE_NAME)) {
			private int count = 0;

			@Override
			public synchronized URI getMirrorLocation(URI inputLocation, IProgressMonitor monitor) {
				if (count++ == 0) {
					return inputLocation;
				}
				return URI.create(getBaseURL() + "/mirrorrequest/mirror-two/plugins/HelloWorldText_1.0.0.txt");
			}

			@Override
			public synchronized boolean hasValidMirror() {
				return true;
			}
		});

		request.perform(mirrorRepo, new NullProgressMonitor());

		// The download succeeded
		assertTrue(request.getResult().toString(), request.getResult().isOK());
	}

	public void testTimeoutForgivableAfterTimeout() {
		try {
			System.setProperty(RepositoryTransport.TIMEOUT_RETRY, "4");
			// call test
			IArtifactKey key = new ArtifactKey("test.txt", "HelloWorldText", Version.parseVersion("1.0.0"));
			MirrorRequest request = new MirrorRequest(key, targetRepository, null, null, (Transport) getAgent().getService(Transport.SERVICE_NAME));
			request.perform(sourceRepository, new NullProgressMonitor());

			// The download succeeded
			assertTrue(request.getResult().toString(), request.getResult().isOK());
		} finally {
			System.clearProperty(RepositoryTransport.TIMEOUT_RETRY);
		}
	}

	protected class MirrorRepo extends AbstractArtifactRepository {
		SimpleArtifactRepository delegate;
		int downloadCount = 0;

		MirrorRepo(SimpleArtifactRepository repo) {
			super(getAgent(), repo.getName(), repo.getType(), repo.getVersion(), repo.getLocation(), repo.getDescription(), repo.getProvider(), repo.getProperties());
			delegate = repo;
		}

		@Override
		public String getProperty(String key) {
			return getProperties().get(key);
		}

		@Override
		public synchronized Map<String, String> getProperties() {
			Map<String, String> newProperties = new HashMap<String, String>(super.getProperties());
			newProperties.put(IRepository.PROP_MIRRORS_URL, getBaseURL() + "/mirrorrequest/mirrors.xml");
			newProperties.put(IRepository.PROP_MIRRORS_BASE_URL, getBaseURL() + "/mirrorrequest");
			return newProperties;
		}

		public boolean contains(IArtifactDescriptor descriptor) {
			return delegate.contains(descriptor);
		}

		public boolean contains(IArtifactKey key) {
			return delegate.contains(key);
		}

		public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
			downloadCount++;
			return delegate.getArtifact(descriptor, destination, monitor);
		}

		public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
			return delegate.getArtifactDescriptors(key);
		}

		public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
			return delegate.getArtifacts(requests, monitor);
		}

		public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
			return delegate.getOutputStream(descriptor);
		}

		public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
			return delegate.getRawArtifact(descriptor, destination, monitor);
		}

		public IQueryable<IArtifactDescriptor> descriptorQueryable() {
			return delegate.descriptorQueryable();
		}

		public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
			return delegate.query(query, monitor);
		}
	}
}
