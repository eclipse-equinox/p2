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
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;

/**
 * Abstract class representing provisioning profile operations
 * 
 * @since 3.4
 */
public abstract class ProfileOperation extends UndoableProvisioningOperation {

	String[] profileIds;
	// We cache profiles along with ids in case we have to recreate a deleted profile
	IProfile[] cachedProfiles;

	ProfileOperation(String label, String[] ids) {
		super(label);
		profileIds = ids;
	}

	ProfileOperation(String label, IProfile[] profiles) {
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

	IProfile[] getProfiles() throws ProvisionException {
		if (profileIds == null) {
			return null;
		}
		if (cachedProfiles == null) {
			cachedProfiles = new IProfile[profileIds.length];
			for (int i = 0; i < profileIds.length; i++) {
				cachedProfiles[i] = ProvisioningUtil.getProfile(profileIds[i]);
			}
		}
		return cachedProfiles;
	}
}
