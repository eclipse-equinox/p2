/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.query;

import java.util.*;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;

/**
 * This query returns the latest version for each unique VersionedID.  
 * All other elements are discarded.
 */
public class LatestIUVersionQuery extends ContextQuery {

	/**
	 * Performs the LatestIUVersionQuery
	 */
	public IQueryResult perform(Iterator iterator) {
		HashMap greatestIUVersion = new HashMap();
		while (iterator.hasNext()) {
			Object next = iterator.next();

			if (!(next instanceof IVersionedId))
				// Don't accept things if they are not IUs
				continue;
			IVersionedId versionedID = (IVersionedId) next;
			if (greatestIUVersion.containsKey(versionedID.getId())) {
				IVersionedId currentIU = (IVersionedId) greatestIUVersion.get(versionedID.getId());
				if (currentIU.getVersion().compareTo(versionedID.getVersion()) < 0)
					greatestIUVersion.put(versionedID.getId(), versionedID);
			} else
				greatestIUVersion.put(versionedID.getId(), versionedID);
		}

		Collection values = greatestIUVersion.values();
		Iterator valuesIterator = values.iterator();
		boolean continueGather = true;

		Collector result = new Collector();
		while (valuesIterator.hasNext() && continueGather) {
			IVersionedId nextIU = (IVersionedId) valuesIterator.next();
			continueGather = result.accept(nextIU);
		}
		return result;
	}
}
