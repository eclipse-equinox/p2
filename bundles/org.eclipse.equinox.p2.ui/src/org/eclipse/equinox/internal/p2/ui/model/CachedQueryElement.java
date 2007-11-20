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

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Element wrapper class for an element who obtains its 
 * children via a query, but caches its results in order
 * to accurately report the presence of children.  Should be
 * used when accurate child reporting is more critical than the
 * space used by the cache.
 * 
 * @since 3.4
 */
public abstract class CachedQueryElement extends RemoteQueriedElement {

	Object[] cachedChildren;

	/*
	 * Overridden to cache the children so that we can more
	 * quickly and accurately report whether other versions
	 * are available.
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement#fetchChildren(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		if (cachedChildren == null)
			cachedChildren = super.fetchChildren(o, monitor);
		return cachedChildren;
	}

	/*
	 * Overridden to give a more accurate answer since often there
	 * are no children.
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement#isContainer()
	 */
	public boolean isContainer() {
		return fetchChildren(this, null).length > 0;
	}

}
