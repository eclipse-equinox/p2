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

import org.eclipse.equinox.p2.query.QueryUtil;

import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;

public class AttachmentHelper {
	private static final IInstallableUnitFragment[] NO_FRAGMENTS = new IInstallableUnitFragment[0];

	public static Collection<IInstallableUnit> attachFragments(Iterator<IInstallableUnit> toAttach, Map<IInstallableUnitFragment, List<IInstallableUnit>> fragmentsToIUs) {
		Map<IInstallableUnit, IInstallableUnitFragment> fragmentBindings = new HashMap<IInstallableUnit, IInstallableUnitFragment>();
		//Build a map inverse of the one provided in input (host --> List of fragments)
		Map<IInstallableUnit, List<IInstallableUnitFragment>> iusToFragment = new HashMap<IInstallableUnit, List<IInstallableUnitFragment>>(fragmentsToIUs.size());
		for (Entry<IInstallableUnitFragment, List<IInstallableUnit>> mapping : fragmentsToIUs.entrySet()) {
			IInstallableUnitFragment fragment = mapping.getKey();
			List<IInstallableUnit> existingMatches = mapping.getValue();

			for (IInstallableUnit host : existingMatches) {
				List<IInstallableUnitFragment> potentialFragments = iusToFragment.get(host);
				if (potentialFragments == null) {
					potentialFragments = new ArrayList<IInstallableUnitFragment>();
					iusToFragment.put(host, potentialFragments);
				}
				potentialFragments.add(fragment);
			}
		}

		for (Entry<IInstallableUnit, List<IInstallableUnitFragment>> entry : iusToFragment.entrySet()) {
			IInstallableUnit hostIU = entry.getKey();
			List<IInstallableUnitFragment> potentialIUFragments = entry.getValue();
			ArrayList<IInstallableUnitFragment> applicableFragments = new ArrayList<IInstallableUnitFragment>();
			for (IInstallableUnitFragment potentialFragment : potentialIUFragments) {
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
			for (IInstallableUnitFragment fragment : applicableFragments) {
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
			if (QueryUtil.isFragment(iu)) {
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
