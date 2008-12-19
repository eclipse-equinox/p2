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
package org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository;

import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.spi.p2.core.repository.AbstractRepository;

public abstract class AbstractArtifactRepository extends AbstractRepository implements IArtifactRepository {

	protected AbstractArtifactRepository(String name, String type, String version, URI location, String description, String provider, Map properties) {
		super(name, type, version, location, description, provider, properties);
	}

	public abstract boolean contains(IArtifactDescriptor descriptor);

	public abstract boolean contains(IArtifactKey key);

	public abstract IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor);

	public abstract IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);

	public abstract IArtifactKey[] getArtifactKeys();

	public abstract IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor);

	public abstract OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException;

	public void addDescriptor(IArtifactDescriptor descriptor) {
		assertModifiable();
	}

	public void addDescriptors(IArtifactDescriptor[] descriptors) {
		assertModifiable();
	}

	public void removeDescriptor(IArtifactDescriptor descriptor) {
		assertModifiable();
	}

	public void removeDescriptor(IArtifactKey key) {
		assertModifiable();
	}

	public void removeAll() {
		assertModifiable();
	}

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

	public int hashCode() {
		return (this.getLocation().toString().hashCode()) * 87;
	}

}
