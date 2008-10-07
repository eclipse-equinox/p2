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
package org.eclipse.equinox.internal.provisional.p2.ui;

import org.eclipse.equinox.internal.provisional.p2.query.*;

/**
 * Data class representing everything needed to run a query, including
 * the object to be queried, the query to use, and the query result.
 * 
 * @since 3.4
 */
public class ElementQueryDescriptor {

	public Query query;
	public Collector collector;
	public IQueryable queryable;

	public ElementQueryDescriptor(IQueryable queryable, Query query, Collector collector) {
		this.query = query;
		this.collector = collector;
		this.queryable = queryable;
	}

	public boolean isComplete() {
		return query != null && collector != null && queryable != null;
	}
}
