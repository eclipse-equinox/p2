/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.*;
import java.net.*;
import java.util.*;
import junit.framework.Assert;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.Transport;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.AbstractArtifactRepository;

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
	 * Map of IArtifactKey -> String (location)
	 */
	Map keysToLocations = new HashMap();
	/**
	 * Map of String (location) -> byte[] (contents)
	 */
	Map locationsToContents = new HashMap();

	/**
	 * 	Set of artifact descriptors
	 */
	Set artifactDescriptors = new HashSet();

	Transport testhandler = new Transport() {
		public IStatus download(String toDownload, OutputStream target, IProgressMonitor pm) {
			byte[] contents = (byte[]) locationsToContents.get(toDownload);
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
	};

	public TestArtifactRepository() {
		super(NAME, TYPE, VERSION, null, DESCRIPTION, PROVIDER);
	}

	public void addArtifact(IArtifactKey key, byte[] contents) {
		String location = key.toString();
		keysToLocations.put(key, location);
		locationsToContents.put(location, contents);
	}

	public URI getArtifact(IArtifactKey key) {
		String location = (String) keysToLocations.get(key);
		if (location == null)
			return null;
		try {
			return new URI(SCHEME, location, null);
		} catch (URISyntaxException e) {
			Assert.fail("Invalid URI in TestArtifactRepository: " + e.getMessage());
			return null;
		}
	}

	public IArtifactKey[] getArtifactKeys() {
		return (IArtifactKey[]) keysToLocations.keySet().toArray(new IArtifactKey[keysToLocations.keySet().size()]);
	}

	private IStatus getArtifact(ArtifactRequest request, IProgressMonitor monitor) {
		request.setSourceRepository(this);
		request.perform(monitor);
		return request.getResult();
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		try {
			MultiStatus overallStatus = new MultiStatus(TestActivator.PI_PROV_TESTS, IStatus.OK, null, null);
			for (int i = 0; i < requests.length; i++) {
				overallStatus.add(getArtifact((ArtifactRequest) requests[i], subMonitor.newChild(1)));
			}
			return (monitor.isCanceled() ? Status.CANCEL_STATUS : overallStatus);
		} finally {
			subMonitor.done();
		}
	}

	public void initialize(URL repoURL, InputStream descriptorFile) throws RepositoryCreationException {
		location = repoURL;
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		return keysToLocations.get(descriptor.getArtifactKey()) != null;
	}

	public boolean contains(IArtifactKey key) {
		return keysToLocations.get(key) != null;
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ProcessingStepHandler handler = new ProcessingStepHandler();
		destination = handler.createAndLink(descriptor.getProcessingSteps(), null, destination, monitor);
		testhandler.download((String) keysToLocations.get(descriptor.getArtifactKey()), destination, monitor);
		return Status.OK_STATUS;
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		if (!contains(key))
			return null;
		return new IArtifactDescriptor[] {new ArtifactDescriptor(key)};
	}

	public void addDescriptor(IArtifactDescriptor descriptor) {
		((ArtifactDescriptor) descriptor).setRepository(this);
		artifactDescriptors.add(descriptor);
		keysToLocations.put(descriptor.getArtifactKey(), null);
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		removeDescriptor(descriptor.getArtifactKey());
	}

	public void removeDescriptor(IArtifactKey key) {
		for (Iterator iter = artifactDescriptors.iterator(); iter.hasNext();) {
			IArtifactDescriptor nextDescriptor = (IArtifactDescriptor) iter.next();
			if (key.equals(nextDescriptor.getArtifactKey())) {
				artifactDescriptors.remove(nextDescriptor);
			}
		}
		if (keysToLocations.containsKey(key)) {
			String theLocation = (String) keysToLocations.get(key);
			locationsToContents.remove(theLocation);
			keysToLocations.remove(key);
		}
	}

	public void removeAll() {
		artifactDescriptors.clear();
		keysToLocations.clear();
		locationsToContents.clear();
	}

	public boolean isModifiable() {
		return true;
	}
}
