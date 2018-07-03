/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Fix compiler problems from generified IAdaptable#getAdapter
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;

/**
 * Element wrapper class for installed IU's. Used instead of the plain IU when
 * there should be a parent profile available for operations.
 * 
 * @since 3.4
 */
public class InstalledIUElement extends QueriedElement implements IIUElement {

	String profileId;
	IInstallableUnit iu;
	boolean isPatch = false;

	public InstalledIUElement(Object parent, String profileId, IInstallableUnit iu) {
		super(parent);
		this.profileId = profileId;
		this.iu = iu;
		this.isPatch = iu == null ? false : Boolean.valueOf(iu.getProperty(InstallableUnitDescription.PROP_TYPE_PATCH));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	@Override
	protected String getImageId(Object obj) {
		return isPatch ? ProvUIImages.IMG_PATCH_IU : ProvUIImages.IMG_IU;
	}

	@Override
	public String getLabel(Object o) {
		return iu.getId();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IInstallableUnit.class)
			return (T) iu;
		return super.getAdapter(adapter);
	}

	public String getProfileId() {
		return profileId;
	}

	@Override
	public IInstallableUnit getIU() {
		return iu;
	}

	// TODO Later we might consider showing this in the installed views,
	// but it is less important than before install.
	@Override
	public long getSize() {
		return ProvUI.SIZE_UNKNOWN;
	}

	@Override
	public boolean shouldShowSize() {
		return false;
	}

	@Override
	public void computeSize(IProgressMonitor monitor) {
		// Should never be called, as long as shouldShowSize() returns false
	}

	@Override
	public boolean shouldShowVersion() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.IUElement#getRequirements()
	 */
	@Override
	public Collection<IRequirement> getRequirements() {
		return iu.getRequirements();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.QueriedElement#getDefaultQueryType()
	 */
	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.INSTALLED_IUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.IIUElement#shouldShowChildren()
	 */
	@Override
	public boolean shouldShowChildren() {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof InstalledIUElement))
			return false;
		if (iu == null)
			return false;
		if (!iu.equals(((InstalledIUElement) obj).getIU()))
			return false;

		Object parent = getParent(this);
		Object objParent = ((InstalledIUElement) obj).getParent(obj);
		if (parent == this)
			return objParent == obj;
		else if (parent != null && objParent != null)
			return parent.equals(objParent);
		else if (parent == null && objParent == null)
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		if (iu == null)
			return 0;
		return iu.hashCode();
	}

	@Override
	public String toString() {
		if (iu == null)
			return "NULL"; //$NON-NLS-1$
		return iu.toString();
	}

}
