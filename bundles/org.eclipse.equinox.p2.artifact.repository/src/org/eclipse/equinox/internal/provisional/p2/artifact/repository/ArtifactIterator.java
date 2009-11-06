/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.artifact.repository;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

/**
 * An iterator over an IArtifactRepository
 * Returns IArtifactKey's and IArtifactDescriptor's.  First a key is returned, 
 * then that key's descriptors, and again for the next key.
 */
public class ArtifactIterator implements Iterator {
	private final IArtifactRepository artifactRepository;
	private Iterator keys = null;
	private Iterator descriptors = null;
	private boolean returnKeys = false;
	private boolean returnDescriptors = false;

	/**
	 * @param artifactRepository - the artifact repository
	 * @param returnKeys - whether or not to include IArtifactKey's in the iteration
	 * @param returnDescriptors - whether or not to include IArtifactDescriptor's in the iteration
	 */
	public ArtifactIterator(IArtifactRepository artifactRepository, boolean returnKeys, boolean returnDescriptors) {
		this.artifactRepository = artifactRepository;
		this.returnDescriptors = returnDescriptors;
		this.returnKeys = returnKeys;

		//initialize the key interator
		keys = Arrays.asList(artifactRepository.getArtifactKeys()).iterator();
	}

	/**
	 * See {@link Iterator#hasNext()}
	 */
	public boolean hasNext() {
		if (!returnDescriptors && !returnKeys)
			return false; //perhaps this is an exception?

		if (!returnDescriptors)
			return keys.hasNext(); //keys only

		if (returnKeys) //keys and descriptors
			return (descriptors != null && descriptors.hasNext()) || keys.hasNext();

		//descriptors only
		if (descriptors != null && descriptors.hasNext())
			return true;
		while (keys.hasNext()) {
			IArtifactKey key = (IArtifactKey) keys.next();
			descriptors = Arrays.asList(artifactRepository.getArtifactDescriptors(key)).iterator();
			if (descriptors.hasNext())
				return true;
		}
		return false;
	}

	/**
	 * See {@link Iterator#next()}
	 */
	public Object next() {
		if (!returnDescriptors && !returnKeys)
			throw new NoSuchElementException();

		if (!returnDescriptors)
			return keys.next(); //keys only

		if (returnKeys) {
			//keys and descriptors
			if (descriptors != null && descriptors.hasNext())
				return descriptors.next();

			IArtifactKey nextKey = (IArtifactKey) keys.next();
			descriptors = Arrays.asList(artifactRepository.getArtifactDescriptors(nextKey)).iterator();
			return nextKey;
		}

		//descriptors only
		if (descriptors != null && descriptors.hasNext())
			return descriptors.next();
		while (keys.hasNext()) {
			IArtifactKey key = (IArtifactKey) keys.next();
			descriptors = Arrays.asList(artifactRepository.getArtifactDescriptors(key)).iterator();
			if (!descriptors.hasNext())
				continue;
			return descriptors.next();
		}
		throw new NoSuchElementException();
	}

	/**
	 * Returns true if the iteration contains more IArtifactKeys
	 * @return boolean
	 */
	public boolean hasNextKey() {
		if (!returnKeys)
			return false;
		return keys.hasNext();
	}

	/**
	 * Get the next IArtifactKey skipping over any IArtifactDescriptor's
	 * @return IArtifactKey
	 * @throws IllegalStateException if this iterator is not returning keys 
	 */
	public IArtifactKey nextKey() {
		if (!returnKeys)
			throw new IllegalStateException();
		IArtifactKey next = (IArtifactKey) keys.next();
		if (returnDescriptors)
			descriptors = Arrays.asList(artifactRepository.getArtifactDescriptors(next)).iterator();
		return next;
	}

	/**
	 * See {@link Iterator#remove()}
	 * This operation is not supported
	 * @throws UnsupportedOperationException
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}
}