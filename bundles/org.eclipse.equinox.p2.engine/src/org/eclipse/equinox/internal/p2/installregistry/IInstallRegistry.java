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
package org.eclipse.equinox.internal.p2.installregistry;

import java.util.Collection;
import org.eclipse.equinox.p2.engine.Profile;

/**
 * The install registry records information about a profile, including profile
 * properties, and the installable units stored in that profile.
 */
public interface IInstallRegistry {

	/**
	 * Returns the install registry for the given profile. Creates a new install
	 * registry for the profile if none exists.
	 * @param profile The profile to obtain the registry for
	 * @return The install registry for the given profile.
	 */
	public abstract IProfileInstallRegistry getProfileInstallRegistry(Profile profile);

	/**
	 * Returns the profile install registries of all profiles known to the install registry.
	 * @return A Collection of IProfileInstallRegistry
	 */
	public abstract Collection getProfileInstallRegistries();
}