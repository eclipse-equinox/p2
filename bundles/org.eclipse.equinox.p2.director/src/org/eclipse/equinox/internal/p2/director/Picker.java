/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.util.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.service.resolver.VersionRange;

//The pickers goal is to find an installable unit that satisfies a search criteria 
//TODO we may need additional variations of these method where version can be null, or where the search returns all the match, not just the first one
//TODO do we want a facility to limit the searching space, or do we assume that the unitsToPickFrom has already been scoped 
public class Picker {
	//TODO we'll likely need better indexing capabilities
	private IInstallableUnit[] preferredSet;
	private IInstallableUnit[] completeSet;
	private IInstallableUnit[] secondChoiceSet;
	private RecommendationDescriptor recommendations;

	private List filters;

	public Picker(IInstallableUnit[] unitsToPickFrom, RecommendationDescriptor recommendations) {
		if (unitsToPickFrom != null)
			completeSet = unitsToPickFrom;
		else
			completeSet = new IInstallableUnit[0];
		this.secondChoiceSet = completeSet;
		this.recommendations = recommendations;
		this.filters = new ArrayList(2);
	}

	public Collection[] findInstallableUnit(String id, VersionRange range, RequiredCapability searchedCapability) {
		IInstallableUnit[][] tmp = findInstallableUnit(id, range, new RequiredCapability[] {searchedCapability}, false);
		return new Collection[] {Arrays.asList(tmp[0]), Arrays.asList(tmp[1])};
	}

	public void prefer(Collection filtersToAdd) {
		if (!filters.addAll(filtersToAdd))
			return;
		if (filters.size() == 0)
			return;
		Set preferredIUs = new HashSet(completeSet.length);
		Set secondChoice = new HashSet(completeSet.length);
		for (int i = 0; i < completeSet.length; i++) {
			for (Iterator iterator = filters.iterator(); iterator.hasNext();) {
				if (((IUFilter) iterator.next()).accept(completeSet[i])) {
					preferredIUs.add(completeSet[i]);
					continue;
				} else {
					secondChoice.add(completeSet[i]);
				}
			}
		}
		preferredSet = (IInstallableUnit[]) preferredIUs.toArray(new IInstallableUnit[preferredIUs.size()]);
		secondChoiceSet = (IInstallableUnit[]) secondChoice.toArray(new IInstallableUnit[secondChoice.size()]);
	}

	public IInstallableUnit[][] findInstallableUnit(String id, VersionRange range, RequiredCapability[] searchedCapability, boolean fragmentsOnly) {
		return new IInstallableUnit[][] {findInstallableUnit(preferredSet, id, range, searchedCapability, fragmentsOnly), findInstallableUnit(secondChoiceSet, id, range, searchedCapability, fragmentsOnly)};
	}

	//TODO what should be the return value when all the parameters are null. Is it even a valid call?
	//TODO A lot of improvement could be done on this algorithm, for example
	// - remove from the set of searchedCapability the one that are found
	private IInstallableUnit[] findInstallableUnit(IInstallableUnit[] pool, String id, VersionRange range, RequiredCapability[] searchedCapability, boolean fragmentsOnly) {
		if (pool == null || pool.length == 0)
			return new IInstallableUnit[0];
		Set candidates = new HashSet();

		//Filter on plugin id and range
		if (id != null && range != null) {
			for (int i = 0; i < pool.length; i++) {
				if (pool[i].getId().equals(id) && range.isIncluded(pool[i].getVersion()))
					candidates.add(pool[i]);
			}
			pool = (IInstallableUnit[]) candidates.toArray(new IInstallableUnit[candidates.size()]);
		}

		//Filter on capabilities.
		if (searchedCapability != null) {
			searchedCapability = rewrite(searchedCapability);
			for (int i = 0; i < pool.length; i++) {
				IInstallableUnit candidate = pool[i];
				for (int k = 0; k < searchedCapability.length; k++) {
					boolean valid = false;
					ProvidedCapability[] capabilities = candidate.getProvidedCapabilities();
					if (capabilities.length == 0)
						valid = false;
					for (int j = 0; j < capabilities.length; j++) {
						if ((searchedCapability[k].getName().equals(capabilities[j].getName()) && searchedCapability[k].getNamespace().equals(capabilities[j].getNamespace()) && (searchedCapability[k].getRange() == null ? true : searchedCapability[k].getRange().isIncluded(capabilities[j].getVersion())))) { //TODO Need to deal with option
							valid = true;
							break;
						}
					}
					if (valid && (!fragmentsOnly || candidate.isFragment())) {
						candidates.add(candidate);
					}
				}
			}
			pool = (IInstallableUnit[]) candidates.toArray(new IInstallableUnit[candidates.size()]);
		}

		return pool;
	}

	private RequiredCapability[] rewrite(RequiredCapability[] requiredCapabilities) {
		if (recommendations == null)
			return requiredCapabilities;
		RequiredCapability[] result = new RequiredCapability[requiredCapabilities.length];
		for (int i = 0; i < requiredCapabilities.length; i++) {
			result[i] = getRecommendation(requiredCapabilities[i]);
			if (result[i] == null)
				result[i] = requiredCapabilities[i];
		}
		return result;
	}

	private RequiredCapability getRecommendation(RequiredCapability match) {
		Recommendation foundRecommendation = recommendations.findRecommendation(match);
		return foundRecommendation != null ? foundRecommendation.newValue() : match;
	}
}
