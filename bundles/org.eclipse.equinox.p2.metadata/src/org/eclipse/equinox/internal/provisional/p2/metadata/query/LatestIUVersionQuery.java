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
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.ContextQuery;

/**
 * This query returns the latest version for each unique IU Id.  
 * All other elements are discarded.
 */
public class LatestIUVersionQuery extends ContextQuery {

	/**
	 * Performs the LatestIUVersionQuery
	 */
	public Collector perform(Iterator iterator, Collector result) {
		HashMap greatestIUVersion = new HashMap();
		while (iterator.hasNext()) {
			Object next = iterator.next();

			if (!(next instanceof IInstallableUnit))
				// Don't accept things if they are not IUs
				continue;
			IInstallableUnit iu = (IInstallableUnit) next;
			if (greatestIUVersion.containsKey(iu.getId())) {
				IInstallableUnit currentIU = (IInstallableUnit) greatestIUVersion.get(iu.getId());
				if (currentIU.getVersion().compareTo(iu.getVersion()) < 0)
					greatestIUVersion.put(iu.getId(), iu);
			} else
				greatestIUVersion.put(iu.getId(), iu);
		}

		Collection values = greatestIUVersion.values();
		Iterator valuesIterator = values.iterator();
		boolean continueGather = true;

		while (valuesIterator.hasNext() && continueGather) {
			IInstallableUnit nextIU = (IInstallableUnit) valuesIterator.next();
			continueGather = result.accept(nextIU);
		}
		return result;
	}
}
