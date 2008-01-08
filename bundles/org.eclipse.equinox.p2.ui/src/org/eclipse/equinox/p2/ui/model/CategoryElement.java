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

import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.query.IProvElementQueryProvider;

/**
 * Element wrapper class for IU's that represent categories of
 * available IU's
 * 
 * @since 3.4
 */
public class CategoryElement extends RemoteQueriedElement implements IUElement {

	private IInstallableUnit iu;

	public CategoryElement(IInstallableUnit iu) {
		this.iu = iu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_CATEGORY;
	}

	public String getLabel(Object o) {
		return iu.getId();
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IInstallableUnit.class)
			return iu;
		return super.getAdapter(adapter);
	}

	protected int getQueryType() {
		return IProvElementQueryProvider.AVAILABLE_IUS;
	}

	public IInstallableUnit getIU() {
		return iu;
	}

	public long getSize() {
		return SIZE_UNKNOWN;
	}

	public boolean shouldShowSize() {
		return false;
	}

	public void computeSize() {
		// Should never be called, since shouldShowSize() returns false
	}

	public boolean shouldShowVersion() {
		return false;
	}

}
