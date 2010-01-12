/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.director;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;

public class ProfileChangeRequest implements Cloneable {

	private final IProfile profile;
	private ArrayList<IInstallableUnit> iusToRemove = null; // list of ius to remove
	private ArrayList<IInstallableUnit> iusToAdd = null; // list of ius to add
	private ArrayList<String> propertiesToRemove = null; // list of keys for properties to be removed
	private HashMap<String, String> propertiesToAdd = null; // map of key->value for properties to be added
	private HashMap<IInstallableUnit, Map<String, String>> iuPropertiesToAdd = null; // map iu->map of key->value pairs for properties to be added for an iu
	private HashMap<IInstallableUnit, List<String>> iuPropertiesToRemove = null; // map of iu->list of property keys to be removed for an iu
	private boolean isAbsolute = false; //Indicate whether or not the request is an absolute one

	public static ProfileChangeRequest createByProfileId(String profileId) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(DirectorActivator.context, IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			throw new IllegalStateException(Messages.Planner_no_profile_registry);
		IProfile profile = profileRegistry.getProfile(profileId);
		if (profile == null)
			throw new IllegalArgumentException("Profile id " + profileId + " is not registered."); //$NON-NLS-1$//$NON-NLS-2$
		return new ProfileChangeRequest(profile);
	}

	public ProfileChangeRequest(IProfile profile) {
		if (profile == null)
			throw new IllegalArgumentException("Profile cannot be null."); //$NON-NLS-1$
		this.profile = profile;
	}

	public IProfile getProfile() {
		return profile;
	}

	public Map<String, String> getProfileProperties() {
		Map<String, String> result = new HashMap<String, String>(profile.getProperties());
		if (propertiesToRemove != null) {
			for (String key : propertiesToRemove) {
				result.remove(key);
			}
		}
		if (propertiesToAdd != null)
			result.putAll(propertiesToAdd);

		return result;
	}

	private void addInstallableUnit(IInstallableUnit toInstall) {
		if (iusToAdd == null)
			iusToAdd = new ArrayList<IInstallableUnit>();
		iusToAdd.add(toInstall);
	}

	public void addInstallableUnits(Collection<IInstallableUnit> toInstall) {
		for (IInstallableUnit iu : toInstall)
			addInstallableUnit(iu);
	}

	public void addInstallableUnits(IQueryResult<IInstallableUnit> toInstall) {
		for (Iterator<IInstallableUnit> itor = toInstall.iterator(); itor.hasNext();)
			addInstallableUnit(itor.next());
	}

	public void addInstallableUnits(IInstallableUnit... toInstall) {
		for (int i = 0; i < toInstall.length; i++)
			addInstallableUnit(toInstall[i]);
	}

	public void removeInstallableUnit(IInstallableUnit toUninstall) {
		if (iusToRemove == null)
			iusToRemove = new ArrayList<IInstallableUnit>();
		iusToRemove.add(toUninstall);
	}

	public void removeInstallableUnits(IInstallableUnit[] toUninstall) {
		for (int i = 0; i < toUninstall.length; i++)
			removeInstallableUnit(toUninstall[i]);
	}

	public void removeInstallableUnits(Collection<IInstallableUnit> toUninstall) {
		for (IInstallableUnit iu : toUninstall)
			removeInstallableUnit(iu);
	}

	public void removeInstallableUnits(IQueryResult<IInstallableUnit> toUninstall) {
		for (Iterator<IInstallableUnit> itor = toUninstall.iterator(); itor.hasNext();)
			removeInstallableUnit(itor.next());
	}

	public void setProfileProperty(String key, String value) {
		if (propertiesToAdd == null)
			propertiesToAdd = new HashMap<String, String>();
		propertiesToAdd.put(key, value);
	}

	public void removeProfileProperty(String key) {
		if (propertiesToRemove == null)
			propertiesToRemove = new ArrayList<String>(1);
		propertiesToRemove.add(key);
	}

	public void setInstallableUnitProfileProperty(IInstallableUnit iu, String key, String value) {
		if (iuPropertiesToAdd == null)
			iuPropertiesToAdd = new HashMap<IInstallableUnit, Map<String, String>>();
		Map<String, String> properties = iuPropertiesToAdd.get(iu);
		if (properties == null) {
			properties = new HashMap<String, String>();
			iuPropertiesToAdd.put(iu, properties);
		}
		properties.put(key, value);
	}

