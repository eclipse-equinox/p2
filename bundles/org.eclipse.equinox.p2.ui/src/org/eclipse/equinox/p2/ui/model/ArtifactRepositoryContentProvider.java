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

/**
 * Content provider for artifact repository viewers.
 * The repositories themselves are the elements.  The
 * artifact keys are the children of the artifact
 * repositories.
 * 
 * @since 3.4
 */
public class ArtifactRepositoryContentProvider extends RepositoryContentProvider {

	public Object[] getElements(Object input) {
		if (input == null) {
			return getChildren(new AllArtifactRepositories());
		}
		return getChildren(input);
	}

	public Object[] getChildren(final Object parent) {
		Object[] children = super.getChildren(parent);
		if (children != null)
			return children;
		if (parent instanceof ArtifactElement) {
			ArtifactElement element = (ArtifactElement) parent;
			return element.getArtifactRepository().getArtifactDescriptors(element.getArtifactKey());
		}
		if (parent instanceof IArtifactDescriptor) {
			return ((IArtifactDescriptor) parent).getProcessingSteps();
		}
		return null;
	}
}