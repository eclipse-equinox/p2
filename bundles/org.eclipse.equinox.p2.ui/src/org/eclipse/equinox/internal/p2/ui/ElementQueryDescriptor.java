/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.query.*;

/**
 * ElementQueryDescriptor represents everything needed to run a query, including
 * the object to be queried, the query to use, and the query result.  It can optionally
 * wrap the query results in a UI element.
 * 
 * @since 3.4
 */
public class ElementQueryDescriptor {

	private IQuery<Object> query;
	private Collector<Object> collector;
	private IQueryable<Object> queryable;
	private ElementWrapper wrapper;

	/**
	 * Creates an ElementQueryDescriptor to represent a Query, its collector the queryable
	 * on which it will run.
	 */
	public ElementQueryDescriptor(IQueryable<?> queryable, IQuery<?> query, Collector<?> collector) {
		this(queryable, query, collector, null);
	}

	/**
	 * Creates an ElementQueryDescriptor to represent a Query, its collector the queryable
	 * on which it will run, and the transformer used to transform the results.
	 */
	@SuppressWarnings("unchecked")
	public ElementQueryDescriptor(IQueryable<?> queryable, IQuery<?> query, Collector<?> collector, ElementWrapper wrapper) {
		this.query = (IQuery<Object>) query;
		this.collector = (Collector<Object>) collector;
		this.queryable = (IQueryable<Object>) queryable;
		this.wrapper = wrapper;
	}

	/**
	 * Performs the query returning a collection of results.
	 * @param monitor
	 */
	public Collection<?> performQuery(IProgressMonitor monitor) {
		Collector<Object> results = this.collector;
		// If the query is completely described, perform it
		if (query != null && collector != null && queryable != null)
			results.addAll(this.queryable.query(this.query, monitor));
		else if (results == null)
			results = new Collector<Object>();
		// Let the wrapper analyze the results, even if we didn't perform the query.
		// This allows the wrapper to modify the results with explanations.
		if (wrapper != null)
			return wrapper.getElements(results);
		return results.toUnmodifiableSet();
	}

	public boolean hasCollector() {
		return this.collector != null;
	}

	public boolean hasQueryable() {
		return this.queryable != null;
	}

	public boolean hasQuery() {
		return this.query != null;
	}
}
