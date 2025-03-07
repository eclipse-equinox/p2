/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.io.OutputStream;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;

public class AggregatedBundleRepository extends AbstractArtifactRepository implements IFileArtifactRepository {

	private static final String REPOSITORY_TYPE = AggregatedBundleRepository.class.getName();
	private final Collection<IFileArtifactRepository> bundleRepositories;

	public AggregatedBundleRepository(IProvisioningAgent agent,
			Collection<IFileArtifactRepository> bundleRepositories) {
		super(agent, REPOSITORY_TYPE, REPOSITORY_TYPE, "1.0", null, null, null, null); //$NON-NLS-1$
		this.bundleRepositories = bundleRepositories;
	}

	@Override
	public File getArtifactFile(IArtifactKey key) {
		for (IFileArtifactRepository repository : bundleRepositories) {
			File artifactFile = repository.getArtifactFile(key);
			if (artifactFile != null) {
				return artifactFile;
			}
		}
		return null;
	}

	@Override
	public File getArtifactFile(IArtifactDescriptor descriptor) {
		for (IFileArtifactRepository repository : bundleRepositories) {
			File artifactFile = repository.getArtifactFile(descriptor);
			if (artifactFile != null) {
				return artifactFile;
			}
		}
		return null;
	}

	@Override
	public boolean contains(IArtifactDescriptor descriptor) {
		for (IFileArtifactRepository repository : bundleRepositories) {
			if (repository.contains(descriptor)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean contains(IArtifactKey key) {
		for (IFileArtifactRepository repository : bundleRepositories) {
			if (repository.contains(key)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
		Set<IArtifactDescriptor> artifactDescriptors = new HashSet<>();
		for (IFileArtifactRepository repository : bundleRepositories) {
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
			if (descriptors != null) {
				artifactDescriptors.addAll(Arrays.asList(descriptors));
			}
		}
		return artifactDescriptors.toArray(new IArtifactDescriptor[artifactDescriptors.size()]);
	}

	@Override
	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.artifact_retrieval_unsupported);
	}

	@Override
	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.artifact_retrieval_unsupported);
	}

	@Override
	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor) {
		throw new UnsupportedOperationException(Messages.artifact_retrieval_unsupported);
	}

	@Override
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
		throw new UnsupportedOperationException(Messages.artifact_write_unsupported);
	}

	/**
	 * Exposed for testing and debugging purposes.
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public Collection<IFileArtifactRepository> testGetBundleRepositories() {
		return bundleRepositories;
	}

	@Override
	public IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
		// Query all the all the repositories
		IQueryable<IArtifactKey> queryable = QueryUtil.compoundQueryable(bundleRepositories);
		return queryable.query(query, monitor);
	}

	@Override
	public IQueryable<IArtifactDescriptor> descriptorQueryable() {
		List<IQueryable<IArtifactDescriptor>> descQueryables = new ArrayList<>(bundleRepositories.size());
		for (IFileArtifactRepository repository : bundleRepositories) {
			descQueryables.add(repository.descriptorQueryable());
		}

		return QueryUtil.compoundQueryable(descQueryables);
	}
}
