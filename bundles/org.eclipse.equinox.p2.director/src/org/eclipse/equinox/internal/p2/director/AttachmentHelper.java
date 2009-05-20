/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public class AttachmentHelper {
	private static final IInstallableUnitFragment[] NO_FRAGMENTS = new IInstallableUnitFragment[0];

	public static Collection attachFragments(Collection toAttach, Map fragmentsToIUs) {
		Map fragmentBindings = new HashMap();
		//Build a map inverse of the one provided in input (host --> List of fragments)
		Map iusToFragment = new HashMap(fragmentsToIUs.size());
		for (Iterator iterator = fragmentsToIUs.entrySet().iterator(); iterator.hasNext();) {
			Entry mapping = (Entry) iterator.next();
			IInstallableUnitFragment fragment = (IInstallableUnitFragment) mapping.getKey();
			List existingMatches = (List) mapping.getValue();

			for (Iterator iterator2 = existingMatches.iterator(); iterator2.hasNext();) {
				Object host = iterator2.next();
				List potentialFragments = (List) iusToFragment.get(host);
				if (potentialFragments == null) {
					potentialFragments = new ArrayList();
					iusToFragment.put(host, potentialFragments);
				}
				potentialFragments.add(fragment);
			}
		}

		for (Iterator iterator = iusToFragment.entrySet().iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			IInstallableUnit hostIU = (IInstallableUnit) entry.getKey();
			List potentialIUFragments = (List) entry.getValue();
			ArrayList applicableFragments = new ArrayList();
			for (Iterator iterator2 = potentialIUFragments.iterator(); iterator2.hasNext();) {
				IInstallableUnit dependentIU = (IInstallableUnitFragment) iterator2.next();
				if (hostIU.equals(dependentIU) || !dependentIU.isFragment())
					continue;

				IInstallableUnitFragment potentialFragment = (IInstallableUnitFragment) dependentIU;

				// Check to make sure the host meets the requirements of the fragment
				IRequiredCapability reqsFromFragment[] = potentialFragment.getHost();
				boolean match = true;
				boolean requirementMatched = false;
				for (int l = 0; l < reqsFromFragment.length && match == true; l++) {
					requirementMatched = false;
					if (hostIU.satisfies(reqsFromFragment[l]))
						requirementMatched = true;
					if (requirementMatched == false) {
						match = false;
						break;
					}

				}
				if (match) {
					applicableFragments.add(potentialFragment);
				}
			}

			IInstallableUnitFragment theFragment = null;
			int specificityLevel = 0;
			for (Iterator iterator4 = applicableFragments.iterator(); iterator4.hasNext();) {
				IInstallableUnitFragment fragment = (IInstallableUnitFragment) iterator4.next();
				if (fragment.getHost().length > specificityLevel) {
					theFragment = fragment;
					specificityLevel = fragment.getHost().length;
				}
			}
			if (theFragment != null)
				fragmentBindings.put(hostIU, theFragment);
		}
		//build the collection of resolved IUs
		Collection result = new HashSet(toAttach.size());
		for (Iterator iterator = toAttach.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			if (iu == null)
				continue;
			//just return fragments as they are
			if (iu.isFragment()) {
				result.add(iu);
				continue;
			}
			//return a new IU that combines the IU with its bound fragments
			IInstallableUnitFragment fragment = (IInstallableUnitFragment) fragmentBindings.get(iu);
			IInstallableUnitFragment[] fragments;
			if (fragment == null)
				fragments = NO_FRAGMENTS;
			else
				fragments = new IInstallableUnitFragment[] {fragment};
			result.add(MetadataFactory.createResolvedInstallableUnit(iu, fragments));
		}
		return result;
	}

}
