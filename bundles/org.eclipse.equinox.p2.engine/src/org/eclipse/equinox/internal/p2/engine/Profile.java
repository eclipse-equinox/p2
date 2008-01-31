/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.installregistry.IInstallRegistry;
import org.eclipse.equinox.internal.p2.installregistry.IProfileInstallRegistry;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.util.NLS;

public class Profile implements IQueryable, IProfile {

	//Internal id of the profile
	private String profileId;

	private Profile parentProfile;

	/**
	 * 	A collection of child profiles.
	 */
	private List subProfileIds = null; // child profile ids

	private static String[] noSubProfiles = new String[0];
	/**
	 * This storage is to be used by the touchpoints to store data.
	 */
	private OrderedProperties storage = new OrderedProperties();

	/**
	 * iuProperties are stored by the install registry
	 */
	private Map iuProperties = new HashMap();
	private boolean changed = false;

	public Profile(String profileId, Profile parent, Map properties) {
		if (profileId == null || profileId.length() == 0) {
			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Null_Profile_Id, null));
		}
		this.profileId = profileId;
		this.parentProfile = parent;
		if (parent != null) {
			parent.addSubProfile(profileId);
		}
		if (properties != null)
			storage.putAll(properties);

		populateIUs();
	}

	private void populateIUs() {
		IInstallRegistry installRegistry = (IInstallRegistry) ServiceHelper.getService(EngineActivator.getContext(), IInstallRegistry.class.getName());
		if (installRegistry == null)
			return;
		IProfileInstallRegistry profileInstallRegistry = installRegistry.getProfileInstallRegistry(getProfileId());
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#getProfileId()
	 */
	public String getProfileId() {
		return profileId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#getParentProfile()
	 */
	public IProfile getParentProfile() {
		return parentProfile;
	}

	/*
	 * 	A profile is a root profile if it is not a sub-profile
	 * 	of another profile.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#isRootProfile()
	 */
	public boolean isRootProfile() {
		return parentProfile == null;
	}

	public void addSubProfile(String subProfileId) throws IllegalArgumentException {
		if (subProfileIds == null)
			subProfileIds = new ArrayList();

		if (!subProfileIds.contains(subProfileId))
			subProfileIds.add(subProfileId);

		//		if (!subProfileIds.add(subProfileId))
		//			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Duplicate_Child_Profile_Id, new String[] {subProfileId, this.getProfileId()}));
	}

	public void removeSubProfile(String subProfileId) throws IllegalArgumentException {
		if (subProfileIds != null)
			subProfileIds.remove(subProfileId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#hasSubProfiles()
	 */
	public boolean hasSubProfiles() {
		return subProfileIds == null || subProfileIds.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#getSubProfileIds()
	 */
	public String[] getSubProfileIds() {
		if (subProfileIds == null)
			return noSubProfiles;

		return (String[]) subProfileIds.toArray(new String[subProfileIds.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		String value = storage.getProperty(key);
		if (value == null && parentProfile != null) {
			value = parentProfile.getProperty(key);
		}
		return value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#getLocalProperty(java.lang.String)
	 */
	public String getLocalProperty(String key) {
		return storage.getProperty(key);
	}

	/**
	 * 	Associate the given value with the given key
	 * 	in the local storage of this profile.
	 */
	public void setProperty(String key, String value) {
		storage.setProperty(key, value);
		changed = true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#query(org.eclipse.equinox.p2.query.Query, org.eclipse.equinox.p2.query.Collector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return query.perform(iuProperties.keySet().iterator(), collector);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#getInstallableUnitProperty(org.eclipse.equinox.p2.metadata.IInstallableUnit, java.lang.String)
	 */
	public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
		OrderedProperties properties = (OrderedProperties) iuProperties.get(iu);
		if (properties == null)
			return null;

		return properties.getProperty(key);
	}

	public String setInstallableUnitProperty(IInstallableUnit iu, String key, String value) {
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#getLocalProperties()
	 */
	public Map getLocalProperties() {
		return OrderedProperties.unmodifiableProperties(storage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#getProperties()
	 */
	public Map getProperties() {
		if (parentProfile == null)
			return getLocalProperties();

		Map properties = new HashMap(parentProfile.getProperties());
		properties.putAll(storage);
		return OrderedProperties.unmodifiableProperties(properties);
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.engine.IProfile#getInstallableUnitProperties(org.eclipse.equinox.p2.metadata.IInstallableUnit)
	 */
	public OrderedProperties getInstallableUnitProperties(IInstallableUnit iu) {
		return (OrderedProperties) iuProperties.get(iu);
	}

	public void clearLocalProperties() {
		storage.clear();
	}

	public boolean isChanged() {
		return changed;
	}
}
