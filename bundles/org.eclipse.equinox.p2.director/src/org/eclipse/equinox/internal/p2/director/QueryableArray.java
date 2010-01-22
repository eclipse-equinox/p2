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

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.metadata.query.ExpressionQuery;
import org.eclipse.equinox.p2.query.*;

public class QueryableArray implements IQueryable<IInstallableUnit> {
	private final List<IInstallableUnit> dataSet;
	private Map<String, List<IInstallableUnit>> namedCapabilityIndex;

	public QueryableArray(IInstallableUnit[] ius) {
		dataSet = Arrays.asList(ius);
	}

	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		if (query instanceof ExpressionQuery<?>)
			return queryCapability((ExpressionQuery<IInstallableUnit>) query, new Collector<IInstallableUnit>(), monitor);
		return query.perform(dataSet.iterator());
	}

	private Collector<IInstallableUnit> queryCapability(ExpressionQuery<IInstallableUnit> query, Collector<IInstallableUnit> collector, IProgressMonitor monitor) {
		generateNamedCapabilityIndex();

		Collection<IInstallableUnit> resultIUs = null;
		Collection<IInstallableUnit> matchingIUs = findMatchingIUs(query.getExpression());
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

	private Collection<IInstallableUnit> findMatchingIUs(IMatchExpression<IInstallableUnit> requirementMatch) {
		// TODO: This is a hack. Should be replaced by use of proper indexes
		List<IInstallableUnit> ius = namedCapabilityIndex.get(RequiredCapability.extractName(requirementMatch));
		if (ius == null)
			return null;

		Set<IInstallableUnit> matchingIUs = new HashSet<IInstallableUnit>();
		for (IInstallableUnit iu : ius) {
			if (requirementMatch.isMatch(iu))
				matchingIUs.add(iu);
		}
		return matchingIUs;
	}

	private void generateNamedCapabilityIndex() {
		if (namedCapabilityIndex != null)
			return;

		namedCapabilityIndex = new HashMap<String, List<IInstallableUnit>>();
		for (IInstallableUnit iu : dataSet) {

			Collection<IProvidedCapability> providedCapabilities = iu.getProvidedCapabilities();
			for (IProvidedCapability pc : providedCapabilities) {
				String name = pc.getName();
				List<IInstallableUnit> ius = namedCapabilityIndex.get(name);
				if (ius == null) {
					ius = new ArrayList<IInstallableUnit>(5);
					namedCapabilityIndex.put(name, ius);
				}
				ius.add(iu);
			}
		}
	}
}
