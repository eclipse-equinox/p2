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

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class FormerState {

	public static ProfileChangeRequest generateProfileDeltaChangeRequest(IProfile current, IProfile target) {
		ProfileChangeRequest request = new ProfileChangeRequest(current);

		synchronizeProfileProperties(request, current, target);
		synchronizeMarkedIUs(request, current, target);
		synchronizeAllIUProperties(request, current, target);

		return request;
	}

	private static void synchronizeAllIUProperties(ProfileChangeRequest request, IProfile current, IProfile target) {
		Set<IInstallableUnit> currentIUset = current.query(InstallableUnitQuery.ANY, null).unmodifiableSet();
		Iterator<IInstallableUnit> targetIUs = target.query(InstallableUnitQuery.ANY, null).iterator();
		List<IInstallableUnit> iusToAdd = new ArrayList<IInstallableUnit>();
		List<IInstallableUnit> iusToUpdate = new ArrayList<IInstallableUnit>();
		while (targetIUs.hasNext()) {
			IInstallableUnit nxt = targetIUs.next();
			if (currentIUset.contains(nxt))
				iusToUpdate.add(nxt);
			else
				iusToAdd.add(nxt);
		}

		//additions
		for (Iterator<IInstallableUnit> iterator = iusToAdd.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			for (Iterator<Entry<String, String>> it = target.getInstallableUnitProperties(iu).entrySet().iterator(); it.hasNext();) {
				Entry<String, String> entry = it.next();
				request.setInstallableUnitProfileProperty(iu, entry.getKey(), entry.getValue());
			}
		}

		// updates
		for (Iterator<IInstallableUnit> iterator = iusToUpdate.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			Map<String, String> propertiesToSet = new HashMap<String, String>(target.getInstallableUnitProperties(iu));
			for (Iterator<Entry<String, String>> it = current.getInstallableUnitProperties(iu).entrySet().iterator(); it.hasNext();) {
				Entry<String, String> entry = it.next();
				String key = entry.getKey();
				String newValue = propertiesToSet.get(key);
				if (newValue == null) {
					request.removeInstallableUnitProfileProperty(iu, key);
				} else if (newValue.equals(entry.getValue()))
					propertiesToSet.remove(key);
			}

			for (Iterator<Entry<String, String>> it = propertiesToSet.entrySet().iterator(); it.hasNext();) {
				Entry<String, String> entry = it.next();
				request.setInstallableUnitProfileProperty(iu, entry.getKey(), entry.getValue());
			}
		}
	}

	private static void synchronizeMarkedIUs(ProfileChangeRequest request, IProfile current, IProfile target) {
		IInstallableUnit[] currentPlannerMarkedIUs = SimplePlanner.findPlannerMarkedIUs(current);
		IInstallableUnit[] targetPlannerMarkedIUs = SimplePlanner.findPlannerMarkedIUs(target);

		//additions
		List<IInstallableUnit> markedIUsToAdd = new ArrayList<IInstallableUnit>(Arrays.asList(targetPlannerMarkedIUs));
		markedIUsToAdd.removeAll(Arrays.asList(currentPlannerMarkedIUs));
		request.addInstallableUnits(markedIUsToAdd);

		// removes
		List<IInstallableUnit> markedIUsToRemove = new ArrayList<IInstallableUnit>(Arrays.asList(currentPlannerMarkedIUs));
		markedIUsToRemove.removeAll(Arrays.asList(targetPlannerMarkedIUs));
		request.removeInstallableUnits(markedIUsToRemove);
	}

	private static void synchronizeProfileProperties(ProfileChangeRequest request, IProfile current, IProfile target) {
		Map<String, String> profilePropertiesToSet = new HashMap<String, String>(target.getProperties());
		for (Iterator<Entry<String, String>> it = current.getProperties().entrySet().iterator(); it.hasNext();) {
			Entry<String, String> entry = it.next();
			String key = entry.getKey();

			String newValue = profilePropertiesToSet.get(key);
			if (newValue == null) {
				request.removeProfileProperty(key);
			} else if (newValue.equals(entry.getValue()))
				profilePropertiesToSet.remove(key);
		}

		for (Iterator<Entry<String, String>> it = profilePropertiesToSet.entrySet().iterator(); it.hasNext();) {
			Entry<String, String> entry = it.next();
			request.setProfileProperty(entry.getKey(), entry.getValue());
		}
	}
}
