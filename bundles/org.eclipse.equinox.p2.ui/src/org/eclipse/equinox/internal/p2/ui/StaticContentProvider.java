/**
 * 
 */
package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public final class StaticContentProvider implements IStructuredContentProvider {
	private final Object[] elements;

	StaticContentProvider(Object[] elements) {
		this.elements = elements;
	}

	public Object[] getElements(Object inputElement) {
		return elements;
	}

	public void dispose() {
		// nothing to dispose
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// input is static
	}
}