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

import org.eclipse.equinox.p2.metadata.IArtifactKey;

import java.io.File;
import java.io.OutputStream;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.AbstractArtifactRepository;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.repository.artifact.*;

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
	public Collection testGetBundleRepositories() {
		return bundleRepositories;
	}

	public Collector query(IQuery query, Collector collector, IProgressMonitor monitor) {
		// Query all the all the repositories
		CompoundQueryable queryable = new CompoundQueryable((IQueryable[]) bundleRepositories.toArray(new IQueryable[bundleRepositories.size()]));
		return queryable.query(query, collector, monitor);
	}
}
