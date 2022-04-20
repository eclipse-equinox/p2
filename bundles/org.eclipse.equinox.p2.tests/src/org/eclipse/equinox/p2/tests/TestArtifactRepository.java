/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRequest;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.p2.repository.helpers.AbstractRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.junit.Assert;

/**
 * A simple artifact repository implementation used for testing purposes.
 * All artifacts are kept in memory.
 */
public class TestArtifactRepository extends AbstractArtifactRepository {
	private static final String SCHEME = "testartifactrepo";
	private static final String NAME = "ATestArtifactRepository"; //$NON-NLS-1$
	private static final String TYPE = "testartifactrepo"; //$NON-NLS-1$
	private static final String VERSION = "1"; //$NON-NLS-1$
	private static final String PROVIDER = "org.eclipse"; //$NON-NLS-1$
	private static final String DESCRIPTION = "A Test Artifact Repository"; //$NON-NLS-1$

	/**
	 * Map of IArtifactKey -> URI (location)
	 */
	Map<IArtifactKey, URI> keysToLocations = new HashMap<>();
	/**
	 * Map of URI (location) -> byte[] (contents)
	 */
	Map<URI, byte[]> locationsToContents = new HashMap<>();

	/**
	 * 	Set of artifact descriptors
	 */
	Set<IArtifactDescriptor> artifactDescriptors = new HashSet<>();

	Transport testhandler = new Transport() {
		@Override
		public IStatus download(URI toDownload, OutputStream target, IProgressMonitor pm) {
			byte[] contents = locationsToContents.get(toDownload);
			if (contents == null)
				Assert.fail("Attempt to download missing artifact in TestArtifactRepository: " + toDownload);
			try {
				target.write(contents);
			} catch (IOException e) {
				e.printStackTrace();
				Assert.fail("Unexpected exception in TestArtifactRepository" + e.getMessage());
			}
			return Status.OK_STATUS;
		}

		@Override
		public InputStream stream(URI toDownload, IProgressMonitor monitor) {
			throw new IllegalStateException("Method should not be called");
		}

		@Override
		public long getLastModified(URI toDownload, IProgressMonitor monitor) {
			throw new IllegalStateException("Method should not be called");
		}
	};

	public TestArtifactRepository(IProvisioningAgent agent, URI location) {
		super(agent, NAME, TYPE, VERSION, location, DESCRIPTION, PROVIDER, null);
	}

	public TestArtifactRepository(IProvisioningAgent agent) {
		super(agent, NAME, TYPE, VERSION, null, DESCRIPTION, PROVIDER, null);
	}

	public boolean addToRepositoryManager() {
		try {
			Method method = AbstractRepositoryManager.class.getDeclaredMethod("addRepository", IRepository.class, boolean.class, String.class);
			method.setAccessible(true);
			method.invoke(AbstractProvisioningTest.getArtifactRepositoryManager(), this, false, "");
			return true;
		} catch (Exception e) {
			return false;
		}

	}

	public void addArtifact(IArtifactKey key, byte[] contents) {
		URI keyLocation = locationFor(key);
		keysToLocations.put(key, keyLocation);
		locationsToContents.put(keyLocation, contents);
	}

	private URI locationFor(IArtifactKey key) {
		try {
			return new URI(SCHEME, key.toString(), null);
		} catch (URISyntaxException e) {
			Assert.fail("Invalid URI in TestArtifactRepository: " + e.getMessage());
			return null;
		}
	}

	public URI getArtifact(IArtifactKey key) {
		return keysToLocations.get(key);
	}

	private IStatus getArtifact(ArtifactRequest request, IProgressMonitor monitor) {
		request.perform(this, monitor);
		return request.getResult();
	}

	@Override
	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		try {
			MultiStatus overallStatus = new MultiStatus(TestActivator.PI_PROV_TESTS, IStatus.OK, null, null);
			for (IArtifactRequest request : requests) {
				overallStatus.add(getArtifact((ArtifactRequest) request, subMonitor.newChild(1)));
			}
			return (monitor.isCanceled() ? Status.CANCEL_STATUS : overallStatus);
		} finally {
			subMonitor.done();
		}
	}

	public void initialize(URI repoURL, InputStream descriptorFile) {
		setLocation(repoURL);
	}

	@Override
	public boolean contains(IArtifactDescriptor descriptor) {
		return keysToLocations.get(descriptor.getArtifactKey()) != null;
	}

	@Override
	public boolean contains(IArtifactKey key) {
		return keysToLocations.get(key) != null;
	}

	@Override
	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ProcessingStepHandler handler = new ProcessingStepHandler();
		destination = handler.createAndLink(getProvisioningAgent(), descriptor.getProcessingSteps(), null, destination, monitor);
		testhandler.download(keysToLocations.get(descriptor.getArtifactKey()), destination, monitor);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return testhandler.download(keysToLocations.get(descriptor.getArtifactKey()), destination, monitor);
	}

	@Override
	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		if (!contains(key))
			return null;
		return new IArtifactDescriptor[] {new ArtifactDescriptor(key)};
	}

	@Override
	public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		((ArtifactDescriptor) descriptor).setRepository(this);
		artifactDescriptors.add(descriptor);
		keysToLocations.put(descriptor.getArtifactKey(), null);
	}

	@Override
	public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		removeDescriptor(descriptor.getArtifactKey(), monitor);
	}

	@Override
	public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		for (IArtifactDescriptor descriptor : descriptors)
			removeDescriptor(descriptor, monitor);
	}

	@Override
	public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
		for (IArtifactKey key : keys)
			removeDescriptor(key, monitor);
	}

	@Override
	public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
		for (IArtifactDescriptor nextDescriptor : artifactDescriptors) {
			if (key.equals(nextDescriptor.getArtifactKey()))
				artifactDescriptors.remove(nextDescriptor);
		}
		if (keysToLocations.containsKey(key)) {
			URI theLocation = keysToLocations.get(key);
			locationsToContents.remove(theLocation);
			keysToLocations.remove(key);
		}
	}

	@Override
	public void removeAll(IProgressMonitor monitor) {
		artifactDescriptors.clear();
		keysToLocations.clear();
		locationsToContents.clear();
	}

	@Override
	public boolean isModifiable() {
		return true;
	}

	@Override
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException("Method is not implemented by this repository");
	}

	@Override
	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}
}