	public void removeInstallableUnitProfileProperty(IInstallableUnit iu, String key) {
		if (iuPropertiesToRemove == null)
			iuPropertiesToRemove = new HashMap<IInstallableUnit, List<String>>();
		List<String> keys = iuPropertiesToRemove.get(iu);
		if (keys == null) {
			keys = new ArrayList<String>();
			iuPropertiesToRemove.put(iu, keys);
		}
		keys.add(key);
	}

	public IInstallableUnit[] getRemovedInstallableUnits() {
		if (iusToRemove == null)
			return new IInstallableUnit[0];
		return iusToRemove.toArray(new IInstallableUnit[iusToRemove.size()]);
	}

	public IInstallableUnit[] getAddedInstallableUnits() {
		if (iusToAdd == null)
			return new IInstallableUnit[0];
		return iusToAdd.toArray(new IInstallableUnit[iusToAdd.size()]);
	}

	// String [key, key, key] names of properties to remove
	public String[] getPropertiesToRemove() {
		if (propertiesToRemove == null)
			return new String[0];
		return propertiesToRemove.toArray(new String[propertiesToRemove.size()]);
	}

	// map of key value pairs
	public Map<String, String> getPropertiesToAdd() {
		if (propertiesToAdd == null)
			return CollectionUtils.emptyMap();
		return propertiesToAdd;
	}

	// map of iu->list of property keys to be removed for an iu	
	public Map<IInstallableUnit, List<String>> getInstallableUnitProfilePropertiesToRemove() {
		if (iuPropertiesToRemove == null)
			return CollectionUtils.emptyMap();
		return iuPropertiesToRemove;
	}

	// TODO This can be represented and returned in whatever way makes most sense for planner/engine
	// map iu->map of key->value pairs for properties to be added for an iu
	public Map<IInstallableUnit, Map<String, String>> getInstallableUnitProfilePropertiesToAdd() {
		if (iuPropertiesToAdd == null)
			return CollectionUtils.emptyMap();
		return iuPropertiesToAdd;
	}

	public void setInstallableUnitInclusionRules(IInstallableUnit iu, String value) {
		setInstallableUnitProfileProperty(iu, SimplePlanner.INCLUSION_RULES, value);
	}

	public void removeInstallableUnitInclusionRules(IInstallableUnit iu) {
		removeInstallableUnitProfileProperty(iu, SimplePlanner.INCLUSION_RULES);
	}

	public void setAbsoluteMode(boolean absolute) {
		isAbsolute = absolute;
	}

	public boolean getAbsolute() {
		return isAbsolute;
	}

	@SuppressWarnings("unchecked")
	public Object clone() {
		ProfileChangeRequest result = new ProfileChangeRequest(profile);
		result.iusToRemove = iusToRemove == null ? null : (ArrayList<IInstallableUnit>) iusToRemove.clone();
		result.iusToAdd = iusToAdd == null ? null : (ArrayList<IInstallableUnit>) iusToAdd.clone();
		result.propertiesToRemove = propertiesToRemove == null ? null : (ArrayList<String>) propertiesToRemove.clone();
		result.propertiesToAdd = propertiesToAdd == null ? null : (HashMap<String, String>) propertiesToAdd.clone();
		result.iuPropertiesToAdd = iuPropertiesToAdd == null ? null : (HashMap<IInstallableUnit, Map<String, String>>) iuPropertiesToAdd.clone();
		result.iuPropertiesToRemove = iuPropertiesToRemove == null ? null : (HashMap<IInstallableUnit, List<String>>) iuPropertiesToRemove.clone();
		return result;
	}

	public String toString() {
		StringBuffer result = new StringBuffer(1000);
		result.append("==Profile change request for "); //$NON-NLS-1$
		result.append(profile.getProfileId());
		result.append('\n');
		result.append("==Additions=="); //$NON-NLS-1$
		result.append('\n');
		for (IInstallableUnit iu : iusToAdd) {
			result.append('\t');
			result.append(iu);
			result.append('\n');
		}
		result.append("==Removals=="); //$NON-NLS-1$
		result.append('\n');
		for (IInstallableUnit iu : iusToRemove) {
			result.append('\t');
			result.append(iu);
			result.append('\n');
		}
		return result.toString();
	}
}
