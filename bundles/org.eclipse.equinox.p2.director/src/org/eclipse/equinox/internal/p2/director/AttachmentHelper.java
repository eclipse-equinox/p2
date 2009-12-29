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
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.query.FragmentQuery;

public class AttachmentHelper {
	private static final IInstallableUnitFragment[] NO_FRAGMENTS = new IInstallableUnitFragment[0];

	public static Collection<IInstallableUnit> attachFragments(Iterator<IInstallableUnit> toAttach, Map<IInstallableUnitFragment, List<IInstallableUnit>> fragmentsToIUs) {
		Map<IInstallableUnit, IInstallableUnitFragment> fragmentBindings = new HashMap<IInstallableUnit, IInstallableUnitFragment>();
		//Build a map inverse of the one provided in input (host --> List of fragments)
		Map<IInstallableUnit, List<IInstallableUnitFragment>> iusToFragment = new HashMap<IInstallableUnit, List<IInstallableUnitFragment>>(fragmentsToIUs.size());
		for (Iterator<Entry<IInstallableUnitFragment, List<IInstallableUnit>>> iterator = fragmentsToIUs.entrySet().iterator(); iterator.hasNext();) {
			Entry<IInstallableUnitFragment, List<IInstallableUnit>> mapping = iterator.next();
			IInstallableUnitFragment fragment = mapping.getKey();
			List<IInstallableUnit> existingMatches = mapping.getValue();

			for (Iterator<IInstallableUnit> iterator2 = existingMatches.iterator(); iterator2.hasNext();) {
				IInstallableUnit host = iterator2.next();
				List<IInstallableUnitFragment> potentialFragments = iusToFragment.get(host);
				if (potentialFragments == null) {
					potentialFragments = new ArrayList<IInstallableUnitFragment>();
					iusToFragment.put(host, potentialFragments);
				}
				potentialFragments.add(fragment);
			}
		}

		for (Iterator<Entry<IInstallableUnit, List<IInstallableUnitFragment>>> iterator = iusToFragment.entrySet().iterator(); iterator.hasNext();) {
			Entry<IInstallableUnit, List<IInstallableUnitFragment>> entry = iterator.next();
			IInstallableUnit hostIU = entry.getKey();
			List<IInstallableUnitFragment> potentialIUFragments = entry.getValue();
			ArrayList<IInstallableUnitFragment> applicableFragments = new ArrayList<IInstallableUnitFragment>();
			for (Iterator<IInstallableUnitFragment> iterator2 = potentialIUFragments.iterator(); iterator2.hasNext();) {
				IInstallableUnitFragment potentialFragment = iterator2.next();
				if (hostIU.equals(potentialFragment))
					continue;

				// Check to make sure the host meets the requirements of the fragment
				IRequirement reqsFromFragment[] = potentialFragment.getHost();
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
			for (Iterator<IInstallableUnitFragment> iterator4 = applicableFragments.iterator(); iterator4.hasNext();) {
				IInstallableUnitFragment fragment = iterator4.next();
				if (fragment.getHost().length > specificityLevel) {
					theFragment = fragment;
					specificityLevel = fragment.getHost().length;
				}
			}
			if (theFragment != null)
				fragmentBindings.put(hostIU, theFragment);
		}
		//build the collection of resolved IUs
		Collection<IInstallableUnit> result = new HashSet<IInstallableUnit>();
		while (toAttach.hasNext()) {
			IInstallableUnit iu = toAttach.next();
			if (iu == null)
				continue;
			//just return fragments as they are
			if (FragmentQuery.isFragment(iu)) {
				result.add(iu);
				continue;
			}
			//return a new IU that combines the IU with its bound fragments
			IInstallableUnitFragment fragment = fragmentBindings.get(iu);
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
