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
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQuery;

public class QueryableArray implements IQueryable {
	static class IUCapability {
		final IInstallableUnit iu;
		final IProvidedCapability capability;

		public IUCapability(IInstallableUnit iu, IProvidedCapability capability) {
			this.iu = iu;
			this.capability = capability;
		}
	}

	private final List dataSet;
	private Map namedCapabilityIndex;

	public QueryableArray(IInstallableUnit[] ius) {
		dataSet = Arrays.asList(ius);
	}

	public Collector query(IQuery query, IProgressMonitor monitor) {
		Collector collector = new Collector();
		if (query instanceof IRequiredCapability)
			return queryCapability((IRequiredCapability) query, collector, monitor);
		return query.perform(dataSet.iterator(), collector);
	}

	private Collector queryCapability(IRequiredCapability query, Collector collector, IProgressMonitor monitor) {
		generateNamedCapabilityIndex();

		Collection resultIUs = null;
		Collection matchingIUs = findMatchingIUs(query);
		if (matchingIUs == null)
			return collector;
		if (resultIUs == null)
			resultIUs = matchingIUs;
		else
			resultIUs.retainAll(matchingIUs);

		if (resultIUs != null)
			for (Iterator iterator = resultIUs.iterator(); iterator.hasNext();)
				collector.accept(iterator.next());

		return collector;
	}

	private Collection findMatchingIUs(IRequiredCapability requiredCapability) {
		List iuCapabilities = (List) namedCapabilityIndex.get(requiredCapability.getName());
		if (iuCapabilities == null)
			return null;

		Set matchingIUs = new HashSet();
		for (Iterator iterator = iuCapabilities.iterator(); iterator.hasNext();) {
			IUCapability iuCapability = (IUCapability) iterator.next();
			if (iuCapability.iu.satisfies(requiredCapability))
				matchingIUs.add(iuCapability.iu);
		}
		return matchingIUs;
	}

	private void generateNamedCapabilityIndex() {
		if (namedCapabilityIndex != null)
			return;

		namedCapabilityIndex = new HashMap();
		for (Iterator iterator = dataSet.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();

			IProvidedCapability[] providedCapabilities = iu.getProvidedCapabilities();
			for (int i = 0; i < providedCapabilities.length; i++) {
				String name = providedCapabilities[i].getName();
				List iuCapabilities = (List) namedCapabilityIndex.get(name);
				if (iuCapabilities == null) {
					iuCapabilities = new ArrayList(5);
					namedCapabilityIndex.put(name, iuCapabilities);
				}
				iuCapabilities.add(new IUCapability(iu, providedCapabilities[i]));
			}
		}
	}
}
