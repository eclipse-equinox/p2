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
import org.eclipse.equinox.internal.p2.ql.CapabilityIndexFunction.IUCapability;

/**
 * An in-memory implementation of a CapabilityIndex based on a Map.
 */
public class CapabilityIndex {

	private final Map capabilityMap;

	public CapabilityIndex(Map capabilityMap) {
		this.capabilityMap = capabilityMap;
	}

	public Collection satisfiesAny(Object value) {
		if (value instanceof IRequiredCapability) {
			List collector = new ArrayList();
			collectMatchingIUs((IRequiredCapability) value, collector);
			return collector;
		} else if (value instanceof Iterator) {
			Iterator itor = (Iterator) value;
			if (!itor.hasNext())
				return Collections.EMPTY_LIST;
			List collector = new ArrayList();
			do {
				Object nxt = itor.next();
				if (!(nxt instanceof IRequiredCapability))
					throw new IllegalArgumentException();
				collectMatchingIUs((IRequiredCapability) nxt, collector);
			} while (itor.hasNext());
			return collector;
		}
		throw new IllegalArgumentException();
	}

	public Collection satisfiesAll(Object value) {
		if (value instanceof IRequiredCapability) {
			List collector = new ArrayList();
			collectMatchingIUs((IRequiredCapability) value, collector);
			return collector;
		} else if (value instanceof Iterator) {
			Iterator itor = (Iterator) value;
			if (!itor.hasNext())
				return Collections.EMPTY_LIST;

			Set collector = new HashSet();
			Object nxt = itor.next();
			if (!(nxt instanceof IRequiredCapability))
				throw new IllegalArgumentException();
			collectMatchingIUs((IRequiredCapability) nxt, collector);

			while (itor.hasNext() && !collector.isEmpty()) {
				nxt = itor.next();
				if (!(nxt instanceof IRequiredCapability))
					throw new IllegalArgumentException();
				collector = retainMatchingIUs((IRequiredCapability) nxt, collector);
			}
			return collector;
		}
		throw new IllegalArgumentException();
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
