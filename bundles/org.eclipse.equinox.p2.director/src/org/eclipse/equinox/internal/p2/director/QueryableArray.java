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

import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.index.CapabilityIndex;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.index.*;
import org.eclipse.equinox.p2.query.*;

public class QueryableArray implements IQueryable<IInstallableUnit>, IIndexProvider<IInstallableUnit> {
	private final List<IInstallableUnit> dataSet;
	private IIndex<IInstallableUnit> capabilityIndex;

	public QueryableArray(IInstallableUnit[] ius) {
		dataSet = CollectionUtils.unmodifiableList(ius);
	}

	public synchronized IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		return query instanceof IQueryWithIndex<?> ? ((IQueryWithIndex<IInstallableUnit>) query).perform(this) : query.perform(dataSet.iterator());
	}

	public Iterator<IInstallableUnit> everything() {
		return dataSet.iterator();
	}

	public synchronized IIndex<IInstallableUnit> getIndex(String memberName) {
		if (InstallableUnit.MEMBER_PROVIDED_CAPABILITIES.equals(memberName)) {
			if (capabilityIndex == null)
				capabilityIndex = new CapabilityIndex(dataSet.iterator());
			return capabilityIndex;
		}
		return null;
	}
}
