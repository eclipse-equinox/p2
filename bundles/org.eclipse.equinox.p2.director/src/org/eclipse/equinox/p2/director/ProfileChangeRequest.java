/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.director;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class ProfileChangeRequest {

	private final Profile profile;
	private ArrayList iusToRemove = null; // list of ius to remove
	private ArrayList iusToAdd = null; // list of ius to add
	private ArrayList propertiesToRemove = null; // list of keys for properties to be removed
	private HashMap propertiesToAdd = null; // map of key->value for properties to be added
	private Map iuPropertiesToAdd = null; // map iu->map of key->value pairs for properties to be added for an iu
	private Map iuPropertiesToRemove = null; // map of iu->list of property keys to be removed for an iu

	public ProfileChangeRequest(String profileId) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(DirectorActivator.context, IProfileRegistry.class.getName());
		if (profileRegistry == null)
			throw new IllegalStateException("ProfileRegistry unavailable");
		profile = profileRegistry.getProfile(profileId);
		if (profile == null)
			throw new IllegalArgumentException("Profile id " + profileId + " is not registered.");
	}

	public Profile getProfile() {
		return profile;
	}

	public Map getProfileProperties() {
		Map result = new HashMap(profile.getProperties());
		if (propertiesToRemove != null) {
			for (Iterator it = propertiesToRemove.iterator(); it.hasNext();) {
				result.remove(it.next());
			}
		}
		if (propertiesToAdd != null)
			result.putAll(propertiesToAdd);

		return result;
	}

	public String getProfileProperty(String key) {
		return (String) getProfileProperties().get(key);
	}

	public void addInstallableUnits(IInstallableUnit[] toInstall) {
		if (iusToAdd == null)
			iusToAdd = new ArrayList(toInstall.length);
		for (int i = 0; i < toInstall.length; i++)
			iusToAdd.add(toInstall[i]);
	}

	public void removeInstallableUnits(IInstallableUnit[] toUninstall) {
		if (iusToRemove == null)
			iusToRemove = new ArrayList(toUninstall.length);
		for (int i = 0; i < toUninstall.length; i++)
			iusToRemove.add(toUninstall[i]);
	}

	public void setProfileProperty(String key, Object value) {
		if (propertiesToAdd == null)
			propertiesToAdd = new HashMap();
		propertiesToAdd.put(key, value);
	}

	public void removeProfileProperty(String key) {
		if (propertiesToRemove == null)
			propertiesToRemove = new ArrayList(1);
		propertiesToRemove.add(key);
	}

	public void setInstallableUnitProfileProperty(IInstallableUnit iu, String key, Object value) {
		if (iuPropertiesToAdd == null)
			iuPropertiesToAdd = new HashMap();
		Map properties = (Map) iuPropertiesToAdd.get(iu);
		if (properties == null) {
			properties = new HashMap();
			iuPropertiesToAdd.put(iu, properties);
		}
		properties.put(key, value);
	}

	public void removeInstallableUnitProfileProperty(IInstallableUnit iu, String key) {
		if (iuPropertiesToRemove == null)
			iuPropertiesToRemove = new HashMap();
		List keys = (List) iuPropertiesToRemove.get(iu);
		if (keys == null) {
			keys = new ArrayList();
			iuPropertiesToRemove.put(iu, keys);
		}
		keys.add(key);
	}

	public IInstallableUnit[] getRemovedInstallableUnits() {
		if (iusToRemove == null)
			return new IInstallableUnit[0];
		return (IInstallableUnit[]) iusToRemove.toArray(new IInstallableUnit[iusToRemove.size()]);
	}

	public IInstallableUnit[] getAddedInstallableUnits() {
		if (iusToAdd == null)
			return new IInstallableUnit[0];
		return (IInstallableUnit[]) iusToAdd.toArray(new IInstallableUnit[iusToAdd.size()]);
	}

	// String [key, key, key] names of properties to remove
	public String[] getPropertiesToRemove() {
		if (propertiesToRemove == null)
			return new String[0];
		return (String[]) propertiesToRemove.toArray(new String[propertiesToRemove.size()]);
	}

	// map of key value pairs
	public Map getPropertiesToAdd() {
		if (propertiesToAdd == null)
			return new HashMap();
		return propertiesToAdd;
	}

	// map of iu->list of property keys to be removed for an iu	
	public Map getInstallableUnitProfilePropertiesToRemove() {
		if (iuPropertiesToRemove == null)
			return new HashMap();
		return iuPropertiesToRemove;
	}

	// TODO This can be represented and returned in whatever way makes most sense for planner/engine
	// map iu->map of key->value pairs for properties to be added for an iu
	public Map getInstallableUnitProfilePropertiesToAdd() {
		if (iuPropertiesToAdd == null)
			return new HashMap();
		return iuPropertiesToAdd;
	}

}
