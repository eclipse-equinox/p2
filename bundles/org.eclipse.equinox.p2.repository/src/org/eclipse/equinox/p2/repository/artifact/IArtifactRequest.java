/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * Represents a request to transfer an artifact from an artifact repository. When the
 * request is executed against a repository, it will be executed and the result
 * of the execution will become available.
 * 
 * @see IArtifactRepositoryManager#createMirrorRequest(IArtifactKey, IArtifactRepository, java.util.Map, java.util.Map)
 * @see IArtifactRepository#getArtifacts(IArtifactRequest[], IProgressMonitor)
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface IArtifactRequest {

	/**
	 * Returns the key for the artifact that is being requested
	 * 
	 * @return The requested artifact key
	 */
	public IArtifactKey getArtifactKey();

	/**
	 * Performs the artifact request, and sets the result status.
	 * 
	 * @param sourceRepository the repository to download the artifact from 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor);

	/**
	 * Returns the result of the executed artifact request, or <code>null</code> if
	 * the request has never been executed. Artifact requests are executed by invoking
	 * {@link IArtifactRepository#getArtifacts(IArtifactRequest[], IProgressMonitor)}.
	 * 
	 * @return The result of the previous perform call, or <code>null</code>
	 */
	public IStatus getResult();
}