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
import java.util.Objects;
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

	@Override
	public Collection<IRequirement> getRequirements() {
		return iu.getRequirements();
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.INSTALLED_IUS;
	}

	@Override
	public boolean shouldShowChildren() {
		// Check that no parent has the same IU as this parent.
		// That would lead to a cycle and induce an infinite tree.
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=550265
		for (Object parent = getParent(this); parent instanceof InstalledIUElement;) {
			InstalledIUElement installedIUElement = (InstalledIUElement) parent;
			if (Objects.equals(iu, installedIUElement.getIU())) {
				return false;
			}
			parent = installedIUElement.getParent(installedIUElement);
		}
		return true;
	}

	@Override
	public Object[] getChildren(Object o) {
		if (shouldShowChildren()) {
			// Only show children if that would not induce a cycle.
			return super.getChildren(o);
		}

		return new Object[0];
	}

	@Override
	protected Object[] getFilteredChildren(Collection<?> results) {
		// Given the equality definition, a child cannot be equal to a sibling of this
		// because the child has a different parent.
		return results.toArray();
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
