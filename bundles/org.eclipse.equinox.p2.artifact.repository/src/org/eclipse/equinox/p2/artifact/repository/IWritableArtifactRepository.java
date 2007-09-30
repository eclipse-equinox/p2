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
import org.eclipse.equinox.p2.metadata.IArtifactKey;

// TODO consider having the add and remove methods take several keys/descriptors 
// to operate on at one time.

public interface IWritableArtifactRepository extends IArtifactRepository {

	/**
	 * Open an output stream to which a client can write the data for the given 
	 * artifact descriptor.
	 * @param descriptor the descriptor describing the artifact data to be written to the 
	 * resultant stream
	 * @return the stream to which the artifact content can be written
	 */
	public OutputStream getOutputStream(IArtifactDescriptor descriptor);

	/**
	 * Add the given descriptor to the set of descriptors in this repository.  This is 
	 * a relatively low-level operation that should be used only when the actual related 
	 * content is in this repository and the given descriptor accurately describes 
	 * that content.
	 * @param descriptor the descriptor to add.
	 */
	public void addDescriptor(IArtifactDescriptor descriptor);

	/**
	 * Remove the given descriptor from the set of descriptors in this repository.  
	 * @param descriptor the descriptor to remove.
	 */
	public void removeDescriptor(IArtifactDescriptor descriptor);

	/**
	 * Remove the given key and all related descriptors from this repository.  
	 * @param key the key to remove.
	 */
	public void removeDescriptor(IArtifactKey key);

	/**
	 * Remove the all key and descriptor information from this repository.  
	 */
	public void removeAll();

}
