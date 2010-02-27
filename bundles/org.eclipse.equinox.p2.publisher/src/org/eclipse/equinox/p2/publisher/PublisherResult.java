/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   Cloudsmith Inc. - query indexes
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.IUMap;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.expression.CompoundIterator;
import org.eclipse.equinox.internal.p2.metadata.index.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.query.IQueryResult;

public class PublisherResult extends IndexProvider<IInstallableUnit> implements IPublisherResult {

	final IUMap rootIUs = new IUMap();
	final IUMap nonRootIUs = new IUMap();

	private IIndex<IInstallableUnit> idIndex;

	public void addIU(IInstallableUnit iu, String type) {
		if (type == ROOT)
			rootIUs.add(iu);
		if (type == NON_ROOT)
			nonRootIUs.add(iu);
	}

	public void addIUs(Collection<IInstallableUnit> ius, String type) {
		for (IInstallableUnit iu : ius)
			addIU(iu, type);
	}

	public IInstallableUnit getIU(String id, Version version, String type) {
		if (type == null || type == ROOT) {
			IInstallableUnit result = rootIUs.get(id, version);
			if (result != null)
				return result;
		}
		if (type == null || type == NON_ROOT) {
			IInstallableUnit result = nonRootIUs.get(id, version);
			if (result != null)
				return result;
		}
		return null;
	}

	// TODO this method really should not be needed as it just returns the first
	// matching IU non-deterministically.
	public IInstallableUnit getIU(String id, String type) {
		if (type == null || type == ROOT) {
			IQueryResult<IInstallableUnit> ius = rootIUs.get(id);
			if (!ius.isEmpty())
				return ius.iterator().next();
		}
		if (type == null || type == NON_ROOT) {
			IQueryResult<IInstallableUnit> ius = nonRootIUs.get(id);
			if (!ius.isEmpty())
				return ius.iterator().next();
		}
		return null;
	}

	/**
	 * Returns the IUs in this result with the given id.
	 */
	public Collection<IInstallableUnit> getIUs(String id, String type) {
		if (type == null) {
			// TODO can this be optimized?
			ArrayList<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
			result.addAll(rootIUs.get(id).toUnmodifiableSet());
			result.addAll(nonRootIUs.get(id).toUnmodifiableSet());
			return result;
		}
		if (type == ROOT)
			return rootIUs.get(id).toUnmodifiableSet();
		if (type == NON_ROOT)
			return nonRootIUs.get(id).toUnmodifiableSet();
		return null;
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

	public synchronized IIndex<IInstallableUnit> getIndex(String memberName) {
		if (InstallableUnit.MEMBER_ID.equals(memberName)) {
			if (idIndex == null) {
				ArrayList<IIndex<IInstallableUnit>> indexes = new ArrayList<IIndex<IInstallableUnit>>();
				indexes.add(new IdIndex(nonRootIUs));
				indexes.add(new IdIndex(rootIUs));
				idIndex = new CompoundIndex<IInstallableUnit>(indexes);
			}
			return idIndex;
		}
		return null;
	}

	public Iterator<IInstallableUnit> everything() {
		ArrayList<Iterator<IInstallableUnit>> iterators = new ArrayList<Iterator<IInstallableUnit>>();
		iterators.add(nonRootIUs.iterator());
		iterators.add(rootIUs.iterator());
		return new CompoundIterator<IInstallableUnit>(iterators.iterator());
	}

	public Object getManagedProperty(Object client, String memberName, Object key) {
		return null;
	}
}
