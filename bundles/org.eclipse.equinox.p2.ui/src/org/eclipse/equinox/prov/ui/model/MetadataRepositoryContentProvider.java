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

package org.eclipse.equinox.prov.ui.model;

import org.eclipse.equinox.prov.metadata.repository.IMetadataRepository;
import org.eclipse.jface.viewers.*;

/**
 * Content provider for metadata repositories. The raw repositories are the
 * elements, and the raw IU's are the children of the repositories.
 * 
 * @since 3.4
 * 
 */
public class MetadataRepositoryContentProvider implements IStructuredContentProvider, ITreeContentProvider {

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		// input does not affect the content
	}

	public void dispose() {
		// nothing to do
	}

	public Object[] getElements(Object input) {
		if (input == null)
			return getChildren(new AllMetadataRepositories());
		return getChildren(input);
	}

	public Object getParent(Object child) {
		if (child instanceof ProvElement) {
			return ((ProvElement) child).getParent(child);
		}
		return null;
	}

	public Object[] getChildren(Object parent) {
		if (parent instanceof AllMetadataRepositories) {
			return ((AllMetadataRepositories) parent).getChildren(parent);
		}
		if (parent instanceof IMetadataRepository) {
			return ((IMetadataRepository) parent).getInstallableUnits(null);
		}
		return new Object[0];
	}

	public boolean hasChildren(Object parent) {
		if (parent instanceof AllMetadataRepositories) {
			return ((AllMetadataRepositories) parent).hasChildren(parent);
		}
		if (parent instanceof IMetadataRepository) {
			return ((IMetadataRepository) parent).getInstallableUnits(null).length > 0;
		}
		return false;
	}
}