/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.viewers;

import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider that retrieves children of a ProvElement.
 * 
 * @since 3.5
 * 
 */
public class ProvElementContentProvider implements ITreeContentProvider {

	/**
	 * 
	 */
	public ProvElementContentProvider() {
		// Default constructor
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object input) {
		if (input instanceof ProvElement) {
			return getChildren(input);
		}
		return new Object[0];
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object child) {
		if (child instanceof ProvElement) {
			return ((ProvElement) child).getParent(child);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof ProvElement)
			return ((ProvElement) element).hasChildren(element);
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(final Object parent) {
		if (parent instanceof ProvElement) {
			return ((ProvElement) parent).getChildren(parent);
		}
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		// Nothing to do

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Nothing to do
	}
}
