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
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement;

/**
 * Element wrapper class for an element who obtains its 
 * children via a query, but caches its results.  
 * 
 * @since 3.4
 */
public abstract class CachedQueryElement extends QueriedElement {

	Object[] cachedChildren;

	protected CachedQueryElement() {
		super(null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parent) {
		if (cachedChildren == null)
			cachedChildren = super.getChildren(parent);
		return cachedChildren;
	}
}
