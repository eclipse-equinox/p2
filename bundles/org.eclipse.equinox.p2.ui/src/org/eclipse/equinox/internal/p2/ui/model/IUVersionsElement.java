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
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.QueryProvider;

/**
 * Element wrapper class for a particular IU version, whose
 * children are the other versions.
 * 
 * @since 3.4
 */
public class IUVersionsElement extends QueriedElement implements IUElement {

	IInstallableUnit iu;

	public IUVersionsElement(Object parent, IInstallableUnit iu) {
		super(parent);
		this.iu = iu;
	}

	public String getLabel(Object o) {
		return iu.getId();
	}

	public String getImageId(Object o) {
		return ProvUIImages.IMG_UNINSTALLED_IU;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IInstallableUnit.class)
			return iu;
		return super.getAdapter(adapter);
	}

	public IInstallableUnit getIU() {
		return iu;
	}

	public void setIU(IInstallableUnit iu) {
		this.iu = iu;
	}

	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_IUS;
	}

	public long getSize() {
		return SIZE_UNKNOWN;
	}

	public boolean shouldShowSize() {
		return false;
	}

	public boolean shouldShowVersion() {
		return true;
	}

	public void computeSize(IProgressMonitor monitor) {
		// Should never be called, since shouldShowSize() returns false
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.IUElement#getRequirements()
	 */
	public RequiredCapability[] getRequirements() {
		return iu.getRequiredCapabilities();
	}
}
