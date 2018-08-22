/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *     Red Hat Inc. - Fix compiler problems from generified IAdaptable#getAdapter
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

/**
 * Element wrapper class for an artifact key and its repository
 * 
 * @since 3.4
 */
public class ArtifactElement extends ProvElement {

	IArtifactKey key;
	IArtifactRepository repo;

	public ArtifactElement(Object parent, IArtifactKey key, IArtifactRepository repo) {
		super(parent);
		this.key = key;
		this.repo = repo;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	@Override
	protected String getImageId(Object obj) {
		return null;
	}

	@Override
	public String getLabel(Object o) {
		return key.getId() + " [" + key.getClassifier() + "]"; //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public Object[] getChildren(Object o) {
		return repo.getArtifactDescriptors(key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IArtifactRepository.class)
			return (T) getArtifactRepository();
		if (adapter == IArtifactKey.class)
			return (T) getArtifactKey();
		return super.getAdapter(adapter);
	}

	public IArtifactKey getArtifactKey() {
		return key;
	}

	public IArtifactRepository getArtifactRepository() {
		return repo;
	}
}
