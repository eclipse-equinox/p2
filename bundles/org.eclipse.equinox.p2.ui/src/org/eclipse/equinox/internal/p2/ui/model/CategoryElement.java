/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.ui.IUPropertyUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.QueryProvider;

/**
 * Element wrapper class for IU's that represent categories of
 * available IU's
 * 
 * @since 3.4
 */
public class CategoryElement extends RemoteQueriedElement implements IIUElement {

	private ArrayList ius = new ArrayList(1);
	private IRequiredCapability[] requirements;

	public CategoryElement(Object parent, IInstallableUnit iu) {
		super(parent);
		ius.add(iu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_CATEGORY;
	}

	public String getLabel(Object o) {
		IInstallableUnit iu = getIU();
		if (iu != null)
			return iu.getId();
		return null;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IInstallableUnit.class)
			return getIU();
		return super.getAdapter(adapter);
	}

	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_IUS;
	}

	public IInstallableUnit getIU() {
		if (ius == null || ius.isEmpty())
			return null;
		return (IInstallableUnit) ius.get(0);
	}

	public long getSize() {
		return SIZE_UNKNOWN;
	}

	public boolean shouldShowSize() {
		return false;
	}

	public void computeSize(IProgressMonitor monitor) {
		// Should never be called, since shouldShowSize() returns false
	}

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
		String mergeKey = IUPropertyUtils.getIUProperty(iu, IInstallableUnit.PROP_NAME);
		if (mergeKey == null || mergeKey.length() == 0) {
			mergeKey = iu.getId();
		}
		return mergeKey;
	}

	public IRequiredCapability[] getRequirements() {
		if (ius == null || ius.isEmpty())
			return new IRequiredCapability[0];
		if (requirements == null) {
			if (ius.size() == 1)
				requirements = getIU().getRequiredCapabilities();
			else {
				ArrayList capabilities = new ArrayList();
				Iterator iter = ius.iterator();
				while (iter.hasNext()) {
					IInstallableUnit iu = (IInstallableUnit) iter.next();
					capabilities.addAll(Arrays.asList(iu.getRequiredCapabilities()));
				}
				requirements = (IRequiredCapability[]) capabilities.toArray(new IRequiredCapability[capabilities.size()]);
			}
		}
		return requirements;
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
		if (!(obj instanceof CategoryElement))
			return false;
		IInstallableUnit myIU = getIU();
		IInstallableUnit objIU = ((CategoryElement) obj).getIU();
		if (myIU == null || objIU == null)
			return false;
		return getMergeKey(myIU).equals(getMergeKey(objIU));
	}

	public int hashCode() {
		IInstallableUnit iu = getIU();
		final int prime = 23;
		int result = 1;
		result = prime * result + ((iu == null) ? 0 : getMergeKey(iu).hashCode());
		return result;
	}

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
