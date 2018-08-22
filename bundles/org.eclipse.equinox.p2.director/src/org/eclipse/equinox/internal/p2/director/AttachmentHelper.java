/*******************************************************************************
 *  Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.QueryUtil;

public class AttachmentHelper {
	private static final IInstallableUnitFragment[] NO_FRAGMENTS = new IInstallableUnitFragment[0];

	public static Collection<IInstallableUnit> attachFragments(Iterator<IInstallableUnit> toAttach, Map<IInstallableUnitFragment, List<IInstallableUnit>> fragmentsToIUs) {
		Map<IInstallableUnit, IInstallableUnitFragment[]> fragmentBindings = new HashMap<>();
		//Build a map inverse of the one provided in input (host --> List of fragments)
		Map<IInstallableUnit, List<IInstallableUnitFragment>> iusToFragment = new HashMap<>(fragmentsToIUs.size());
		for (Map.Entry<IInstallableUnitFragment, List<IInstallableUnit>> mapping : fragmentsToIUs.entrySet()) {
			IInstallableUnitFragment fragment = mapping.getKey();
			List<IInstallableUnit> existingMatches = mapping.getValue();

			for (IInstallableUnit host : existingMatches) {
				List<IInstallableUnitFragment> potentialFragments = iusToFragment.get(host);
				if (potentialFragments == null) {
					potentialFragments = new ArrayList<>();
					iusToFragment.put(host, potentialFragments);
				}
				potentialFragments.add(fragment);
			}
		}

		for (Map.Entry<IInstallableUnit, List<IInstallableUnitFragment>> entry : iusToFragment.entrySet()) {
			IInstallableUnit hostIU = entry.getKey();
			List<IInstallableUnitFragment> potentialIUFragments = entry.getValue();
			ArrayList<IInstallableUnitFragment> applicableFragments = new ArrayList<>();
			for (IInstallableUnitFragment potentialFragment : potentialIUFragments) {
				if (hostIU.equals(potentialFragment))
					continue;

				// Check to make sure the host meets the requirements of the fragment
				Collection<IRequirement> reqsFromFragment = potentialFragment.getHost();
				boolean match = true;
				boolean requirementMatched = false;
				for (Iterator<IRequirement> iterator = reqsFromFragment.iterator(); iterator.hasNext() && match == true;) {
					IRequirement reqs = iterator.next();
					requirementMatched = false;
					if (hostIU.satisfies(reqs))
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
			LinkedList<IInstallableUnitFragment> fragments = new LinkedList<>();
			for (IInstallableUnitFragment fragment : applicableFragments) {
				if (isTranslation(fragment)) {
					fragments.add(fragment);
					continue;
				}
				if (fragment.getHost().size() > specificityLevel) {
					theFragment = fragment;
					specificityLevel = fragment.getHost().size();
				}
			}
			if (theFragment != null)
				fragments.addFirst(theFragment);
			if (!fragments.isEmpty())
				fragmentBindings.put(hostIU, fragments.toArray(new IInstallableUnitFragment[fragments.size()]));
		}
		//build the collection of resolved IUs
		Collection<IInstallableUnit> result = new HashSet<>();
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
			IInstallableUnitFragment[] fragments = fragmentBindings.get(iu);
			if (fragments == null)
				fragments = NO_FRAGMENTS;
			result.add(MetadataFactory.createResolvedInstallableUnit(iu, fragments));
		}
		return result;
	}

	private static boolean isTranslation(IInstallableUnitFragment fragment) {
		for (IProvidedCapability capability : fragment.getProvidedCapabilities()) {
			// TODO make the constant in the TranslationSupport class public and use it
			if ("org.eclipse.equinox.p2.localization".equals(capability.getNamespace())) //$NON-NLS-1$
				return true;
		}
		return false;
	}
}
