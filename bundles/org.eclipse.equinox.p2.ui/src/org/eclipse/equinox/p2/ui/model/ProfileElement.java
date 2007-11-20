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

import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.query.IProvElementQueryProvider;

/**
 * Element wrapper class for a profile that uses the query
 * mechanism to obtain its contents.
 * 
 * @since 3.4
 */
public class ProfileElement extends RemoteQueriedElement {
	Profile profile;

	public ProfileElement(Profile profile) {
		this.profile = profile;
		setQueryable(profile);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == Profile.class)
			return profile;
		return super.getAdapter(adapter);
	}

	protected String getImageID(Object obj) {
		return ProvUIImages.IMG_PROFILE;
	}

	public String getLabel(Object o) {
		return profile.getProfileId();
	}

	protected int getQueryType() {
		return IProvElementQueryProvider.INSTALLED_IUS;
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
