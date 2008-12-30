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
package org.eclipse.equinox.internal.provisional.p2.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElement;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.QueryProvider;

/**
 * Element wrapper class for installed IU's. Used instead of the plain IU when
 * there should be a parent profile available for operations.
 * 
 * @since 3.4
 */
public class InstalledIUElement extends QueriedElement implements IIUElement {

	String profileId;
	IInstallableUnit iu;

	public InstalledIUElement(Object parent, String profileId, IInstallableUnit iu) {
		super(parent);
		this.profileId = profileId;
		this.iu = iu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_IU;
	}

	public String getLabel(Object o) {
		return iu.getId();
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

	public void computeSize(IProgressMonitor monitor) {
		// Should never be called, as long as shouldShowSize() returns false
	}

	public boolean shouldShowVersion() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.IUElement#getRequirements()
	 */
	public IRequiredCapability[] getRequirements() {
		return iu.getRequiredCapabilities();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.QueriedElement#getDefaultQueryType()
	 */
	protected int getDefaultQueryType() {
		return QueryProvider.INSTALLED_IUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.IIUElement#shouldShowChildren()
	 */
	public boolean shouldShowChildren() {
		return true;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof InstalledIUElement))
			return false;
		if (iu == null)
			return false;
		return iu.equals(((InstalledIUElement) obj).getIU());
	}

	public int hashCode() {
		if (iu == null)
			return 0;
		return iu.hashCode();
	}

	public String toString() {
		if (iu == null)
			return "NULL"; //$NON-NLS-1$
		return iu.toString();
	}

}
