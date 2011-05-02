/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.QueryProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Element class that represents available updates.
 * 
 * @since 3.4
 *
 */
public class Updates extends QueriedElement {

	private String profileId;
	private IInstallableUnit[] iusToBeUpdated;

	public Updates(String profileId) {
		this(profileId, null);
	}

	public Updates(String profileId, IInstallableUnit[] iusToBeUpdated) {
		super(null);
		this.profileId = profileId;
		this.iusToBeUpdated = iusToBeUpdated;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	public String getLabel(Object o) {
		return ProvUIMessages.Updates_Label;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getDefaultQueryType()
	 */
	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_UPDATES;
	}

	public String getProfileId() {
		return profileId;
	}

	public IInstallableUnit[] getIUs() {
		return iusToBeUpdated;
	}

}
