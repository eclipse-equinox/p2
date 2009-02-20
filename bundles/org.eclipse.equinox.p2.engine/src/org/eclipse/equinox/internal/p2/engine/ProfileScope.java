/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.core.internal.preferences.AbstractScope;
import org.eclipse.core.runtime.IPath;

public final class ProfileScope extends AbstractScope {

	/*
	 * String constant (value of <code>"profile"</code>) used for the 
	 * scope name for this preference scope.
	 */
	public static final String SCOPE = "profile"; //$NON-NLS-1$

	private String profileId;

	/*
	 * Create and return a new profile scope for the given profile. The given
	 * profile must not be null.
	 */
	public ProfileScope(String profileId) {
		super();
		if (profileId == null)
			throw new IllegalArgumentException();
		this.profileId = profileId;
	}

	public IPath getLocation() {
		// Null returned as the location should only be used when the profile is locked
		return null;
	}

	public String getName() {
		return SCOPE;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProfileScope))
			return false;
		ProfileScope other = (ProfileScope) obj;
		return profileId.equals(other.profileId);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return super.hashCode() + profileId.hashCode();
	}
}
