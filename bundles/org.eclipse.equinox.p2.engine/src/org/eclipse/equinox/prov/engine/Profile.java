/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.engine;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.prov.engine.EngineActivator;
import org.eclipse.equinox.prov.core.helpers.ServiceHelper;
import org.eclipse.equinox.prov.installregistry.IInstallRegistry;
import org.eclipse.equinox.prov.installregistry.IProfileInstallRegistry;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.RequiredCapability;
import org.eclipse.equinox.prov.query.IQueryable;
import org.eclipse.equinox.prov.query.QueryableArray;
import org.eclipse.osgi.service.resolver.VersionRange;

public class Profile implements IQueryable {

	/**
	 * Profile property constant indicating the flavor for the profile.
	 */
	public static String PROP_FLAVOR = "eclipse.prov.flavor"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating the install folder for the profile.
	 */
	public static final String PROP_INSTALL_FOLDER = "eclipse.prov.installFolder"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating the installed language(s) for the profile.
	 */
	public static final String PROP_NL = "eclipse.prov.nl"; //$NON-NLS-1$

	/**
	 * Profile property constant for a string property indicating a user visible short 
	 * textual description of this profile. May be empty or <code>null</code>, and 
	 * generally will be for non-top level install contexts.
	 */
	public static final String PROP_DESCRIPTION = "eclipse.prov.description"; //$NON-NLS-1$

	/**
	 * Profile property constant for a string property indicating a user visible name of this profile.
	 * May be empty or <code>null</code>, and generally will be for non-top level
	 * install contexts.
	 */
	public static final String PROP_NAME = "eclipse.prov.name"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating the list of environments
	 * (e.g., OS, WS, ...) in which a profile can operate. The value of the property
	 * is a comma-delimited string of key/value pairs.
	 */
	public static final String PROP_ENVIRONMENTS = "eclipse.prov.environments"; //$NON-NLS-1$

	/**
	 * Profile property constant for a boolean property indicating if the profiling
	 * is roaming.  A roaming profile is one whose physical install location varies
	 * and is updated whenever it runs.
	 */
	public static final String PROP_ROAMING = "eclipse.prov.roaming"; //$NON-NLS-1$

	/**
	 * Profile property constant indicating the bundle pool cache location.
	 */
	public static final String PROP_CACHE = "eclipse.prov.cache"; //$NON-NLS-1$

	//Internal id of the profile
	private String profileId;

	private Profile parentProfile;

	/**
	 * This storage is to be used by the touchpoints to store data. The data must be serializable
	 */
	private Properties storage = new Properties();

	public Profile(String profileId) {
		if (profileId == null || profileId.length() == 0) {
			throw new IllegalArgumentException("Profile id must be set.");
		}
		this.profileId = profileId;
	}

	public String getProfileId() {
		return profileId;
	}

	public Profile getParentProfile() {
		return parentProfile;
	}

	public String getValue(String key) {
		return storage.getProperty(key);
	}

	public void setValue(String key, String value) {
		storage.setProperty(key, value);
	}

	public Dictionary getSelectionContext() {
		Properties result = new Properties(storage);
		String environments = storage.getProperty(PROP_ENVIRONMENTS);
		if (environments == null)
			return result;
		for (StringTokenizer tokenizer = new StringTokenizer(environments, ","); tokenizer.hasMoreElements();) {
			String entry = tokenizer.nextToken();
			int i = entry.indexOf('=');
			String key = entry.substring(0, i).trim();
			String value = entry.substring(i + 1).trim();
			result.put(key, value);
		}
		return result;
	}

	private IInstallableUnit[] getAllInstallableUnits() {
		IInstallRegistry registry = (IInstallRegistry) ServiceHelper.getService(EngineActivator.getContext(), IInstallRegistry.class.getName());
		if (registry == null)
			return null;
		IProfileInstallRegistry profile = registry.getProfileInstallRegistry(new Profile(profileId));
		if (profile == null)
			return null;
		return profile.getInstallableUnits();
	}

	public Iterator getIterator(String id, VersionRange range, RequiredCapability[] requirements, boolean and) {
		return new QueryableArray(getAllInstallableUnits()).getIterator(id, range, requirements, and);
	}

	public IInstallableUnit[] query(String id, VersionRange range, RequiredCapability[] requirements, boolean and, IProgressMonitor progress) {
		return new QueryableArray(getAllInstallableUnits()).query(id, range, requirements, and, progress);
	}

	public Iterator getInstallableUnits() {
		return Arrays.asList(getAllInstallableUnits()).iterator();
	}
}
