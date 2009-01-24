/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.query;

import java.util.*;

/**
 * A query that combines a group of sub-queries.<P>
 * 
 * In a CompoundQuery each sub-query is executed and the results are combined using
 * either logical AND or logical OR operations. <P>
 * 
 * Clients are expected to call {@link CompoundQuery#createCompoundQuery(Query[], boolean)}
 * to create a concrete instance of a CompoundQuery.  If all Queries are instances of 
 * {@link IMatchQuery} then the resulting compound query will be a MatchCompoundQuery, otherwise the
 * resulting compound query will be a {@link ContextQuery}.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class CompoundQuery implements Query {
	protected Query[] queries;
	protected boolean and;

	/**
	 * Creates a compound query that combines the given queries. The queries
	 * will be performed by the compound query in the given order. This method
	 * might not perform all queries if it can determine the result of the compound
	 * expression without doing so.
	 * 
	 * If all the queries are instances of {@link IMatchQuery} then the resulting
	 * compound query will be an instance of IMatchQuery, otherwise the resulting
	 * compound query will be a context query.
	 * 
	 * @param queries The queries to perform
	 * @param and <code>true</code> if this query represents a logical 'and', and
	 * <code>false</code> if this query represents a logical 'or'.
	 */
	public static CompoundQuery createCompoundQuery(Query[] queries, boolean and) {
		if (isMatchQueries(queries)) {
			return new CompoundQuery.MatchCompoundQuery(queries, and);
		}
		return new CompoundQuery.ContextCompoundQuery(queries, and);
	}

	/**
	 * Returns the queries that make up this compound query
	 */
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

	protected CompoundQuery(Query[] queries, boolean and) {
		this.queries = queries;
		this.and = and;
	}

	/**
	 * @param queries
	 */
	private static boolean isMatchQueries(Query[] queries) {
		for (int i = 0; i < queries.length; i++) {
			if (!(queries[i] instanceof IMatchQuery)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The compound query instantiated when all queries are Match Queries.
	 */
	private static class MatchCompoundQuery extends CompoundQuery implements IMatchQuery {

		protected MatchCompoundQuery(Query[] queries, boolean and) {
			super(queries, and);
		}

		public boolean isMatch(Object candidate) {
			for (int i = 0; i < queries.length; i++) {
				boolean valid = ((IMatchQuery) queries[i]).isMatch(candidate);
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

		/**
		 * Performs this query on the given iterator, passing all objects in the iterator 
		 * that match the criteria of this query to the given result.
		 */
		public final Collector perform(Iterator iterator, Collector result) {
			prePerform();
			try {
				while (iterator.hasNext()) {
					Object candidate = iterator.next();
					if (isMatch(candidate))
						if (!result.accept(candidate))
							break;
				}
			} finally {
				postPerform();
			}
			return result;
		}

		public void prePerform() {
			for (int i = 0; i < queries.length; i++) {
				((IMatchQuery) queries[i]).prePerform();
			}
		}

		public void postPerform() {
			for (int i = 0; i < queries.length; i++) {
				((IMatchQuery) queries[i]).postPerform();
			}
		}
	}

	/**
	 * The compound query instantiated when any of the queries are not 
	 * match queries.
	 */
	private static class ContextCompoundQuery extends CompoundQuery {

		protected ContextCompoundQuery(Query[] queries, boolean and) {
			super(queries, and);
		}

		/*
		 * A collector that takes the set to puts the elements in.
		 */
		class SetCollector extends Collector {
			Set s = null;

			public SetCollector(Set s) {
				this.s = s;
			}

			public boolean accept(Object object) {
				s.add(object);
				return true;
			}
		}

		public Collector perform(Iterator iterator, Collector result) {
			if (queries.length < 1)
				return result;

			Collection data = new LinkedList();

			while (iterator.hasNext()) {
				data.add(iterator.next());
			}

			Set[] resultSets = new Set[queries.length];
			for (int i = 0; i < queries.length; i++) {
				resultSets[i] = new HashSet();
				queries[i].perform(data.iterator(), new SetCollector(resultSets[i]));
			}

			Set set = resultSets[0];
			for (int i = 1; i < resultSets.length; i++) {
				if (isAnd())
					set.retainAll(resultSets[i]);
				else
					set.addAll(resultSets[i]);
			}

			Iterator resultIterator = set.iterator();
			boolean gatherResults = true;
			while (resultIterator.hasNext() && gatherResults)
				gatherResults = result.accept(resultIterator.next());
			return result;
		}
	}
}
