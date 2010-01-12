/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import org.eclipse.equinox.p2.metadata.IProvidedCapability;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;

public class QueryableArray implements IQueryable<IInstallableUnit> {
	static class IUCapability {
		final IInstallableUnit iu;
		final IProvidedCapability capability;

		public IUCapability(IInstallableUnit iu, IProvidedCapability capability) {
			this.iu = iu;
			this.capability = capability;
		}
	}

	private final List<IInstallableUnit> dataSet;
	private Map<String, List<IUCapability>> namedCapabilityIndex;

	public QueryableArray(IInstallableUnit[] ius) {
		dataSet = Arrays.asList(ius);
	}

	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		if (query instanceof IRequiredCapability)
			return queryCapability((IRequiredCapability) query, new Collector<IInstallableUnit>(), monitor);
		return query.perform(dataSet.iterator());
	}

	private Collector<IInstallableUnit> queryCapability(IRequiredCapability query, Collector<IInstallableUnit> collector, IProgressMonitor monitor) {
		generateNamedCapabilityIndex();

		Collection<IInstallableUnit> resultIUs = null;
		Collection<IInstallableUnit> matchingIUs = findMatchingIUs(query);
		if (matchingIUs == null)
			return collector;
		if (resultIUs == null)
			resultIUs = matchingIUs;
		else
			resultIUs.retainAll(matchingIUs);

		if (resultIUs != null)
			for (Iterator<IInstallableUnit> iterator = resultIUs.iterator(); iterator.hasNext();)
				collector.accept(iterator.next());

		return collector;
	}

	private Collection<IInstallableUnit> findMatchingIUs(IRequiredCapability requiredCapability) {
		List<IUCapability> iuCapabilities = namedCapabilityIndex.get(requiredCapability.getName());
		if (iuCapabilities == null)
			return null;

		Set<IInstallableUnit> matchingIUs = new HashSet<IInstallableUnit>();
		for (IUCapability iuCapability : iuCapabilities) {
			if (iuCapability.iu.satisfies(requiredCapability))
				matchingIUs.add(iuCapability.iu);
		}
		return matchingIUs;
	}

	private void generateNamedCapabilityIndex() {
		if (namedCapabilityIndex != null)
			return;

		namedCapabilityIndex = new HashMap<String, List<IUCapability>>();
		for (IInstallableUnit iu : dataSet) {

			Collection<IProvidedCapability> providedCapabilities = iu.getProvidedCapabilities();
			for (IProvidedCapability pc : providedCapabilities) {
				String name = pc.getName();
				List<IUCapability> iuCapabilities = namedCapabilityIndex.get(name);
				if (iuCapabilities == null) {
					iuCapabilities = new ArrayList<IUCapability>(5);
					namedCapabilityIndex.put(name, iuCapabilities);
				}
				iuCapabilities.add(new IUCapability(iu, pc));
			}
		}
	}
}
