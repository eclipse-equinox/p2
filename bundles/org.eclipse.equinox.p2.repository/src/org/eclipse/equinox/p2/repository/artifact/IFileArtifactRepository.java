/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact;

import java.io.File;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * An artifact repository whose artifacts are available in the local file system.
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface IFileArtifactRepository extends IArtifactRepository {
	/**
	 * Return the location of the full local file corresponding to the given 
	 * artifact key to the given key, or <code>null</code> if not available.
	 * 
	 * @param key the artifact key for the file to be returned
	 * @return the location of the requested artifact or<code>null</code> if not available
	 */
	public File getArtifactFile(IArtifactKey key);

	/**
	 * Return the location of the local file corresponding to the given 
	 * artifact descriptor, or <code>null</code> if not available.
	 * 
	 * @param descriptor the artifact descriptor for the file to be returned
	 * @return the location of the requested descriptor or<code>null</code> if not available
	 */
	public File getArtifactFile(IArtifactDescriptor descriptor);
}
