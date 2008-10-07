/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;

/**
 * Collector that assigns a query provider and the queryable
 * who was performing the query to the elements
 * as they are accepted.
 * 
 * @since 3.4
 */
public class QueriedElementCollector extends Collector {

	protected IQueryable queryable;
	protected Object parent;

	public QueriedElementCollector(IQueryable queryable, Object parent) {
		this.queryable = queryable;
		this.parent = parent;
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
			if (!element.knowsQueryable()) {
				element.setQueryable(queryable);
			}
		}
		return super.accept(match);
	}
}
