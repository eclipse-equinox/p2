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

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.ProvUIActivator;
import org.eclipse.equinox.p2.ui.ProvisioningUtil;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

/**
 * Factory class that can create a new profile with the correct
 * default values.
 * 
 * @since 3.4
 *
 */
public class ProfileFactory {

	static private String FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$
	static private String EMPTY = ""; //$NON-NLS-1$
	static private EnvironmentInfo info;

	public static Profile makeProfile(String profileID) {
		Profile profile = new Profile(profileID);
		profile.setValue(Profile.PROP_INSTALL_FOLDER, getDefaultLocation());
		profile.setValue(Profile.PROP_FLAVOR, getDefaultFlavor());
		profile.setValue(Profile.PROP_ENVIRONMENTS, getDefaultEnvironments());
		profile.setValue(Profile.PROP_NL, getDefaultNL());
		try {
			ProvisioningUtil.addProfile(profile, null);
		} catch (ProvisionException e) {
			return null;
		}
		return profile;
	}

	public static String getDefaultLocation() {
		return Platform.getUserLocation().getURL().getPath();
	}

	public static String getDefaultFlavor() {
		return FLAVOR_DEFAULT;
	}

	private static EnvironmentInfo getEnvironmentInfo() {
		if (info == null) {
			info = (EnvironmentInfo) ServiceHelper.getService(ProvUIActivator.getContext(), EnvironmentInfo.class.getName());
		}
		return info;
	}

	public static String getDefaultNL() {
		if (getEnvironmentInfo() != null) {
			return info.getNL();
		}
		return EMPTY;
	}

	public static String getDefaultEnvironments() {
		if (getEnvironmentInfo() != null) {
			return "osgi.os=" //$NON-NLS-1$
					+ info.getOS() + ",osgi.ws=" + info.getWS() //$NON-NLS-1$
					+ ",osgi.arch=" + info.getOSArch(); //$NON-NLS-1$
		}
		return EMPTY;
	}
}
