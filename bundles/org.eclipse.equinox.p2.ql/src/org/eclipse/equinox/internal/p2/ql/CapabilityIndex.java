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
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.ql.ICapabilityIndex;

/**
 * An in-memory implementation of a CapabilityIndex based on a Map.
 */
public class CapabilityIndex implements ICapabilityIndex {

	private final Map<String, Object> capabilityMap;

	private static class IUCapability {
		final IInstallableUnit iu;
		final IProvidedCapability capability;

		IUCapability(IInstallableUnit iu, IProvidedCapability capability) {
			this.iu = iu;
			this.capability = capability;
		}
	}

	public CapabilityIndex(Iterator<IInstallableUnit> itor) {
		HashMap<String, Object> index = new HashMap<String, Object>();
		while (itor.hasNext()) {
			IInstallableUnit iu = itor.next();
			Collection<IProvidedCapability> pcs = iu.getProvidedCapabilities();
			for (IProvidedCapability pc : pcs) {
				IUCapability iuCap = new IUCapability(iu, pc);
				String name = pc.getName();
				Object prev = index.put(name, iuCap);
				if (prev != null) {
					ArrayList<IUCapability> lst;
					if (prev instanceof ArrayList<?>)
						lst = (ArrayList<IUCapability>) prev;
					else {
						lst = new ArrayList<IUCapability>(4);
						lst.add((IUCapability) prev);
					}
					lst.add(iuCap);
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
			collectMatchingIUs((IRequiredCapability) nxt, collector);
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
		if (!(requirement instanceof IRequiredCapability))
			return;
		IRequiredCapability rc = (IRequiredCapability) requirement;
		Object v = capabilityMap.get(rc.getName());
		if (v == null)
			return;

		if (v instanceof IUCapability) {
			IUCapability iuc = (IUCapability) v;
			if (iuc.capability.satisfies(requirement))
				collector.add(iuc.iu);
		} else {
			List<IUCapability> iucs = (List<IUCapability>) v;
			int idx = iucs.size();
			while (--idx >= 0) {
				IUCapability iuc = iucs.get(idx);
				if (iuc.capability.satisfies(requirement))
					collector.add(iuc.iu);
			}
		}
	}

	private Set<IInstallableUnit> retainMatchingIUs(IRequirement requirement, Set<IInstallableUnit> collector) {
		if (!(requirement instanceof IRequiredCapability))
			return CollectionUtils.emptySet();

		IRequiredCapability rc = (IRequiredCapability) requirement;
		Object v = capabilityMap.get(rc.getName());
		if (v == null)
			return CollectionUtils.emptySet();

		Set<IInstallableUnit> retained = null;
		if (v instanceof IUCapability) {
			IUCapability iuc = (IUCapability) v;
			if (iuc.capability.satisfies(requirement) && collector.contains(iuc.iu)) {
				if (retained == null)
					retained = new HashSet<IInstallableUnit>();
				retained.add(iuc.iu);
			}
		} else {
			List<IUCapability> iucs = (List<IUCapability>) v;
			int idx = iucs.size();
			while (--idx >= 0) {
				IUCapability iuc = iucs.get(idx);
				if (iuc.capability.satisfies(requirement) && collector.contains(iuc.iu)) {
					if (retained == null)
						retained = new HashSet<IInstallableUnit>();
					retained.add(iuc.iu);
				}
			}
		}
		return retained == null ? CollectionUtils.<IInstallableUnit> emptySet() : retained;
	}
}
