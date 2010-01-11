/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;

import java.io.File;
import java.io.OutputStream;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.CompoundQueryable;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.*;

public class AggregatedBundleRepository extends AbstractArtifactRepository implements IFileArtifactRepository {

	private static final String REPOSITORY_TYPE = AggregatedBundleRepository.class.getName();
	private final Collection<IFileArtifactRepository> bundleRepositories;

	public AggregatedBundleRepository(Collection<IFileArtifactRepository> bundleRepositories) {
		super(REPOSITORY_TYPE, REPOSITORY_TYPE, "1.0", null, null, null, null); //$NON-NLS-1$
		this.bundleRepositories = bundleRepositories;
	}

	public File getArtifactFile(IArtifactKey key) {
		for (IFileArtifactRepository repository : bundleRepositories) {
			File artifactFile = repository.getArtifactFile(key);
			if (artifactFile != null)
				return artifactFile;
		}
		return null;
	}

	public File getArtifactFile(IArtifactDescriptor descriptor) {
		for (IFileArtifactRepository repository : bundleRepositories) {
			File artifactFile = repository.getArtifactFile(descriptor);
			if (artifactFile != null)
				return artifactFile;
		}
		return null;
	}

	public boolean contains(IArtifactDescriptor descriptor) {
		for (IFileArtifactRepository repository : bundleRepositories) {
			if (repository.contains(descriptor))
				return true;
		}
		return false;
	}

	public boolean contains(IArtifactKey key) {
		for (IFileArtifactRepository repository : bundleRepositories) {
			if (repository.contains(key))
				return true;
		}
		return false;
	}

	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		Set<IArtifactDescriptor> artifactDescriptors = new HashSet<IArtifactDescriptor>();
		for (IFileArtifactRepository repository : bundleRepositories) {
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
			if (descriptors != null)
				artifactDescriptors.addAll(Arrays.asList(descriptors));
		}
		return artifactDescriptors.toArray(new IArtifactDescriptor[artifactDescriptors.size()]);
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

	public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException(Messages.artifact_write_unsupported);
	}

	/**
	 * Exposed for testing and debugging purposes.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public Collection<IFileArtifactRepository> testGetBundleRepositories() {
		return bundleRepositories;
	}

	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		// Query all the all the repositories
		CompoundQueryable<IArtifactKey> queryable = new CompoundQueryable<IArtifactKey>(bundleRepositories);
		return queryable.query(query, monitor);
	}

	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		List<IQueryable<IArtifactDescriptor>> descQueryables = new ArrayList<IQueryable<IArtifactDescriptor>>(bundleRepositories.size());
		for (IFileArtifactRepository repository : bundleRepositories)
			descQueryables.add(repository.descriptorQueryable());

		return new CompoundQueryable<IArtifactDescriptor>(descQueryables);
	}
}
