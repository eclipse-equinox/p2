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

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.model.IUElement;

/**
 * Element wrapper class for IU's that are available for installation.
 * Used instead of the plain IU when additional information such as sizing
 * info is necessary.
 * 
 * @since 3.4
 */
public class AvailableIUElement extends ProvElement implements IUElement {

	long size;
	IInstallableUnit iu;

	public AvailableIUElement(IInstallableUnit iu, long size) {
		this.size = size;
		this.iu = iu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageID(Object obj) {
		return ProvUIImages.IMG_UNINSTALLED_IU;
	}

	public String getLabel(Object o) {
		return iu.getId();
	}

	public Object[] getChildren(Object o) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.ui.model.ProvElement#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return null;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IInstallableUnit.class)
			return iu;
		return super.getAdapter(adapter);
	}

	public long getSize() {
		return size;
	}

	public IInstallableUnit getIU() {
		return iu;
	}

	public boolean shouldShowSize() {
		return true;
	}

	public boolean shouldShowVersion() {
		return true;
	}
}
