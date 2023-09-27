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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.QueryUtil;

public class AttachmentHelper {
	private static final IInstallableUnitFragment[] NO_FRAGMENTS = new IInstallableUnitFragment[0];

	public static Collection<IInstallableUnit> attachFragments(Stream<IInstallableUnit> toAttach,
			Map<IInstallableUnitFragment, List<IInstallableUnit>> fragmentsToIUs) {
		Map<IInstallableUnit, IInstallableUnitFragment[]> fragmentBindings = new HashMap<>();
		//Build a map inverse of the one provided in input (host --> List of fragments)
		Map<IInstallableUnit, List<IInstallableUnitFragment>> iusToFragment = new HashMap<>(fragmentsToIUs.size());
		fragmentsToIUs.forEach((fragment, existingMatches) -> {
			for (IInstallableUnit host : existingMatches) {
				iusToFragment.computeIfAbsent(host, h -> new ArrayList<>()).add(fragment);
			}
		});

		iusToFragment.forEach((hostIU, potentialIUFragments) -> {
			List<IInstallableUnitFragment> applicableFragments = new ArrayList<>();
			for (IInstallableUnitFragment potentialFragment : potentialIUFragments) {
				if (hostIU.equals(potentialFragment)) {
					continue;
				}
				// Check to make sure the host meets the requirements of the fragment
				Collection<IRequirement> reqsFromFragment = potentialFragment.getHost();
				if (reqsFromFragment.stream().allMatch(hostIU::satisfies)) {
					applicableFragments.add(potentialFragment);
				}
			}

			IInstallableUnitFragment theFragment = null;
			int specificityLevel = 0;
			Deque<IInstallableUnitFragment> fragments = new LinkedList<>();
			for (IInstallableUnitFragment fragment : applicableFragments) {
				if (isTranslation(fragment)) {
					fragments.addLast(fragment);
					continue;
				}
				if (fragment.getHost().size() > specificityLevel) {
					theFragment = fragment;
					specificityLevel = fragment.getHost().size();
				}
			}
			if (theFragment != null) {
				fragments.addFirst(theFragment);
			}
			if (!fragments.isEmpty()) {
				fragmentBindings.put(hostIU, fragments.toArray(IInstallableUnitFragment[]::new));
			}
		});
		//build the collection of resolved IUs
		return toAttach.filter(Objects::nonNull).map(iu -> {
			//just return fragments as they are
			if (QueryUtil.isFragment(iu)) {
				return iu;
			}
			//return a new IU that combines the IU with its bound fragments
			IInstallableUnitFragment[] fragments = fragmentBindings.getOrDefault(iu, NO_FRAGMENTS);
			return MetadataFactory.createResolvedInstallableUnit(iu, fragments);
		}).collect(Collectors.toCollection(HashSet::new));
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
