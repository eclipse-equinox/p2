/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;



/**
 * Element class representing a fixed set of IU's.  This element should
 * never appear in a list, but can be used as a parent in a list.
 * 
 * @since 3.5
 */
public class IUElementListRoot extends ProvElement {

	Object[] children;

	public IUElementListRoot(Object[] children) {
		super(null);
		this.children = children;
	}

	public IUElementListRoot() {
		this(new Object[0]);
	}

	public void setChildren(Object[] children) {
		this.children = children;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageId(Object obj) {
		return null;
	}

	public String getLabel(Object o) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object o) {
		return children;
	}
}
