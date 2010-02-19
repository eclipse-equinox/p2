/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.query;

import java.util.*;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.query.*;

/**
 * This query returns the latest version for each unique VersionedID.  
 * All other elements are discarded.
 */
public class LatestIUVersionQuery<T extends IVersionedId> extends ContextQuery<T> {

	private final IQuery<T> query;

	public LatestIUVersionQuery() {
		this.query = null;
	}

	public LatestIUVersionQuery(IQuery<T> query) {
		this.query = query;
	}

	/**
	 * Performs the LatestIUVersionQuery
	 */
	public IQueryResult<T> perform(Iterator<T> iterator) {
		if (query != null)
			iterator = query.perform(iterator).iterator();

		HashMap<String, T> greatestIUVersion = new HashMap<String, T>();
		while (iterator.hasNext()) {
			T versionedID = iterator.next();
			if (greatestIUVersion.containsKey(versionedID.getId())) {
				T currentIU = greatestIUVersion.get(versionedID.getId());
				if (currentIU.getVersion().compareTo(versionedID.getVersion()) < 0)
					greatestIUVersion.put(versionedID.getId(), versionedID);
			} else
				greatestIUVersion.put(versionedID.getId(), versionedID);
		}

		Collection<T> values = greatestIUVersion.values();
		Iterator<T> valuesIterator = values.iterator();
		boolean continueGather = true;

		Collector<T> result = new Collector<T>();
		while (valuesIterator.hasNext() && continueGather) {
			T nextIU = valuesIterator.next();
			continueGather = result.accept(nextIU);
		}
		return result;
	}
}
