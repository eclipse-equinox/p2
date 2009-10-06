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

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.QueryProvider;
import org.eclipse.osgi.util.NLS;

/**
 * Element wrapper class for a profile that uses the query
 * mechanism to obtain its contents.
 * 
 * @since 3.4
 */
public class ProfileElement extends RemoteQueriedElement {
	String profileId;

	public ProfileElement(Object parent, String profileId) {
		super(parent);
		this.profileId = profileId;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IProfile.class)
			return getQueryable();
		return super.getAdapter(adapter);
	}

	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_PROFILE;
	}

	public String getLabel(Object o) {
		return profileId;
	}

	public String getProfileId() {
		return profileId;
	}

	protected int getDefaultQueryType() {
		return QueryProvider.INSTALLED_IUS;
	}

	public IQueryable getQueryable() {
		try {
			return ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			handleException(e, NLS.bind(ProvUIMessages.ProfileElement_InvalidProfile, profileId));
			return null;
		}
	}

	/*
	 * Overridden to check whether we know the profile id rather
	 * than fetch the profile from the registry using getQueryable()
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#knowsQueryable()
	 */
	public boolean knowsQueryable() {
		return profileId != null;
	}

	/*
	 * Overridden to check the children so that profiles
	 * showing in profile views accurately reflect if they
	 * are empty.  We do not cache the children because often
	 * this element is the input of a view and when the view
	 * is refreshed we want to refetch the children.
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement#isContainer()
	 */
	public boolean isContainer() {
		return super.getChildren(this).length > 0;
	}
}
