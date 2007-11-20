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

import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUIImages;

/**
 * Element wrapper class for installed IU's. Used instead of the plain IU when
 * there should be a parent profile available for operations.
 * 
 * @since 3.4
 */
public class InstalledIUElement extends ProvElement {

	Profile parent;
	IInstallableUnit iu;

	public InstalledIUElement(Profile parent, IInstallableUnit iu) {
		this.parent = parent;
		this.iu = iu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageID(Object obj) {
		return ProvUIImages.IMG_IU;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return parent;
	}

	public String getLabel(Object o) {
		return iu.getId();
	}

	public Object[] getChildren(Object o) {
		return null;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IInstallableUnit.class)
			return iu;
		return super.getAdapter(adapter);
	}

	public Profile getProfile() {
		return parent;
	}

	public IInstallableUnit getIU() {
		return iu;
	}
}
