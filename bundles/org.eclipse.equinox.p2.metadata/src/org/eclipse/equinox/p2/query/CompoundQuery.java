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
 *     Cloudsmith Inc. - added index capabilities
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.*;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;
import org.eclipse.equinox.p2.metadata.index.IQueryWithIndex;

/**
 * A query that combines a group of sub-queries.<P>
 * 
 * In a CompoundQuery each sub-query is executed over the entire input and the 
 * results are combined using either logical AND or logical OR operations. <P>
 * 
 * Clients are expected to call {@link CompoundQuery#createCompoundQuery(IQuery[], boolean)}
 * to create a concrete instance of a CompoundQuery.  If all Queries are instances of 
 * {@link IMatchQuery} then the resulting compound query will be an {@link IMatchQuery}, otherwise the
 * resulting query will be a {@link ContextQuery}.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public abstract class CompoundQuery<T> implements ICompositeQuery<T>, IQueryWithIndex<T> {
	IQuery<T>[] queries;
	boolean and;

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
	public static <T> IQuery<T> createCompoundQuery(IQuery<T>[] queries, boolean and) {
		if (isMatchQueries(queries)) {
			return new CompoundQuery.MatchCompoundQuery<T>(queries, and);
		}
		return new CompoundQuery.ContextCompoundQuery<T>(queries, and);
	}

	@SuppressWarnings("unchecked")
	public static <T> IQuery<T> createCompoundQuery(IQuery<T> query1, IQuery<T> query2, boolean and) {
		return createCompoundQuery(new IQuery[] {query1, query2}, and);
	}

	/**
	 * Returns the queries that make up this compound query
	 * 
	 * @return the queries that make up this compound query
	 */
	public List<IQuery<T>> getQueries() {
		return Arrays.asList(queries);
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

	protected CompoundQuery(IQuery<T>[] queries, boolean and) {
		this.queries = queries;
		this.and = and;
	}

	/**
	 * @param queries
	 */
	private static boolean isMatchQueries(IQuery<?>[] queries) {
		for (int i = 0; i < queries.length; i++) {
			if (!(queries[i] instanceof IMatchQuery<?>)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The compound query instantiated when all queries are Match Queries.
	 */
	private static class MatchCompoundQuery<T> extends CompoundQuery<T> implements IMatchQuery<T> {

		protected MatchCompoundQuery(IQuery<T>[] queries, boolean and) {
			super(queries, and);
		}

		public boolean isMatch(T candidate) {
			for (int i = 0; i < queries.length; i++) {
				boolean valid = ((IMatchQuery<T>) queries[i]).isMatch(candidate);
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
		public final IQueryResult<T> perform(Iterator<T> iterator) {
			Collector<T> result = new Collector<T>();
			while (iterator.hasNext()) {
				T candidate = iterator.next();
				if (isMatch(candidate))
					if (!result.accept(candidate))
						break;
			}
			return result;
		}
	}

	/**
	 * The compound query instantiated when any of the queries are not 
	 * match queries.
	 */
	private static class ContextCompoundQuery<T> extends CompoundQuery<T> {

		protected ContextCompoundQuery(IQuery<T>[] queries, boolean and) {
			super(queries, and);
		}

		public IQueryResult<T> perform(Iterator<T> iterator) {
			if (queries.length < 1)
				return Collector.emptyCollector();

			if (queries.length == 1)
				return queries[0].perform(iterator);

			Collection<T> data = new ArrayList<T>();
			while (iterator.hasNext())
				data.add(iterator.next());

			Set<T> result;
			if (isAnd()) {
				result = queries[0].perform(data.iterator()).unmodifiableSet();
				for (int i = 1; i < queries.length && result.size() > 0; i++) {
					HashSet<T> retained = new HashSet<T>();
					Iterator<T> itor = queries[i].perform(data.iterator()).iterator();
					while (itor.hasNext()) {
						T nxt = itor.next();
						if (result.contains(nxt))
							retained.add(nxt);
					}
					result = retained;
				}
			} else {
				result = queries[0].perform(data.iterator()).toSet();
				for (int i = 1; i < queries.length; i++) {
					Iterator<T> itor = queries[i].perform(data.iterator()).iterator();
					while (itor.hasNext()) {
						result.add(itor.next());
					}
				}
			}
			return new CollectionResult<T>(result);
		}
	}

	public IExpression getExpression() {
		return null;
	}

	public IQueryResult<T> perform(IIndexProvider<T> indexProvider) {
		if (queries.length < 1)
			return Collector.emptyCollector();

		if (queries.length == 1)
			return doPerform(queries[0], indexProvider);

		Set<T> result;
		if (isAnd()) {
			result = doPerform(queries[0], indexProvider).unmodifiableSet();
			for (int i = 1; i < queries.length && result.size() > 0; i++) {
				HashSet<T> retained = new HashSet<T>();
				Iterator<T> itor = doPerform(queries[i], indexProvider).iterator();
				while (itor.hasNext()) {
					T nxt = itor.next();
					if (result.contains(nxt))
						retained.add(nxt);
				}
				result = retained;
			}
		} else {
			result = doPerform(queries[0], indexProvider).toSet();
			for (int i = 1; i < queries.length; i++) {
				Iterator<T> itor = doPerform(queries[i], indexProvider).iterator();
				while (itor.hasNext()) {
					result.add(itor.next());
				}
			}
		}
		return new CollectionResult<T>(result);
	}

	private static <Q> IQueryResult<Q> doPerform(IQuery<Q> first, IIndexProvider<Q> indexProvider) {
		return first instanceof IQueryWithIndex<?> ? ((IQueryWithIndex<Q>) first).perform(indexProvider) : first.perform(indexProvider.everything());
	}
}
