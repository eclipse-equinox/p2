/*******************************************************************************
 * Copyright (c) 2011, 2017 WindRiver Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorSelector;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.p2.transport.ecf.RepositoryTransport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
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

		IArtifactRepositoryManager mgr = getAgent().getService(IArtifactRepositoryManager.class);
		sourceRepository = (SimpleArtifactRepository) mgr.loadRepository(location, null);
	}

	@Override
	public void tearDown() throws Exception {
		IArtifactRepositoryManager mgr = getAgent().getService(IArtifactRepositoryManager.class);
		mgr.removeRepository(targetLocation.toURI());
		AbstractProvisioningTest.delete(targetLocation);
		super.tearDown();
	}

	public void testRetryMirrorAfterTimeout() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		// call test
		IArtifactKey key = new ArtifactKey("test.txt", "HelloWorldText", Version.parseVersion("1.0.0"));
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, null, getAgent().getService(Transport.class));
		MirrorRepo mirrorRepo = new MirrorRepo(sourceRepository);
		Field field = sourceRepository.getClass().getDeclaredField("mirrors");
		field.setAccessible(true);
		field.set(sourceRepository, new MirrorSelector(mirrorRepo, getAgent().getService(Transport.class)) {
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
			MirrorRequest request = new MirrorRequest(key, targetRepository, null, null, getAgent().getService(Transport.class));
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
			Map<String, String> newProperties = new HashMap<>(super.getProperties());
			newProperties.put(IRepository.PROP_MIRRORS_URL, getBaseURL() + "/mirrorrequest/mirrors.xml");
			newProperties.put(IRepository.PROP_MIRRORS_BASE_URL, getBaseURL() + "/mirrorrequest");
			return newProperties;
		}

		@Override
		public boolean contains(IArtifactDescriptor descriptor) {
			return delegate.contains(descriptor);
		}

		@Override
		public boolean contains(IArtifactKey key) {
			return delegate.contains(key);
		}

		@Override
		public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
			downloadCount++;
			return delegate.getArtifact(descriptor, destination, monitor);
		}

		@Override
		public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
			return delegate.getArtifactDescriptors(key);
		}

		@Override
		public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
			return delegate.getArtifacts(requests, monitor);
		}

		@Override
		public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException {
			return delegate.getOutputStream(descriptor);
		}

		@Override
		public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
			return delegate.getRawArtifact(descriptor, destination, monitor);
		}

		@Override
		public IQueryable<IArtifactDescriptor> descriptorQueryable() {
			return delegate.descriptorQueryable();
		}

		@Override
		public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
			return delegate.query(query, monitor);
		}
	}
}
