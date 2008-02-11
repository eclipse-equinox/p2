/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.viewers;

import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.provisional.p2.ui.query.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.progress.DeferredTreeContentManager;

/**
 * Content provider that retrieves children asynchronously where
 * possible using the IDeferredWorkbenchAdapter and provisioning
 * query mechanisms.
 * 
 * @since 3.4
 * 
 */
public class DeferredQueryContentProvider implements ITreeContentProvider {

	DeferredTreeContentManager manager;
	IQueryProvider queryProvider;

	public DeferredQueryContentProvider(IQueryProvider queryProvider) {
		this.queryProvider = queryProvider;
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		if (v instanceof AbstractTreeViewer) {
			manager = new DeferredTreeContentManager((AbstractTreeViewer) v);
		}
	}

	public Object[] getElements(Object input) {
		if (input instanceof QueriedElement) {
			QueriedElement element = (QueriedElement) input;
			element.setQueryProvider(queryProvider);
			return element.getChildren(null);
		}
		return null;
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
		if (manager != null) {
			if (manager.isDeferredAdapter(element))
				return manager.mayHaveChildren(element);
		}
		if (element instanceof ProvElement)
			return ((ProvElement) element).hasChildren(element);
		return false;
	}

	public Object[] getChildren(final Object parent) {
		if (manager != null) {
			Object[] children = manager.getChildren(parent);
			if (children != null) {
				// This will be a placeholder to indicate 
				// that the real children are being fetched
				return children;
			}
		}
		// We don't have a deferred content manager or else it could
		// not retrieve deferred content.
		if (parent instanceof ProvElement) {
			return ((ProvElement) parent).getChildren(parent);
		}
		return null;
	}
}
