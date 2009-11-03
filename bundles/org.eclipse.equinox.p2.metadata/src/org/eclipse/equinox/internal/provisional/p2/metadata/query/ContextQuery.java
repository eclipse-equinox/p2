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

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * ContextQuery is the abstract superclass for Queries that require the entire
 * input to evaluate the results.  Queries must consider the group of elements before
 * processing the results. <P>
 * 
 * ContextQueries must also be transitive. That is, if run on a subset of the 
 * input, the order in which they are executed must not matter. If there is the 
 * need for a non-transitive query, please see:
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=261403
 * <p>
 * Users of this query must call {@link #perform(Iterator, Collector)} to compute 
 * the results. <P>
 * This class may be subclassed by clients. Subclasses should specify the type
 * of object they support querying on. Subclasses are also encouraged to clearly
 * specify their match algorithm, and expose the parameters involved in the match
 * computation, to allow {@link IQueryable} implementations to optimize their
 * execution of the query. <P>
 * 
 */
public abstract class ContextQuery implements IQuery {

	/**
	 * Evaluates the query for a specific input.  
	 * 
	 * @param iterator The elements for which to evaluate the query on
	 * @param result A collector to collect the results.  For each element accepted 
	 * by the query,{@link Collector#accept(Object)} must be called.
	 * @return The results of the query.  The collector returned must be
	 * the collector passed in.
	 */
	public abstract Collector perform(Iterator iterator, Collector result);

	/**
	 * Gets the ID for this Query. 
	 */
	public String getId() {
		return QueryHelpers.getId(this);
	}

	/**
	 * Gets a particular property of the query.
	 * @param property The property to retrieve 
	 */
	public Object getProperty(String property) {
		return QueryHelpers.getProperty(this, property);
	}
}
