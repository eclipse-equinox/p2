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

import org.eclipse.equinox.p2.query.Collector;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * Collector that assigns a query provider and the queryable
 * who was doing the query to the elements
 * as they are accepted.
 * 
 * @since 3.4
 */
public class QueriedElementCollector extends Collector {

	IProvElementQueryProvider queryProvider;
	IQueryable queryable;

	public QueriedElementCollector(IProvElementQueryProvider queryProvider, IQueryable queryable) {
		this.queryProvider = queryProvider;
		this.queryable = queryable;
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		if (match instanceof QueriedElement) {
			QueriedElement element = (QueriedElement) match;
			element.setQueryProvider(queryProvider);
			if (element.getQueryable() == null) {
				element.setQueryable(queryable);
			}
		}
		return super.accept(match);
	}
}
