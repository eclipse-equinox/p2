/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.artifact.repository;

import java.io.OutputStream;
import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.repository.IRepositoryInfo;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * TODO: Convert to abstract class
 */
public interface IArtifactRepository extends IRepositoryInfo {
	/** 
	 * Returns true if this repository contains the given descriptor.
	 * @param descriptor the descriptor to query
	 * @return true if the given descriptor is already in this repository
	 */
	public boolean contains(IArtifactDescriptor descriptor);

	/** 
	 * Returns true if this repository contains the given artifact key.
	 * @param key the key to query
	 * @return true if the given key is already in this repository
	 */
	public boolean contains(IArtifactKey key);

	/**
	 * Executes the given artifact requests on this byte server.
	 * @param requests The artifact requests
	 * @param monitor
	 * @return a status object that is <code>OK</code> if requests were
	 * processed successfully. Otherwise, a status indicating information,
	 * warnings, or errors that occurred while executing the artifact requests
	 */
	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor);

	/**
	 * Return the set of artifact descriptors describing the ways that this repository
	 * can supply the artifact associated with the given artifact key
	 * @param key the artifact key to lookup
	 * @return the descriptors associated with the given key
	 */
	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);

	/**
	 * Return a stream containing the described artifact, or null if not available 
	 */
	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor);

	/**
	 * Returns the list of artifact keys managed by this repository
	 * @return list of artifact keys
	 */
	public IArtifactKey[] getArtifactKeys();

	// TODO move this to a local repo interface and change to return a file
	/**
	 * Return a URI to the given key, or null if not available 
	 */
	public URI getArtifact(IArtifactKey key);

}
