/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine.query;

import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

public class UserVisibleRootQuery extends IUProfilePropertyQuery {

	public UserVisibleRootQuery() {
		super(IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
	}

	public static boolean isUserVisible(IProfile profile, IInstallableUnit iu) {
		String value = profile.getInstallableUnitProperty(iu, IProfile.PROP_PROFILE_ROOT_IU);
		if (value != null && (value.equals(Boolean.TRUE.toString())))
			return true;
		return false;
	}
}
