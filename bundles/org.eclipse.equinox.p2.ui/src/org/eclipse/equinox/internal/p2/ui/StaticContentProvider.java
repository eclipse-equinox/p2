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

package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public final class StaticContentProvider implements IStructuredContentProvider {
	private final Object[] elements;

	public StaticContentProvider(Object[] elements) {
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