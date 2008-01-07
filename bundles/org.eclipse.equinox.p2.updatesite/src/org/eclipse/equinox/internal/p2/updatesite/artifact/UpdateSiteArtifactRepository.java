/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite.artifact;

import java.io.File;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.updatesite.Activator;
import org.eclipse.equinox.spi.p2.core.repository.AbstractRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class UpdateSiteArtifactRepository extends AbstractRepository implements IArtifactRepository {

	private final IArtifactRepository artifactRepository;

	public UpdateSiteArtifactRepository(URL location) {
		super("update site: " + location.toExternalForm(), null, null, location, null, null);
		BundleContext context = Activator.getBundleContext();

		URL localRepositoryURL = null;
		try {
			String stateDirName = Integer.toString(location.toExternalForm().hashCode());
			File bundleData = context.getDataFile(null);
			File stateDir = new File(bundleData, stateDirName);
			localRepositoryURL = stateDir.toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		artifactRepository = initializeArtifactRepository(context, localRepositoryURL, "update site implementation - " + location.toExternalForm());

	}

	private IArtifactRepository initializeArtifactRepository(BundleContext context, URL stateDirURL, String repositoryName) {
		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
		if (reference == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered.");

		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered.");

		IArtifactRepository repository = null;
		try {
			repository = manager.loadRepository(stateDirURL, null);
			if (repository == null) {
				repository = manager.createRepository(stateDirURL, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository");
				repository.setProperty(IRepository.IMPLEMENTATION_ONLY_KEY, Boolean.TRUE.toString());
			}
		} finally {
			context.ungetService(reference);
		}

		if (repository == null)
			throw new IllegalStateException("Couldn't create artifact repository for: " + repositoryName);

		return repository;
	}

	public Map getProperties() {
		Map result = new HashMap(artifactRepository.getProperties());
		result.remove(IRepository.IMPLEMENTATION_ONLY_KEY);

		return result;
	}

	public String setProperty(String key, String value) {
		return artifactRepository.setProperty(key, value);
	}

	public void addDescriptor(IArtifactDescriptor descriptor) {
		artifactRepository.addDescriptor(descriptor);
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		return artifactRepository.contains(descriptor);
	}

	public boolean contains(IArtifactKey key) {
		return artifactRepository.contains(key);
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		return artifactRepository.getArtifact(descriptor, destination, monitor);
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		return artifactRepository.getArtifactDescriptors(key);
	}

	public IArtifactKey[] getArtifactKeys() {
		return artifactRepository.getArtifactKeys();
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		return artifactRepository.getArtifacts(requests, monitor);
	}

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
		return artifactRepository.getOutputStream(descriptor);
	}

	public void removeAll() {
		artifactRepository.removeAll();
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		artifactRepository.removeDescriptor(descriptor);
	}

	public void removeDescriptor(IArtifactKey key) {
		artifactRepository.removeDescriptor(key);
	}
}
