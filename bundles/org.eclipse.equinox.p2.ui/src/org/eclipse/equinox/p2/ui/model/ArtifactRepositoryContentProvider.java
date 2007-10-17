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

package org.eclipse.equinox.p2.ui.model;

import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.jface.viewers.*;

/**
 * Content provider for artifact repository viewers.
 * The repositories themselves are the elements.  The
 * artifact keys are the children of the artifact
 * repositories.
 * 
 * @since 3.4
 */
public class ArtifactRepositoryContentProvider implements IStructuredContentProvider, ITreeContentProvider {

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		// input does not affect the content
	}

	public void dispose() {
		// nothing to do
	}

	public Object[] getElements(Object input) {
		if (input == null) {
			return getChildren(new AllArtifactRepositories());
		}
		return getChildren(input);
	}

	public Object getParent(Object child) {
		return null;
	}

	public Object[] getChildren(Object parent) {
		if (parent instanceof AllArtifactRepositories) {
			return ((AllArtifactRepositories) parent).getChildren(parent);
		}
		if (parent instanceof IArtifactRepository) {
			IArtifactKey[] keys = ((IArtifactRepository) parent).getArtifactKeys();
			ArtifactElement[] elements = new ArtifactElement[keys.length];
			for (int i = 0; i < keys.length; i++) {
				elements[i] = new ArtifactElement(keys[i], (IArtifactRepository) parent);
			}
			return elements;
		}
		if (parent instanceof ArtifactElement) {
			ArtifactElement element = (ArtifactElement) parent;
			return element.getArtifactRepository().getArtifactDescriptors(element.getArtifactKey());
		}
		if (parent instanceof IArtifactDescriptor) {
			return ((IArtifactDescriptor) parent).getProcessingSteps();
		}
		return new Object[0];
	}

	public boolean hasChildren(Object parent) {
		return getChildren(parent).length > 0;
	}
}