/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.rollback;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;

public class FormerState {

	public static ProfileChangeRequest generateProfileDeltaChangeRequest(IProfile current, IProfile target) {
		ProfileChangeRequest request = new ProfileChangeRequest(current);

		synchronizeProfileProperties(request, current, target);
		synchronizeMarkedIUs(request, current, target);
		synchronizeAllIUProperties(request, current, target);

		return request;
	}

	private static void synchronizeAllIUProperties(ProfileChangeRequest request, IProfile current, IProfile target) {
		Collection currentIUs = current.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection();
		Collection targetIUs = target.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection();
		List iusToAdd = new ArrayList(targetIUs);
		iusToAdd.remove(currentIUs);

		//additions
		for (Iterator iterator = iusToAdd.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			for (Iterator it = target.getInstallableUnitProperties(iu).entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				request.setInstallableUnitProfileProperty(iu, key, value);
			}
		}

		// updates
		List iusToUpdate = new ArrayList(targetIUs);
		iusToUpdate.remove(iusToAdd);
		for (Iterator iterator = iusToUpdate.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			Map propertiesToSet = new HashMap(target.getInstallableUnitProperties(iu));
			for (Iterator it = current.getInstallableUnitProperties(iu).entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				String key = (String) entry.getKey();
				String newValue = (String) propertiesToSet.get(key);
				if (newValue == null) {
					request.removeInstallableUnitProfileProperty(iu, key);
				} else if (newValue.equals(entry.getValue()))
					propertiesToSet.remove(key);
			}

			for (Iterator it = propertiesToSet.entrySet().iterator(); it.hasNext();) {
				Entry entry = (Entry) it.next();
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				request.setInstallableUnitProfileProperty(iu, key, value);
			}
		}
	}

	private static void synchronizeMarkedIUs(ProfileChangeRequest request, IProfile current, IProfile target) {
		IInstallableUnit[] currentPlannerMarkedIUs = SimplePlanner.findPlannerMarkedIUs(current);
		IInstallableUnit[] targetPlannerMarkedIUs = SimplePlanner.findPlannerMarkedIUs(target);

		//additions
		List markedIUsToAdd = new ArrayList(Arrays.asList(targetPlannerMarkedIUs));
		markedIUsToAdd.removeAll(Arrays.asList(currentPlannerMarkedIUs));
		request.addInstallableUnits((IInstallableUnit[]) markedIUsToAdd.toArray(new IInstallableUnit[markedIUsToAdd.size()]));

		// removes
		List markedIUsToRemove = new ArrayList(Arrays.asList(currentPlannerMarkedIUs));
		markedIUsToRemove.removeAll(Arrays.asList(targetPlannerMarkedIUs));
		request.removeInstallableUnits((IInstallableUnit[]) markedIUsToRemove.toArray(new IInstallableUnit[markedIUsToRemove.size()]));
	}

	private static void synchronizeProfileProperties(ProfileChangeRequest request, IProfile current, IProfile target) {
		Map profilePropertiesToSet = new HashMap(target.getProperties());
		for (Iterator it = current.getProperties().entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String key = (String) entry.getKey();

			String newValue = (String) profilePropertiesToSet.get(key);
			if (newValue == null) {
				request.removeProfileProperty(key);
			} else if (newValue.equals(entry.getValue()))
				profilePropertiesToSet.remove(key);
		}

		for (Iterator it = profilePropertiesToSet.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			request.setProfileProperty(key, value);
		}
	}
}
