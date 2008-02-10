package org.eclipse.equinox.internal.provisional.p2.engine;

import java.util.Map;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;

public interface IProfile extends IQueryable {

	/**
	 * Profile property constant indicating the flavor for the profile.
	 */
	public static final String PROP_FLAVOR = "eclipse.p2.flavor"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the install folder for the profile.
	 */
	public static final String PROP_INSTALL_FOLDER = "eclipse.p2.installFolder"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the installed language(s) for the profile.
	 */
	public static final String PROP_NL = "eclipse.p2.nl"; //$NON-NLS-1$
	/**
	 * Profile property constant for a string property indicating a user visible short 
	 * textual description of this profile. May be empty or <code>null</code>, and 
	 * generally will be for non-top level install contexts.
	 */
	public static final String PROP_DESCRIPTION = "eclipse.p2.description"; //$NON-NLS-1$
	/**
	 * Profile property constant for a string property indicating a user visible name of this profile.
	 * May be empty or <code>null</code>, and generally will be for non-top level
	 * install contexts.
	 */
	public static final String PROP_NAME = "eclipse.p2.name"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the list of environments
	 * (e.g., OS, WS, ...) in which a profile can operate. The value of the property
	 * is a comma-delimited string of key/value pairs.
	 */
	public static final String PROP_ENVIRONMENTS = "eclipse.p2.environments"; //$NON-NLS-1$
	/**
	 * Profile property constant for a boolean property indicating if the profiling
	 * is roaming.  A roaming profile is one whose physical install location varies
	 * and is updated whenever it runs.
	 */
	public static final String PROP_ROAMING = "eclipse.p2.roaming"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the bundle pool cache location.
	 */
	public static final String PROP_CACHE = "eclipse.p2.cache"; //$NON-NLS-1$
	/**
	 * Profile property constant indicating the bundle pool cache location.
	 */
	public static final String PROP_INSTALL_FEATURES = "eclipse.p2.install.features"; //$NON-NLS-1$

	public String getProfileId();

	public IProfile getParentProfile();

	/*
	 * 	A profile is a root profile if it is not a sub-profile
	 * 	of another profile.
	 */
	public boolean isRootProfile();

	public boolean hasSubProfiles();

	public String[] getSubProfileIds();

	/**
	 * 	Get the stored value associated with the given key.
	 * 	If the profile is a sub-profile and there is no value
	 * 	locally associated with the key, then the chain
	 * 	of parent profiles will be traversed to get an associated
	 *  value from the nearest ancestor.
	 *  
	 *  <code>null</code> is return if none of this profile
	 *  or its ancestors associates a value with the key.
	 */
	public String getProperty(String key);

	/**
	 * 	Get the stored value associated with the given key
	 * 	in this profile.
	 * 	No traversal of the ancestor hierarchy is done
	 * 	for sub-profiles.
	 */
	public String getLocalProperty(String key);

	public String getInstallableUnitProperty(IInstallableUnit iu, String key);

	/**
	 * Get an <i>unmodifiable copy</i> of the local properties
	 * associated with the profile.
	 * 
	 * @return an <i>unmodifiable copy</i> of the Profile properties.
	 */
	public Map getLocalProperties();

	public Map getProperties();

	public OrderedProperties getInstallableUnitProperties(IInstallableUnit iu);

}