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

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;

/**
 * Element class that represents the root of a profile
 * viewer.  Its children are all installed profiles.
 * 
 * @since 3.4
 *
 */
public class AllProfiles extends ProvElement {

	public Object[] getChildren(Object o) {
		try {
			return ProvisioningUtil.getProfiles();
		} catch (ProvisionException e) {
			handleException(e, null);
		}
		return new Object[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.p2.ui.model.RepositoryElement#getParent(java.lang.Object)
	 */
	public Object getParent(Object o) {
		return null;
	}

}
