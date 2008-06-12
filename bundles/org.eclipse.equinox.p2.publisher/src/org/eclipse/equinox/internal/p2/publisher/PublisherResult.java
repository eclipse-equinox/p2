/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

public class PublisherResult implements IPublisherResult {
	// type markers
	public static final String ROOT = "root"; //$NON-NLS-1$
	public static final String NON_ROOT = "non_root"; //$NON-NLS-1$

	// The set of top level IUs
	final Map rootIUs = new HashMap();

	// The set of internal and leaf IUs
	final Map nonRootIUs = new HashMap();

	public void addIU(IInstallableUnit iu, String type) {
		if (type == ROOT)
			addIU(rootIUs, iu.getId(), iu);
		if (type == NON_ROOT)
			addIU(nonRootIUs, iu.getId(), iu);
	}

	public void addIUs(Collection ius, String type) {
		for (Iterator i = ius.iterator(); i.hasNext();)
			addIU((IInstallableUnit) i.next(), type);
	}

	private void addIU(Map map, String id, IInstallableUnit iu) {
		Set ius = (Set) map.get(id);
		if (ius == null) {
			ius = new HashSet(11);
			map.put(id, ius);
		}
		ius.add(iu);
	}

	/**
	 * Returns all IUs generated during this execution of the generator.
	 */
	public Map getGeneratedIUs(String type) {
		if (type == null) {
			HashMap all = new HashMap();
			all.putAll(rootIUs);
			all.putAll(nonRootIUs);
			return all;
		}
		if (type == ROOT)
			return rootIUs;
		if (type == NON_ROOT)
			return nonRootIUs;
		throw new IllegalArgumentException("Invalid IU type: " + type); //$NON-NLS-1$
	}

	// TODO this method really should not be needed as it just returns the first
	// matching IU non-deterministically.
	public IInstallableUnit getIU(String id, String type) {
		if (type == null || type == ROOT) {
			Collection ius = (Collection) rootIUs.get(id);
			if (ius != null && ius.size() > 0)
				return (IInstallableUnit) ius.iterator().next();
		}
		if (type == null || type == NON_ROOT) {
			Collection ius = (Collection) nonRootIUs.get(id);
			if (ius != null && ius.size() > 0)
				return (IInstallableUnit) ius.iterator().next();
		}
		return null;
	}

	/**
	 * Returns the IUs in this result with the given id.
	 */
	public Collection getIUs(String id, String type) {
		if (type == null) {
			ArrayList result = new ArrayList();
			result.addAll(id == null ? flatten(rootIUs.values()) : (Collection) rootIUs.get(id));
			result.addAll(id == null ? flatten(nonRootIUs.values()) : (Collection) nonRootIUs.get(id));
			return result;
		}
		if (type == ROOT)
			return id == null ? flatten(rootIUs.values()) : (Collection) rootIUs.get(id);
		if (type == NON_ROOT)
			return id == null ? flatten(nonRootIUs.values()) : (Collection) nonRootIUs.get(id);
		return null;
	}

	private List flatten(Collection values) {
		ArrayList result = new ArrayList();
		for (Iterator i = values.iterator(); i.hasNext();)
			for (Iterator j = ((HashSet) i.next()).iterator(); j.hasNext();) 
				result.add(j.next());
		return result;
	}

	public void merge(IPublisherResult result, int mode) {
		if (mode == MERGE_MATCHING) {
			addIUs(result.getIUs(null, ROOT), ROOT);
			addIUs(result.getIUs(null, NON_ROOT), NON_ROOT);
		} else if (mode == MERGE_ALL_ROOT) {
			addIUs(result.getIUs(null, ROOT), ROOT);
			addIUs(result.getIUs(null, NON_ROOT), ROOT);
		} else if (mode == MERGE_ALL_NON_ROOT) {
			addIUs(result.getIUs(null, ROOT), NON_ROOT);
			addIUs(result.getIUs(null, NON_ROOT), NON_ROOT);
		}
	}

}
