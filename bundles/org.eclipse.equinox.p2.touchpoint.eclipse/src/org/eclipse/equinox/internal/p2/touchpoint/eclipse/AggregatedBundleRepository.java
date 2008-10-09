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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.io.OutputStream;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.AbstractArtifactRepository;

public class AggregatedBundleRepository extends AbstractArtifactRepository implements IFileArtifactRepository {

	private static final String REPOSITORY_TYPE = AggregatedBundleRepository.class.getName();
	private final Collection bundleRepositories;

	public AggregatedBundleRepository(Collection bundleRepositories) {
		super(REPOSITORY_TYPE, REPOSITORY_TYPE, "1.0", null, null, null, null); //$NON-NLS-1$
		this.bundleRepositories = bundleRepositories;
	}

	public File getArtifactFile(IArtifactKey key) {
		for (Iterator it = bundleRepositories.iterator(); it.hasNext();) {
			IFileArtifactRepository repository = (IFileArtifactRepository) it.next();
			File artifactFile = repository.getArtifactFile(key);
			if (artifactFile != null)
				return artifactFile;
		}
		return null;
	}

	public File getArtifactFile(IArtifactDescriptor descriptor) {
		for (Iterator it = bundleRepositories.iterator(); it.hasNext();) {
			IFileArtifactRepository repository = (IFileArtifactRepository) it.next();
			File artifactFile = repository.getArtifactFile(descriptor);
			if (artifactFile != null)
				return artifactFile;
		}
		return null;
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		for (Iterator it = bundleRepositories.iterator(); it.hasNext();) {
			IFileArtifactRepository repository = (IFileArtifactRepository) it.next();
			if (repository.contains(descriptor))
				return true;
		}
		return false;
	}

	public boolean contains(IArtifactKey key) {
		for (Iterator it = bundleRepositories.iterator(); it.hasNext();) {
			IFileArtifactRepository repository = (IFileArtifactRepository) it.next();
			if (repository.contains(key))
				return true;
		}
		return false;
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		Set artifactDescriptors = new HashSet();
		for (Iterator it = bundleRepositories.iterator(); it.hasNext();) {
			IFileArtifactRepository repository = (IFileArtifactRepository) it.next();
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
			if (descriptors != null)
				artifactDescriptors.addAll(Arrays.asList(descriptors));
		}
		return (IArtifactDescriptor[]) artifactDescriptors.toArray(new IArtifactDescriptor[artifactDescriptors.size()]);
	}

	public IArtifactKey[] getArtifactKeys() {
		Set artifactKeys = new HashSet();
		for (Iterator it = bundleRepositories.iterator(); it.hasNext();) {
			IFileArtifactRepository repository = (IFileArtifactRepository) it.next();
			IArtifactKey[] keys = repository.getArtifactKeys();
			if (keys != null)
				artifactKeys.addAll(Arrays.asList(keys));
		}
		return (IArtifactKey[]) artifactKeys.toArray(new IArtifactKey[artifactKeys.size()]);
	}

	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.artifact_retrieval_unsupported);
	}

	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.artifact_retrieval_unsupported);
	}

	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.artifact_retrieval_unsupported);
	}
}
