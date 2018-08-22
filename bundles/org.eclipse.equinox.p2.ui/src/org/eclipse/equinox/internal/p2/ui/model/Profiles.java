/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.QueryProvider;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Element class that represents the root of a profile
 * viewer.  Its children are the profiles that match the
 * specified query for profiles.
 * 
 * @since 3.4
 *
 */
public class Profiles extends RootElement {

	public Profiles(ProvisioningUI ui) {
		super(ui);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	@Override
	public String getLabel(Object o) {
		return ProvUIMessages.Label_Profiles;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getDefaultQueryType()
	 */
	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.PROFILES;
	}

}
