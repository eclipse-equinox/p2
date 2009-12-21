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
package org.eclipse.equinox.internal.provisional.p2.metadata.query;

import java.lang.reflect.Array;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;

/**
 * A query that combines a group of sub-queries.<P>
 * 
 * In a CompoundQuery each sub-query is executed and the results are combined using
 * either logical AND or logical OR operations. <P>
 * 
 * Clients are expected to call {@link CompoundQuery#createCompoundQuery(IQuery[], boolean)}
 * to create a concrete instance of a CompoundQuery.  If all Queries are instances of 
 * {@link IMatchQuery} then the resulting compound query will be a MatchCompoundQuery, otherwise the
 * resulting compound query will be a {@link ContextQuery}.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class CompoundQuery implements IQuery, ICompositeQuery {
	protected IQuery[] queries;
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
	public static CompoundQuery createCompoundQuery(IQuery[] queries, boolean and) {
		if (isMatchQueries(queries)) {
			return new CompoundQuery.MatchCompoundQuery(queries, and);
		}
		return new CompoundQuery.ContextCompoundQuery(queries, and);
	}

	/**
	 * Returns the queries that make up this compound query
	 */
	public IQuery[] getQueries() {
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

	protected CompoundQuery(IQuery[] queries, boolean and) {
		this.queries = queries;
		this.and = and;
	}

	/**
	 * @param queries
	 */
	private static boolean isMatchQueries(IQuery[] queries) {
		for (int i = 0; i < queries.length; i++) {
			if (!(queries[i] instanceof IMatchQuery)) {
				return false;
			}
		}
		return true;
	}

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

	/**
	 * The compound query instantiated when all queries are Match Queries.
	 */
	private static class MatchCompoundQuery extends CompoundQuery implements IMatchQuery {

		protected MatchCompoundQuery(IQuery[] queries, boolean and) {
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
		public final IQueryResult perform(Iterator iterator) {
			prePerform();
			Collector result = new Collector();
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

		protected ContextCompoundQuery(IQuery[] queries, boolean and) {
			super(queries, and);
		}

		/*
		 * A collector that takes the set to puts the elements in.
		 */
		static class SetCollector implements IQueryResult {
			private final Set s;

			public SetCollector(Set s) {
				this.s = s;
			}

			public boolean isEmpty() {
				return s.isEmpty();
			}

			public Iterator iterator() {
				return s.iterator();
			}

			public Object[] toArray(Class clazz) {
				return s.toArray((Object[]) Array.newInstance(clazz, s.size()));
			}

			public IQueryResult query(IQuery query, IProgressMonitor monitor) {
				return query.perform(iterator());
			}

			public Set toSet() {
				return new HashSet(s);
			}

			public Set unmodifiableSet() {
				return Collections.unmodifiableSet(s);
			}
		}

		public IQueryResult perform(Iterator iterator) {
			if (queries.length < 1)
				return Collector.EMPTY_COLLECTOR;

			if (queries.length == 1)
				return queries[0].perform(iterator);

			Collection data = new ArrayList();
			while (iterator.hasNext())
				data.add(iterator.next());

			Set result;
			if (isAnd()) {
				result = queries[0].perform(data.iterator()).unmodifiableSet();
				for (int i = 1; i < queries.length && result.size() > 0; i++) {
					HashSet retained = new HashSet();
					Iterator itor = queries[i].perform(data.iterator()).iterator();
					while (itor.hasNext()) {
						Object nxt = itor.next();
						if (result.contains(nxt))
							retained.add(nxt);
					}
					result = retained;
				}
			} else {
				result = queries[0].perform(data.iterator()).toSet();
				for (int i = 1; i < queries.length; i++) {
					Iterator itor = queries[i].perform(data.iterator()).iterator();
					while (itor.hasNext()) {
						result.add(itor.next());
					}
				}
			}
			return new SetCollector(result);
		}
	}
}
