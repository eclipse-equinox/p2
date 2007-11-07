/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.installregistry.IInstallRegistry;
import org.eclipse.equinox.p2.installregistry.IProfileInstallRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryableArray;
import org.eclipse.osgi.service.resolver.VersionRange;

public class Profile implements IQueryable {

	/**
	 * Profile property constant indicating the flavor for the profile.
	 */
	public static String PROP_FLAVOR = "eclipse.p2.flavor"; //$NON-NLS-1$

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

	//Internal id of the profile
	private String profileId;

	private Profile parentProfile;

	/**
	 * 	TODO: need a collection of child profiles
	 */

	/**
	 * This storage is to be used by the touchpoints to store data. The data must be serializable
	 */
	private OrderedProperties storage = new OrderedProperties();

	public Profile(String profileId) {
		if (profileId == null || profileId.length() == 0) {
			throw new IllegalArgumentException("Profile id must be set.");
		}
		this.profileId = profileId;
	}

	public Profile(String profileId, Map properties) {
		this(profileId);
		storage.putAll(properties);
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
		OrderedProperties result = new OrderedProperties(storage);
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

	public String getInstallableUnitProfileProperty(IInstallableUnit iu, String key) {
		IInstallRegistry registry = (IInstallRegistry) ServiceHelper.getService(EngineActivator.getContext(), IInstallRegistry.class.getName());
		if (registry == null)
			return null;
		IProfileInstallRegistry profile = registry.getProfileInstallRegistry(this);
		if (profile == null)
			return null;
		return profile.getInstallableUnitProfileProperty(iu, key);
	}

	public String setInstallableUnitProfileProperty(IInstallableUnit iu, String key, String value) {
		IInstallRegistry registry = (IInstallRegistry) ServiceHelper.getService(EngineActivator.getContext(), IInstallRegistry.class.getName());
		if (registry == null)
			return null;
		IProfileInstallRegistry profile = registry.getProfileInstallRegistry(this);
		if (profile == null)
			return null;
		String previousValue = profile.setInstallableUnitProfileProperty(iu, key, value);
		// TODO this is not the ideal place for this.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=206077
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=197701
		ProvisioningEventBus bus = (ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName());
		if (bus != null)
			bus.publishEvent(new ProfileEvent(this, ProfileEvent.CHANGED));
		return previousValue;
	}

	/**
	 * Get an <i>unmodifiable copy</i> of the properties
	 * associated with the profile registry.
	 * 
	 * @return an <i>unmodifiable copy</i> of the IU properties.
	 */
	public Map getProperties() {
		return OrderedProperties.unmodifiableProperties(storage);
	}
}
