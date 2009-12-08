/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * @since 2.0
 */
public interface IProfile extends IQueryable {

	/**
	 * Constant used to indicate that an installable unit is not locked in anyway.
	 * @see #PROP_PROFILE_LOCKED_IU
	 */
	public static int LOCK_NONE = 0;
	/**
	 * Constant used to indicate that an installable unit is locked so that it may
	 * not be uninstalled.
	 * @see #PROP_PROFILE_LOCKED_IU
	 */
	public static int LOCK_UNINSTALL = 1 << 0;
	/**
	 * Constant used to indicate that an installable unit is locked so that it may
	 * not be updated. updates.
	 * @see #PROP_PROFILE_LOCKED_IU
	 */
	public static int LOCK_UPDATE = 1 << 1;

	/**
	 * A property key (value <code>"org.eclipse.equinox.p2.type.lock"</code>) for an
	 * integer property indicating how an installable unit is locked in its profile.
	 * The integer is a bit-mask indicating the different locks defined on the installable
	 * unit.  The property should be obtained from a profile using 
	 * IProfile#getInstallableUnitProperty(IInstallableUnit, String).
	 * 
	 * @see #LOCK_UNINSTALL
	 * @see #LOCK_UPDATE
	 * @see #LOCK_NONE
	 */
	public static final String PROP_PROFILE_LOCKED_IU = "org.eclipse.equinox.p2.type.lock"; //$NON-NLS-1$

	//TODO Move to UI
	public static final String PROP_PROFILE_ROOT_IU = "org.eclipse.equinox.p2.type.root"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating the install folder for the profile.
	 */
	public static final String PROP_INSTALL_FOLDER = "org.eclipse.equinox.p2.installFolder"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the configuration folder for the profile.
	 */
	public static final String PROP_CONFIGURATION_FOLDER = "org.eclipse.equinox.p2.configurationFolder"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the location of the launcher configuration file for the profile.
	 */
	public static final String PROP_LAUNCHER_CONFIGURATION = "org.eclipse.equinox.p2.launcherConfiguration"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating the installed language(s) for the profile.
	 */
	public static final String PROP_NL = "org.eclipse.equinox.p2.nl"; //$NON-NLS-1$
	/**
	 * Profile property constant for a string property indicating a user visible short 
	 * textual description of this profile. May be empty or <code>null</code>, and 
	 * generally will be for non-top level install contexts.
	 */
	public static final String PROP_DESCRIPTION = "org.eclipse.equinox.p2.description"; //$NON-NLS-1$
	/**
	 * Profile property constant for a string property indicating a user visible name of this profile.
	 * May be empty or <code>null</code>, and generally will be for non-top level
	 * install contexts.
	 */
	public static final String PROP_NAME = "org.eclipse.equinox.p2.name"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the list of environments
	 * (e.g., OS, WS, ...) in which a profile can operate. The value of the property
	 * is a comma-delimited string of key/value pairs.
	 */
	public static final String PROP_ENVIRONMENTS = "org.eclipse.equinox.p2.environments"; //$NON-NLS-1$
	/**
	 * Profile property constant for a boolean property indicating if the profiling
	 * is roaming.  A roaming profile is one whose physical install location varies
	 * and is updated whenever it runs.
	 */
	public static final String PROP_ROAMING = "org.eclipse.equinox.p2.roaming"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the bundle pool cache location.
	 */
	public static final String PROP_CACHE = "org.eclipse.equinox.p2.cache"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating a shared read-only bundle pool cache location.
	 */
	public static final String PROP_SHARED_CACHE = "org.eclipse.equinox.p2.cache.shared"; //$NON-NLS-1$

	/**
	 * Profile property constant for a boolean property indicating if update features should
	 * be installed in this profile
	 */
	public static final String PROP_INSTALL_FEATURES = "org.eclipse.update.install.features"; //$NON-NLS-1$

	public String getProfileId();

	/**
	 * Get the stored value associated with the given key.
	 *  
	 * <code>null</code> is returned if this property is not present
	 */
	public String getProperty(String key);

	public String getInstallableUnitProperty(IInstallableUnit iu, String key);

	public Map getProperties();

	public Map getInstallableUnitProperties(IInstallableUnit iu);

	public long getTimestamp();

	public Collector available(IQuery query, IProgressMonitor monitor);

}