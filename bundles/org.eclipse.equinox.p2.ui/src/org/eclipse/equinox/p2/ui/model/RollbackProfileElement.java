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

import java.text.DateFormat;
import java.util.*;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.ui.ProvUIImages;

/**
 * Element wrapper class for IU's that represent categories of
 * available IU's
 * 
 * @since 3.4
 */
public class RollbackProfileElement extends ProvElement implements IUElement {

	private IInstallableUnit iu;

	public RollbackProfileElement(IInstallableUnit iu) {
		this.iu = iu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageID(Object obj) {
		return ProvUIImages.IMG_PROFILE;
	}

	public String getLabel(Object o) {
		return DateFormat.getInstance().format(new Date(Long.decode(iu.getVersion().getQualifier()).longValue()));
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IInstallableUnit.class)
			return iu;
		return super.getAdapter(adapter);
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

	public boolean shouldShowVersion() {
		return false;
	}

	public Object[] getChildren(Object o) {
		RequiredCapability[] reqs = iu.getRequiredCapabilities();
		List roots = new ArrayList(reqs.length);
		// TODO we really want to filter out install roots
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=197701
		for (int i = 0; i < reqs.length; i++)
			if (IInstallableUnit.NAMESPACE_IU.equals(reqs[i].getNamespace()))
				roots.add(reqs[i]);
		return roots.toArray();
	}

	public Object getParent(Object o) {
		return null;
	}

}
