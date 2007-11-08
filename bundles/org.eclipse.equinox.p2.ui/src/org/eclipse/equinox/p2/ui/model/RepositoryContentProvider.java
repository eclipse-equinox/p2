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

import org.eclipse.jface.viewers.*;
import org.eclipse.ui.progress.DeferredTreeContentManager;

/**
 * Content provider for provisioning repositories. The repositories are the
 * elements and the repository children are retrieved asynchronously
 * using the IDeferredWorkbenchAdapter mechanism.
 * 
 * @since 3.4
 * 
 */
public abstract class RepositoryContentProvider implements IStructuredContentProvider, ITreeContentProvider {

	DeferredTreeContentManager manager;

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		if (v instanceof AbstractTreeViewer) {
			manager = new DeferredTreeContentManager((AbstractTreeViewer) v);
		}
	}

	public void dispose() {
		// Nothing to do
	}

	public Object getParent(Object child) {
		if (child instanceof ProvElement) {
			return ((ProvElement) child).getParent(child);
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof ProvElement)
			return ((ProvElement) element).hasChildren(element);
		return false;
	}

	public Object[] getChildren(final Object element) {
		if (manager != null) {
			Object[] children = manager.getChildren(element);
			if (children != null) {
				// This will be a placeholder to indicate 
				// that the real children are being fetched
				return children;
			}
		}
		// We don't have a deferred content manager or else it could
		// not retrieve deferred content.
		if (element instanceof ProvElement) {
			return ((ProvElement) element).getChildren(null);
		}
		return null;
	}
}