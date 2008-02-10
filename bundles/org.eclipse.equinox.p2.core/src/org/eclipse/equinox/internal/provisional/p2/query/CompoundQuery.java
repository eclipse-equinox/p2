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
package org.eclipse.equinox.internal.provisional.p2.query;

/**
 * A query that combines a group of other queries.
 */
public class CompoundQuery extends Query {
	private boolean and;
	private Query[] queries;

	/**
	 * Creates a compound query that combines the given queries. The queries
	 * will be performed by the compound query in the given order. This method
	 * might not perform all queries if it can determine the result of the compound
	 * expression without doing so.
	 * 
	 * @param queries The queries to perform
	 * @param and <code>true</code> if this query represents a logical 'and', and
	 * <code>false</code> if this query represents a logical 'or'.
	 */
	public CompoundQuery(Query[] queries, boolean and) {
		this.queries = queries;
		this.and = and;
	}

	public Query[] getQueries() {
		return queries;
	}

	/**
	 * Returns whether this compound query combines its queries with a logical
	 * 'and' or 'or'.
	 * @return <code>true</code> if this query represents a logical 'and', and
	 * <code>false</code> if this query represents a logical 'or'.
	 */
	public boolean isAnd() {
		return and;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query2.Query#isMatch(java.lang.Object)
	 */
	public boolean isMatch(Object candidate) {
		for (int i = 0; i < queries.length; i++) {
			boolean valid = queries[i].isMatch(candidate);
			// if we are OR'ing then the first time we find a requirement that is met, return success
			if (valid && !and)
				return true;
			// if we are AND'ing then the first time we find a requirement that is NOT met, return failure
			if (!valid && and)
				return false;
		}
		// if we get past the requirements check and we are AND'ing then return true 
		// since all requirements must have been met.  If we are OR'ing then return false 
		// since none of the requirements were met.
		return and;
	}
}
