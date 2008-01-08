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
package org.eclipse.equinox.p2.ui.model;

import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUIImages;

/**
 * Element wrapper class for installed IU's. Used instead of the plain IU when
 * there should be a parent profile available for operations.
 * 
 * @since 3.4
 */
public class InstalledIUElement extends ProvElement implements IUElement {

	String profileId;
	IInstallableUnit iu;

	public InstalledIUElement(String profileId, IInstallableUnit iu) {
		this.profileId = profileId;
		this.iu = iu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_IU;
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

	public String getProfileId() {
		return profileId;
	}

	public IInstallableUnit getIU() {
		return iu;
	}

	// TODO Later we might consider showing this in the installed views,
	// but it is less important than before install.
	public long getSize() {
		return SIZE_UNKNOWN;
	}

	public boolean shouldShowSize() {
		return false;
	}

	public void computeSize() {
		// Should never be called, as long as shouldShowSize() returns false
	}

	public boolean shouldShowVersion() {
		return true;
	}

	public Object getParent(Object o) {
		// we do not know the element
		return null;
	}
}
