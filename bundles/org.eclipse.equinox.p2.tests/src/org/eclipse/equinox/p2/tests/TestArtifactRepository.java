/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRequest;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.core.helpers.*;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;
import org.eclipse.equinox.p2.core.repository.IRepositoryInfo;
import org.eclipse.equinox.p2.core.repository.RepositoryCreationException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * A simple artifact repository implementation used for testing purposes.
 * All artifacts are kept in memory.
 */
public class TestArtifactRepository extends Assert implements IArtifactRepository {
	private static final String SCHEME = "testartifactrepo";
	/**
	 * Map of IArtifactKey -> String (location)
	 */
	Map keysToLocations = new HashMap();
	/**
	 * Map of String (location) -> byte[] (contents)
	 */
	Map locationsToContents = new HashMap();

	Transport testhandler = new Transport() {
		public IStatus download(String toDownload, OutputStream target, IProgressMonitor pm) {
			byte[] contents = (byte[]) locationsToContents.get(toDownload);
			if (contents == null)
				fail("Attempt to download missing artifact in TestArtifactRepository: " + toDownload);
			try {
				target.write(contents);
			} catch (IOException e) {
				e.printStackTrace();
				fail("Unexpected exception in TestArtifactRepository" + e.getMessage());
			}
			return Status.OK_STATUS;
		}
	};
	private URL url;

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
			fail("Invalid URI in TestArtifactRepository: " + e.getMessage());
			return null;
		}
	}

	public IArtifactKey[] getArtifactKeys() {
		return (IArtifactKey[]) keysToLocations.keySet().toArray(new IArtifactKey[0]);
	}

	private IStatus getArtifact(ArtifactRequest request, IProgressMonitor monitor) {
		request.setSourceRepository(this);
		request.perform(monitor);
		return request.getResult();
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, requests.length);
		try {
			MultiStatus overallStatus = new MultiStatus();
			for (int i = 0; i < requests.length; i++) {
				overallStatus.add(getArtifact((ArtifactRequest) requests[i], subMonitor.newChild(1)));
			}
			return (monitor.isCanceled() ? Status.CANCEL_STATUS : overallStatus);
		} finally {
			subMonitor.done();
		}
	}

	public String getType() {
		return SCHEME;
	}

	public URL getLocation() {
		return url;
	}

	public void initialize(URL repoURL, InputStream descriptorFile) throws RepositoryCreationException {
		this.url = repoURL;
	}

	public String getDescription() {
		return "A Test Artifact Repository"; //$NON-NLS-1$
	}

	public String getName() {
		return "ATestArtifactRepository"; //$NON-NLS-1$
	}

	public String getProvider() {
		return "org.eclipse"; //$NON-NLS-1$
	}

	public String getVersion() {
		return "1"; //$NON-NLS-1$
	}

	public UnmodifiableProperties getProperties() {
		return new UnmodifiableProperties(new OrderedProperties());
	}

	public Object getAdapter(Class adapter) {
		if (adapter == TestArtifactRepository.class || adapter == IArtifactRepository.class || adapter == IRepositoryInfo.class) {
			return this;
		}
		return null;
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
}
