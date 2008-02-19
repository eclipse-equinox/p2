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
import java.util.Map.Entry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.osgi.util.NLS;

public class Profile implements IQueryable, IProfile {

	//Internal id of the profile
	private final String profileId;

	private Profile parentProfile;

	/**
	 * 	A collection of child profiles.
	 */
	private List subProfileIds; // child profile ids

	private static final String[] noSubProfiles = new String[0];
	/**
	 * This storage is to be used by the touchpoints to store data.
	 */
	private OrderedProperties storage = new OrderedProperties();

	private Set ius = new HashSet();
	private Map iuProperties = new HashMap();
	private boolean changed = false;

	public Profile(String profileId, Profile parent, Map properties) {
		if (profileId == null || profileId.length() == 0) {
			throw new IllegalArgumentException(NLS.bind(Messages.Profile_Null_Profile_Id, null));
		}
		this.profileId = profileId;
		setParent(parent);
		if (properties != null)
			storage.putAll(properties);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getProfileId()
	 */
	public String getProfileId() {
		return profileId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getParentProfile()
	 */
	public IProfile getParentProfile() {
		return parentProfile;
	}

	public void setParent(Profile profile) {
		if (profile == parentProfile)
			return;

		if (parentProfile != null)
			parentProfile.removeSubProfile(profileId);

		parentProfile = profile;
		if (parentProfile != null)
			parentProfile.addSubProfile(profileId);
	}

	/*
	 * 	A profile is a root profile if it is not a sub-profile
	 * 	of another profile.
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#isRootProfile()
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
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#hasSubProfiles()
	 */
	public boolean hasSubProfiles() {
		return subProfileIds == null || subProfileIds.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getSubProfileIds()
	 */
	public String[] getSubProfileIds() {
		if (subProfileIds == null)
			return noSubProfiles;

		return (String[]) subProfileIds.toArray(new String[subProfileIds.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		String value = storage.getProperty(key);
		if (value == null && parentProfile != null) {
			value = parentProfile.getProperty(key);
		}
		return value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getLocalProperty(java.lang.String)
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
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#query(org.eclipse.equinox.internal.provisional.p2.query.Query, org.eclipse.equinox.internal.provisional.p2.query.Collector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
		return query.perform(ius.iterator(), collector);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getInstallableUnitProperty(org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit, java.lang.String)
	 */
	public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
		OrderedProperties properties = (OrderedProperties) iuProperties.get(createIUKey(iu));
		if (properties == null)
			return null;

		return properties.getProperty(key);
	}

	public String setInstallableUnitProperty(IInstallableUnit iu, String key, String value) {
		String iuKey = createIUKey(iu);
		OrderedProperties properties = (OrderedProperties) iuProperties.get(iuKey);
		if (properties == null) {
			properties = new OrderedProperties();
			iuProperties.put(iuKey, properties);
		}

		changed = true;
		return (String) properties.setProperty(key, value);
	}

	private static String createIUKey(IInstallableUnit iu) {
		return iu.getId() + "_" + iu.getVersion().toString(); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getLocalProperties()
	 */
	public Map getLocalProperties() {
		return OrderedProperties.unmodifiableProperties(storage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getProperties()
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
		if (ius.contains(iu))
			return;

		ius.add(iu);
		changed = true;
	}

	public void removeInstallableUnit(IInstallableUnit iu) {
		if (ius.remove(iu) == true) {
			iuProperties.remove(createIUKey(iu));
			changed = true;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.engine.IProfile#getInstallableUnitProperties(org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit)
	 */
	public Map getInstallableUnitProperties(IInstallableUnit iu) {
		OrderedProperties properties = (OrderedProperties) iuProperties.get(createIUKey(iu));
		if (properties == null)
			properties = new OrderedProperties();

		return OrderedProperties.unmodifiableProperties(properties);
	}

	public void clearLocalProperties() {
		storage.clear();
		changed = true;
	}

	public boolean isChanged() {
		return changed;
	}

	public void clearInstallableUnits() {
		ius.clear();
		iuProperties.clear();
		changed = true;
	}

	public Profile snapshot() {
		Profile parentSnapshot = null;
		if (parentProfile != null)
			parentSnapshot = parentProfile.snapshot();

		Profile snapshot = new Profile(profileId, parentSnapshot, storage);

		if (subProfileIds != null) {
			for (Iterator it = subProfileIds.iterator(); it.hasNext();) {
				String subProfileId = (String) it.next();
				snapshot.addSubProfile(subProfileId);
			}
		}

		for (Iterator it = ius.iterator(); it.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) it.next();
			snapshot.addInstallableUnit(iu);
			Map properties = getInstallableUnitProperties(iu);
			if (properties != null)
				snapshot.addInstallableUnitProperties(iu, properties);
		}
		return snapshot;
	}

	public void addInstallableUnitProperties(IInstallableUnit iu, Map properties) {
		for (Iterator it = properties.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			setInstallableUnitProperty(iu, key, value);
		}
	}

}
