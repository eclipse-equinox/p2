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
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.internal.p2.installregistry.IInstallRegistry;
import org.eclipse.equinox.internal.p2.installregistry.IProfileInstallRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.util.NLS;

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
	 * 	A collection of child profiles.
	 */
	private Map subProfiles = null; // Map profile id -> child profiles

	private static Profile[] noSubProfiles = new Profile[0];
	/**
	 * This storage is to be used by the touchpoints to store data.
	 */
	private OrderedProperties storage = new OrderedProperties();

	/**
	 * iuProperties are stored by the install registry
	 */
	private Map iuProperties = new HashMap();
	private boolean changed = false;

	public Profile(String profileId) {
		this(profileId, null, null);
	}

	public Profile(String profileId, Profile parent) {
		this(profileId, parent, null);
	}

	public Profile(String profileId, Profile parent, Map properties) {
		if (profileId == null || profileId.length() == 0) {
			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Null_Profile_Id, null));
		}
		this.profileId = profileId;
		this.parentProfile = parent;
		if (parent != null) {
			parent.addSubprofile(this);
		}
		if (properties != null)
			storage.putAll(properties);

		populateIUs();
	}

	private void populateIUs() {
		IInstallRegistry installRegistry = (IInstallRegistry) ServiceHelper.getService(EngineActivator.getContext(), IInstallRegistry.class.getName());
		if (installRegistry == null)
			return;
		//TODO: Should be using profile id not "this"
		IProfileInstallRegistry profileInstallRegistry = installRegistry.getProfileInstallRegistry(this);
		if (profileInstallRegistry == null)
			return;

		IInstallableUnit[] ius = profileInstallRegistry.getInstallableUnits();
		if (ius == null)
			return;

		for (int i = 0; i < ius.length; i++) {
			IInstallableUnit iu = ius[i];
			OrderedProperties properties = profileInstallRegistry.getInstallableUnitProfileProperties(iu);
			iuProperties.put(iu, new OrderedProperties(properties));
		}
	}

	public String getProfileId() {
		return profileId;
	}

	public Profile getParentProfile() {
		return parentProfile;
	}

	/*
	 * 	A profile is a root profile if it is not a sub-profile
	 * 	of another profile.
	 */
	public boolean isARootProfile() {
		return (parentProfile == null ? true : false);
	}

	protected void addSubprofile(Profile subprofile) throws IllegalArgumentException {
		if (subProfiles == null) {
			subProfiles = new LinkedHashMap(2);
		}
		if (subProfiles.containsKey(subprofile.getProfileId())) {
			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Duplicate_Child_Profile_Id, new String[] {subprofile.getProfileId(), this.getProfileId()}));
		}
		subProfiles.put(subprofile.getProfileId(), subprofile);
	}

	public boolean hasSubProfiles() {
		return (subProfiles != null ? !subProfiles.isEmpty() : false);
	}

	public Profile[] getSubProfiles() {
		if (subProfiles == null)
			return noSubProfiles;
		Collection values = subProfiles.values();
		return (Profile[]) values.toArray(new Profile[values.size()]);
	}

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
	public String getValue(String key) {
		String value = storage.getProperty(key);
		if (value == null && parentProfile != null) {
			value = parentProfile.getValue(key);
		}
		return value;
	}

	/**
	 * 	Get the stored value associated with the given key
	 * 	in this profile.
	 * 	No traversal of the ancestor hierarchy is done
	 * 	for sub-profiles.
	 */
	public String getLocalValue(String key) {
		return storage.getProperty(key);
	}

	/**
	 * 	Associate the given value with the given key
	 * 	in the local storage of this profile.
	 */
	public void setValue(String key, String value) {
		storage.setProperty(key, value);
		changed = true;
	}

	public Dictionary getSelectionContext() {
		Hashtable result = new Hashtable(storage);
		String environments = getValue(PROP_ENVIRONMENTS);
		if (environments == null)
			return result;
		for (StringTokenizer tokenizer = new StringTokenizer(environments, ","); tokenizer.hasMoreElements();) { //$NON-NLS-1$
			String entry = tokenizer.nextToken();
			int i = entry.indexOf('=');
			String key = entry.substring(0, i).trim();
			String value = entry.substring(i + 1).trim();
			result.put(key, value);
		}
		return result;
	}

	private IInstallableUnit[] getAllInstallableUnits() {
		return (IInstallableUnit[]) iuProperties.keySet().toArray(new IInstallableUnit[iuProperties.size()]);
	}

	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return query.perform(Arrays.asList(getAllInstallableUnits()).iterator(), collector);
	}

	public Iterator getInstallableUnits() {
		// NOTE: this is a copy
		return Arrays.asList(getAllInstallableUnits()).iterator();
	}

	public String getInstallableUnitProfileProperty(IInstallableUnit iu, String key) {
		OrderedProperties properties = (OrderedProperties) iuProperties.get(iu);
		if (properties == null)
			return null;

		return properties.getProperty(key);
	}

	public String setInstallableUnitProfileProperty(IInstallableUnit iu, String key, String value) {
		OrderedProperties properties = (OrderedProperties) iuProperties.get(iu);
		if (properties == null) {
			properties = new OrderedProperties();
			iuProperties.put(iu, properties);
		}

		changed = true;
		return (String) properties.setProperty(key, value);
		// TODO this is not the ideal place for this.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=206077
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=197701
		//		ProvisioningEventBus bus = (ProvisioningEventBus) ServiceHelper.getService(EngineActivator.getContext(), ProvisioningEventBus.class.getName());
		//		if (bus != null)
		//			bus.publishEvent(new ProfileEvent(this, ProfileEvent.CHANGED));
	}

	/**
	 * Get an <i>unmodifiable copy</i> of the local properties
	 * associated with the profile.
	 * 
	 * @return an <i>unmodifiable copy</i> of the Profile properties.
	 */
	public Map getProperties() {
		return OrderedProperties.unmodifiableProperties(storage);
	}

	/**
	 * 	Add all the properties in the map to the local properties
	 * 	of the profile.
	 */
	public void addProperties(Map properties) {
		storage.putAll(properties);
		changed = true;
	}

	public void addInstallableUnit(IInstallableUnit iu) {
		if (iuProperties.containsKey(iu))
			return;

		iuProperties.put(iu, new OrderedProperties());
		changed = true;
	}

	public void removeInstallableUnit(IInstallableUnit iu) {
		if (iuProperties.remove(iu) != null)
			changed = true;
	}

	public OrderedProperties getInstallableUnitProfileProperties(IInstallableUnit iu) {
		return (OrderedProperties) iuProperties.get(iu);
	}

	public boolean isChanged() {
		return changed;
	}
}
