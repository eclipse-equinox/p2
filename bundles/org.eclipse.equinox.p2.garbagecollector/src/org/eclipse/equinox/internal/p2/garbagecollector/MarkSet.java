/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.garbagecollector;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * Wrapper class used to store an IArtifactRepository and its root set of IArtifactKeys.
 */
public class MarkSet {

	/**
	 * The root set for repo.  This is the set of keys that currently map to an artifact in repo.
	 */
	private IArtifactKey[] keys;

	/**
	 * The ArtifactRepository for which a root set is being stored.
	 */
	private IArtifactRepository repo;

	public MarkSet(IArtifactKey[] inKeys, IArtifactRepository inRepo) {
		Assert.isNotNull(inKeys);
		Assert.isNotNull(inRepo);
		keys = inKeys;
		repo = inRepo;
	}

	public IArtifactKey[] getKeys() {
		return keys;
	}

	public IArtifactRepository getRepo() {
		return repo;
	}

}
