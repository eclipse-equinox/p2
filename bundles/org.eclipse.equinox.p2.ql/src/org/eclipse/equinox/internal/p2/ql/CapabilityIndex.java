/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ql;

import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.ql.ICapabilityIndex;

/**
 * An in-memory implementation of a CapabilityIndex based on a Map.
 */
public class CapabilityIndex implements ICapabilityIndex {

	private final Map<String, Object> capabilityMap;

	@SuppressWarnings("unchecked")
	public CapabilityIndex(Iterator<IInstallableUnit> itor) {
		HashMap<String, Object> index = new HashMap<String, Object>(300);
		while (itor.hasNext()) {
			IInstallableUnit iu = itor.next();
			Collection<IProvidedCapability> pcs = iu.getProvidedCapabilities();
			for (IProvidedCapability pc : pcs) {
				String name = pc.getName();
				Object prev = index.put(name, iu);
				if (prev != null) {
					ArrayList<IInstallableUnit> lst;
					if (prev instanceof ArrayList<?>)
						lst = (ArrayList<IInstallableUnit>) prev;
					else {
						lst = new ArrayList<IInstallableUnit>(4);
						lst.add((IInstallableUnit) prev);
					}
					lst.add(iu);
					index.put(name, lst);
				}
			}
		}
		this.capabilityMap = index;
	}

	public Iterator<IInstallableUnit> satisfiesAny(Iterator<IRequirement> requirements) {
		if (!requirements.hasNext())
			return CollectionUtils.<IInstallableUnit> emptyList().iterator();

		List<IInstallableUnit> collector = new ArrayList<IInstallableUnit>();
		do {
			IRequirement nxt = requirements.next();
			collectMatchingIUs(nxt, collector);
		} while (requirements.hasNext());
		return collector.iterator();
	}

	public Iterator<IInstallableUnit> satisfiesAll(Iterator<IRequirement> requirements) {
		if (!requirements.hasNext())
			return CollectionUtils.<IInstallableUnit> emptyList().iterator();

		Set<IInstallableUnit> collector = new HashSet<IInstallableUnit>();
		collectMatchingIUs(requirements.next(), collector);

		while (requirements.hasNext() && !collector.isEmpty())
			collector = retainMatchingIUs(requirements.next(), collector);
		return collector.iterator();
	}

	private void collectMatchingIUs(IRequirement requirement, Collection<IInstallableUnit> collector) {
		IMatchExpression<IInstallableUnit> matches = requirement.getMatches();
		String name = RequiredCapability.extractName(matches);
		Object v = capabilityMap.get(name);
		if (v == null)
			return;

		IEvaluationContext ctx = matches.createContext();
		if (v instanceof IInstallableUnit) {
			IInstallableUnit iu = (IInstallableUnit) v;
			if (matches.isMatch(ctx, iu))
				collector.add(iu);
		} else {
			@SuppressWarnings("unchecked")
			List<IInstallableUnit> ius = (List<IInstallableUnit>) v;
			int idx = ius.size();
			while (--idx >= 0) {
				IInstallableUnit iu = ius.get(idx);
				if (matches.isMatch(ctx, iu))
					collector.add(iu);
			}
		}
	}

	private Set<IInstallableUnit> retainMatchingIUs(IRequirement requirement, Set<IInstallableUnit> collector) {
		IMatchExpression<IInstallableUnit> matches = requirement.getMatches();
		String name = RequiredCapability.extractName(matches);
		Object v = capabilityMap.get(name);
		if (v == null)
			return CollectionUtils.emptySet();

		IEvaluationContext ctx = matches.createContext();
		Set<IInstallableUnit> retained = null;
		if (v instanceof IInstallableUnit) {
			IInstallableUnit iu = (IInstallableUnit) v;
			if (matches.isMatch(ctx, iu) && collector.contains(iu)) {
				if (retained == null)
					retained = new HashSet<IInstallableUnit>();
				retained.add(iu);
			}
		} else {
			@SuppressWarnings("unchecked")
			List<IInstallableUnit> ius = (List<IInstallableUnit>) v;
			int idx = ius.size();
			while (--idx >= 0) {
				IInstallableUnit iu = ius.get(idx);
				if (matches.isMatch(ctx, iu) && collector.contains(iu)) {
					if (retained == null)
						retained = new HashSet<IInstallableUnit>();
					retained.add(iu);
				}
			}
		}
		return retained == null ? CollectionUtils.<IInstallableUnit> emptySet() : retained;
	}
}
