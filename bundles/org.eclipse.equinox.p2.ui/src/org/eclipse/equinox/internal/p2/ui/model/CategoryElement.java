/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

/**
 * Element wrapper class for IU's that represent categories of
 * available IU's
 * 
 * @since 3.4
 */
public class CategoryElement extends RemoteQueriedElement implements IIUElement {

	private ArrayList<IInstallableUnit> ius = new ArrayList<>(1);
	private Collection<IRequirement> requirements;
	private Object[] cache = null;

	public CategoryElement(Object parent, IInstallableUnit iu) {
		super(parent);
		ius.add(iu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	@Override
	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_CATEGORY;
	}

	@Override
	public String getLabel(Object o) {
		IInstallableUnit iu = getIU();
		if (iu != null)
			return iu.getId();
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IInstallableUnit.class)
			return (T) getIU();
		return super.getAdapter(adapter);
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_IUS;
	}

	@Override
	public IInstallableUnit getIU() {
		if (ius == null || ius.isEmpty())
			return null;
		return ius.get(0);
	}

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
		// Should never be called, since shouldShowSize() returns false
	}

	@Override
	public boolean shouldShowVersion() {
		return false;
	}

	public void mergeIU(IInstallableUnit iu) {
		ius.add(iu);
	}

	public boolean shouldMerge(IInstallableUnit iu) {
		IInstallableUnit myIU = getIU();
		if (myIU == null)
			return false;
		return getMergeKey(myIU).equals(getMergeKey(iu));
	}

	private String getMergeKey(IInstallableUnit iu) {
		String mergeKey = iu.getProperty(IInstallableUnit.PROP_NAME, null);
		if (mergeKey == null || mergeKey.length() == 0) {
			mergeKey = iu.getId();
		}
		return mergeKey;
	}

	@Override
	public Collection<IRequirement> getRequirements() {
		if (ius == null || ius.isEmpty())
			return Collections.emptyList();
		if (requirements == null) {
			if (ius.size() == 1)
				requirements = getIU().getRequirements();
			else {
				ArrayList<IRequirement> capabilities = new ArrayList<>();
				for (IInstallableUnit iu : ius) {
					capabilities.addAll(iu.getRequirements());
				}
				requirements = capabilities;
			}
		}
		return requirements;
	}

	@Override
	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		if (cache == null)
			cache = super.fetchChildren(o, monitor);
		return cache;
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
		if (!(obj instanceof CategoryElement))
			return false;
		IInstallableUnit myIU = getIU();
		IInstallableUnit objIU = ((CategoryElement) obj).getIU();
		if (myIU == null || objIU == null)
			return false;
		return getMergeKey(myIU).equals(getMergeKey(objIU));
	}

	@Override
	public int hashCode() {
		IInstallableUnit iu = getIU();
		final int prime = 23;
		int result = 1;
		result = prime * result + ((iu == null) ? 0 : getMergeKey(iu).hashCode());
		return result;
	}

	@Override
	public String toString() {
		IInstallableUnit iu = getIU();
		if (iu == null)
			return "NULL"; //$NON-NLS-1$
		StringBuffer result = new StringBuffer();
		result.append("Category Element - "); //$NON-NLS-1$
		result.append(getMergeKey(iu));
		result.append(" (merging IUs: "); //$NON-NLS-1$
		result.append(ius.toString());
		result.append(")"); //$NON-NLS-1$
		return result.toString();
	}
}
