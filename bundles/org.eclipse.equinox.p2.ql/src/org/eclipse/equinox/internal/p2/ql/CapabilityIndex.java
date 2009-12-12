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
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ql.ICapabilityIndex;

/**
 * An in-memory implementation of a CapabilityIndex based on a Map.
 */
public class CapabilityIndex implements ICapabilityIndex {

	private final Map capabilityMap;

	private static class IUCapability {
		final IInstallableUnit iu;
		final IProvidedCapability capability;

		IUCapability(IInstallableUnit iu, IProvidedCapability capability) {
			this.iu = iu;
			this.capability = capability;
		}
	}

	public CapabilityIndex(Iterator itor) {
		HashMap index = new HashMap();
		while (itor.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) itor.next();
			IProvidedCapability[] pcs = iu.getProvidedCapabilities();
			int idx = pcs.length;
			while (--idx >= 0) {
				IProvidedCapability pc = pcs[idx];
				IUCapability iuCap = new IUCapability(iu, pc);
				String name = pc.getName();
				Object prev = index.put(name, iuCap);
				if (prev != null) {
					ArrayList lst;
					if (prev instanceof ArrayList)
						lst = (ArrayList) prev;
					else {
						lst = new ArrayList(4);
						lst.add(prev);
					}
					lst.add(iuCap);
					index.put(name, lst);
				}
			}
		}
		this.capabilityMap = index;
	}

	public Iterator satisfiesAny(Iterator requirements) {
		if (!requirements.hasNext())
			return Collections.EMPTY_LIST.iterator();

		List collector = new ArrayList();
		do {
			Object nxt = requirements.next();
			if (!(nxt instanceof IRequiredCapability))
				throw new IllegalArgumentException();
			collectMatchingIUs((IRequiredCapability) nxt, collector);
		} while (requirements.hasNext());
		return collector.iterator();
	}

	public Iterator satisfiesAll(Iterator requirements) {
		if (!requirements.hasNext())
			return Collections.EMPTY_LIST.iterator();

		Set collector = new HashSet();
		Object nxt = requirements.next();
		if (!(nxt instanceof IRequiredCapability))
			throw new IllegalArgumentException();
		collectMatchingIUs((IRequiredCapability) nxt, collector);

		while (requirements.hasNext() && !collector.isEmpty()) {
			nxt = requirements.next();
			if (!(nxt instanceof IRequiredCapability))
				throw new IllegalArgumentException();
			collector = retainMatchingIUs((IRequiredCapability) nxt, collector);
		}
		return collector.iterator();
	}

	private void collectMatchingIUs(IRequiredCapability rc, Collection collector) {
		Object v = capabilityMap.get(rc.getName());
		if (v == null)
			return;

		if (v instanceof IUCapability) {
			IUCapability iuc = (IUCapability) v;
			if (rc.satisfiedBy(iuc.capability))
				collector.add(iuc.iu);
		} else {
			List iucs = (List) v;
			int idx = iucs.size();
			while (--idx >= 0) {
				IUCapability iuc = (IUCapability) iucs.get(idx);
				if (rc.satisfiedBy(iuc.capability))
					collector.add(iuc.iu);
			}
		}
	}

	private Set retainMatchingIUs(IRequiredCapability rc, Set collector) {
		Object v = capabilityMap.get(rc.getName());
		if (v == null)
			return Collections.EMPTY_SET;

		Set retained = null;
		if (v instanceof IUCapability) {
			IUCapability iuc = (IUCapability) v;
			if (rc.satisfiedBy(iuc.capability) && collector.contains(iuc.iu)) {
				if (retained == null)
					retained = new HashSet();
				retained.add(iuc.iu);
			}
		} else {
			List iucs = (List) v;
			int idx = iucs.size();
			while (--idx >= 0) {
				IUCapability iuc = (IUCapability) iucs.get(idx);
				if (rc.satisfiedBy(iuc.capability) && collector.contains(iuc.iu)) {
					if (retained == null)
						retained = new HashSet();
					retained.add(iuc.iu);
				}
			}
		}
		return retained == null ? Collections.EMPTY_SET : retained;
	}
}
