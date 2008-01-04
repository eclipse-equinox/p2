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
package org.eclipse.equinox.p2.ui.operations;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.Profile;

/**
 * Abstract class representing provisioning profile operations
 * 
 * @since 3.4
 */
public abstract class ProfileOperation extends UndoableProvisioningOperation {

	String[] profileIds;
	// We cache profiles along with ids in case we have to recreate a deleted profile
	Profile[] cachedProfiles;

	ProfileOperation(String label, String[] ids) {
		super(label);
		profileIds = ids;
	}

	ProfileOperation(String label, Profile[] profiles) {
		super(label);
		Assert.isNotNull(profiles);
		cachedProfiles = profiles;
		profileIds = new String[profiles.length];
		for (int i = 0; i < profiles.length; i++) {
			profileIds[i] = profiles[i].getProfileId();
		}
	}

	boolean isValid() {
		return profileIds != null && profileIds.length > 0;
	}

	Profile[] getProfiles() throws ProvisionException {
		if (profileIds == null) {
			return null;
		}
		if (cachedProfiles == null) {
			cachedProfiles = new Profile[profileIds.length];
			for (int i = 0; i < profileIds.length; i++) {
				cachedProfiles[i] = ProvisioningUtil.getProfile(profileIds[i]);
			}
		}
		return cachedProfiles;
	}
}
