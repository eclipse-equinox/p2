/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.query;

import java.util.HashMap;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.model.IUVersionsElement;

/**
 * Collector that only accepts categories or the latest version of each
 * IU, and wraps that version in an IUVersionsElement.
 * 
 * @since 3.4
 */
public class LatestIUVersionCollector extends AvailableIUCollector {

	private HashMap uniqueIds = new HashMap();

	public LatestIUVersionCollector(IProvElementQueryProvider queryProvider, IQueryable queryable) {
		super(queryProvider, queryable);
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		if (!(match instanceof IInstallableUnit))
			return true;
		IInstallableUnit iu = (IInstallableUnit) match;
		// If it's a category, treat it as such
		if (isCategory(iu))
			return super.accept(match);
		// It is not a category.  Look for the latest element
		IUVersionsElement matchElement = (IUVersionsElement) uniqueIds.get(iu.getId());
		if (matchElement == null) {
			matchElement = new IUVersionsElement(iu);
			uniqueIds.put(iu.getId(), matchElement);
			return super.accept(matchElement);
		}
		// There is already an element
		if (iu.getVersion().compareTo(matchElement.getIU().getVersion()) > 0) {
			matchElement.setIU(iu);
		}
		return true;
	}

}
