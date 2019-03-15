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
package org.eclipse.equinox.p2.repository.artifact.spi;

import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.spi.AbstractRepository;

/**
 * The common base class for all artifact repository implementations. Clients must
 * subclass this class to create their own repository implementations.
 * <p>
 * This base class provides default implementations of all methods that modify the repository.
 * These default methods throw an exception if {@link #isModifiable()} returns <code>false</code>.
 * Therefore a client can implement a read-only repository by overriding only the abstract methods.
 * @since 2.0
 */
public abstract class AbstractArtifactRepository extends AbstractRepository<IArtifactKey> implements IArtifactRepository {

	protected AbstractArtifactRepository(IProvisioningAgent agent, String name, String type, String version, URI location, String description, String provider, Map<String, String> properties) {
		super(agent, name, type, version, location, description, provider, properties);
	}

	@Override
	public abstract boolean contains(IArtifactDescriptor descriptor);

	@Override
	public abstract boolean contains(IArtifactKey key);

	@Override
	public abstract IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor);

	@Override
	public abstract IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);

	@Override
	public abstract IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor);

	@Override
	public abstract OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException;

	/**
	 * @since 2.1
	 */
	@Override
	public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		assertModifiable();
	}

	/**
	 * @deprecated Use {@link #addDescriptor(IArtifactDescriptor, IProgressMonitor)} instead.
	 */
	@Override
	@Deprecated
	public void addDescriptor(IArtifactDescriptor descriptor) {
		this.addDescriptor(descriptor, new NullProgressMonitor());
	}

	/**
	 * {@inheritDoc}
	 * @since 2.1
	 */
	@Override
	public void addDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		assertModifiable();
	}

	/**
	 * @deprecated Use {@link #addDescriptors(IArtifactDescriptor[], IProgressMonitor)} instead.
	 */
	@Override
	@Deprecated
	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		this.addDescriptors(descriptors, new NullProgressMonitor());
	}

	/**
	 * {@inheritDoc}
	 * @since 2.1
	 */
	@Override
	public void removeDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
		assertModifiable();
	}

	/**
	 * @deprecated Use {@link #removeDescriptor(IArtifactDescriptor, IProgressMonitor)} instead.
	 */
	@Override
	@Deprecated
	public void removeDescriptor(IArtifactDescriptor descriptor) {
		this.removeDescriptor(descriptor, new NullProgressMonitor());
	}

	/**
	 * {@inheritDoc}
	 * @since 2.1
	 */
	@Override
	public void removeDescriptors(IArtifactDescriptor[] descriptors, IProgressMonitor monitor) {
		assertModifiable();
	}

	/**
	 * {@inheritDoc}
	 * @since 2.1
	 * @deprecated Use {@link #removeDescriptors(IArtifactDescriptor[], IProgressMonitor)} instead.
	 */
	@Override
	@Deprecated
	public void removeDescriptors(IArtifactDescriptor[] descriptors) {
		this.removeDescriptors(descriptors, new NullProgressMonitor());
	}

	/**
	 * {@inheritDoc}
	 * @since 2.1
	 */
	@Override
	public void removeDescriptor(IArtifactKey key, IProgressMonitor monitor) {
		assertModifiable();
	}

	/**
	 * @deprecated Use {@link #removeDescriptor(IArtifactKey, IProgressMonitor)} instead.
	 */
	@Override
	@Deprecated
	public void removeDescriptor(IArtifactKey key) {
		this.removeDescriptor(key, new NullProgressMonitor());
	}

	/**
	 * {@inheritDoc}
	 * @since 2.1
	 */
	@Override
	public void removeDescriptors(IArtifactKey[] keys, IProgressMonitor monitor) {
		assertModifiable();
	}

	/**
	 * {@inheritDoc}
	 * @since 2.1
	 * @deprecated Use {@link #removeDescriptors(IArtifactKey[], IProgressMonitor)} instead.
	 */
	@Override
	@Deprecated
	public void removeDescriptors(IArtifactKey[] keys) {
		this.removeDescriptors(keys, new NullProgressMonitor());
	}

	/**
	 * @since 2.1
	 */
	@Override
	public void removeAll(IProgressMonitor monitor) {
		assertModifiable();
	}

	/**
	 * @deprecated Use {@link #removeAll(IProgressMonitor)} instead.
	 */
	@Override
	@Deprecated
	public void removeAll() {
		this.removeAll(new NullProgressMonitor());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AbstractArtifactRepository)) {
			return false;
		}
		if (URIUtil.sameURI(getLocation(), ((AbstractArtifactRepository) o).getLocation()))
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		return (this.getLocation().toString().hashCode()) * 87;
	}

	@Override
	public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key) {
		return new ArtifactDescriptor(key);
	}

	@Override
	public IArtifactKey createArtifactKey(String classifier, String id, Version version) {
		return new ArtifactKey(classifier, id, version);
	}

	@Override
	public IStatus executeBatch(IRunnableWithProgress runnable, IProgressMonitor monitor) {
		try {
			runnable.run(monitor);
		} catch (OperationCanceledException oce) {
			return new Status(IStatus.CANCEL, Activator.ID, oce.getMessage(), oce);
		} catch (Exception e) {
			return new Status(IStatus.ERROR, Activator.ID, e.getMessage(), e);
		}
		return Status.OK_STATUS;
	}
}
