/*******************************************************************************
 * Copyright (c) 2010, 2023 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.expression.ContextExpression;
import org.eclipse.equinox.internal.p2.metadata.expression.Expression.VariableFinder;
import org.eclipse.equinox.internal.p2.metadata.expression.ExpressionFactory;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IContextExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpressionFactory;

/**
 * Helper class for query related tasks.
 * 
 * @since 2.0
 */
public class QueryUtil {

	public static final IQuery<IInstallableUnit> ALL_UNITS = QueryUtil.createMatchQuery(ExpressionUtil.TRUE_EXPRESSION);

	public static final String ANY = "*"; //$NON-NLS-1$

	public static final IQuery<IInstallableUnit> NO_UNITS = QueryUtil.createQuery("limit(0)"); //$NON-NLS-1$

	public static final String PROP_TYPE_CATEGORY = "org.eclipse.equinox.p2.type.category"; //$NON-NLS-1$

	public static final String PROP_TYPE_GROUP = "org.eclipse.equinox.p2.type.group"; //$NON-NLS-1$

	public static final String PROP_TYPE_PATCH = "org.eclipse.equinox.p2.type.patch"; //$NON-NLS-1$

	private static final IExpression matchesRequirementsExpression = ExpressionUtil.parse("$0.exists(r | this ~= r)"); //$NON-NLS-1$

	private static final IExpression matchIU_ID = ExpressionUtil.parse("id == $0"); //$NON-NLS-1$
	private static final IExpression matchIU_IDAndRange = ExpressionUtil.parse("id == $0 && version ~= $1"); //$NON-NLS-1$
	private static final IExpression matchIU_IDAndVersion = ExpressionUtil.parse("id == $0 && version == $1"); //$NON-NLS-1$
	private static final IExpression matchIU_Range = ExpressionUtil.parse("version ~= $0"); //$NON-NLS-1$
	private static final IExpression matchIU_Version = ExpressionUtil.parse("version == $0"); //$NON-NLS-1$
	private static final IExpression matchIU_propAny = ExpressionUtil.parse("properties[$0] != null"); //$NON-NLS-1$
	private static final IExpression matchIU_propNull = ExpressionUtil.parse("properties[$0] == null"); //$NON-NLS-1$
	private static final IExpression matchIU_propTrue = ExpressionUtil.parse("properties[$0] == true"); //$NON-NLS-1$
	private static final IExpression matchIU_propValue = ExpressionUtil.parse("properties[$0] == $1"); //$NON-NLS-1$

	/**
	 * Creates a queryable that combines the given collection of input queryables
	 * 
	 * @param queryables The collection of queryables to be combined
	 */
	public static <T> IQueryable<T> compoundQueryable(Collection<? extends IQueryable<T>> queryables) {
		// don't suppress the warning as it will cause warnings in the official build
		// see bug 423628. Write this without unchecked conversion.
		return new CompoundQueryable<>(queryables);
	}

	/**
	 * Creates a queryable that combines the two provided input queryables
	 * 
	 * @param query1 The first queryable
	 * @param query2 The second queryable
	 */
	public static <T> IQueryable<T> compoundQueryable(IQueryable<T> query1, IQueryable<T> query2) {
		return new CompoundQueryable<>(List.of(query1, query2));
	}

	/**
	 * Creates a compound query that combines the given queries. If all queries
	 * are candidate match queries, then the queries will be concatenated as a
	 * boolean 'and' or a boolean 'or' expression depending on the <code>and</code>
	 * flag. If at least one query is a full query, all queries will instead be evaluated
	 * using intersection when <code>and</code> is <code>true</code> or a union.
	 * 
	 * @param queries The queries to perform
	 * @param and <code>true</code> if this query represents an intersection or a
	 * logical 'and', and <code>false</code> if this query represents a union or
	 * a logical 'or'.
	 * @return A compound query
	 */
	@SuppressWarnings("unchecked")
	public static <T> IQuery<T> createCompoundQuery(Collection<? extends IQuery<? extends T>> queries, boolean and) {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		int top = queries.size();
		if (top == 1)
			return (IQuery<T>) queries.iterator().next();

		Class<? extends T> elementClass = (Class<T>) Object.class;
		if (top == 0)
			return QueryUtil.createMatchQuery(elementClass, ExpressionUtil.TRUE_EXPRESSION);

		IExpression[] expressions = new IExpression[top];
		boolean justBooleans = true;
		boolean justContexts = true;
		int idx = 0;
		for (IQuery<? extends T> query : queries) {
			if (query instanceof IMatchQuery<?>)
				justContexts = false;
			else
				justBooleans = false;

			IExpression expr = query.getExpression();
			if (expr == null)
				expr = factory.toExpression(query);

			Class<? extends T> ec = ExpressionQuery.getElementClass(query);
			if (elementClass == null)
				elementClass = ec;
			else if (elementClass != ec) {
				if (elementClass.isAssignableFrom(ec)) {
					if (and)
						// Use most restrictive class
						elementClass = ec;
				} else if (ec.isAssignableFrom(elementClass)) {
					if (!and)
						// Use least restrictive class
						elementClass = ec;
				}
			}
			expressions[idx++] = expr;
		}

		if (justBooleans) {
			IExpression compound = and ? factory.and(expressions) : factory.or(expressions);
			return QueryUtil.createMatchQuery(elementClass, compound);
		}

		if (!justContexts) {
			// Mix of boolean queries and context queries. All must be converted into context then.
			for (idx = 0; idx < expressions.length; ++idx)
				expressions[idx] = makeContextExpression(factory, expressions[idx]);
		}

		IExpression compound = expressions[0];
		for (idx = 1; idx < expressions.length; ++idx)
			compound = and ? factory.intersect(compound, expressions[idx]) : factory.union(compound, expressions[idx]);
		return QueryUtil.createQuery(elementClass, compound);
	}

	/**
	 * Creates a compound query that combines the two queries. If both queries
	 * are candidate match queries, then the queries will be concatenated as a
	 * boolean 'and' or a boolean 'or' expression depending on the <code>and</code>
	 * flag. If at least one query is a full query, all queries will instead be evaluated
	 * using intersection when <code>and</code> is <code>true</code> or a union.
	 * 
	 * @param query1 the first query
	 * @param query2 the second query
	 * @param and <code>true</code> if this query represents an intersection or a
	 * logical 'and', and <code>false</code> if this query represents a union or
	 * a logical 'or'.
	 * @return A compound query
	 */
	public static <T> IQuery<T> createCompoundQuery(IQuery<? extends T> query1, IQuery<T> query2, boolean and) {
		ArrayList<IQuery<? extends T>> queries = new ArrayList<>(2);
		queries.add(query1);
		queries.add(query2);
		return createCompoundQuery(queries, and);
	}

	/**
	 * Returns a query that matches all {@link InstallableUnit} elements
	 */
	public static IQuery<IInstallableUnit> createIUAnyQuery() {
		return ALL_UNITS;
	}

	/**
	 * Creates a new query that will return the members of the
	 * given <code>category</code>.  If the specified {@link IInstallableUnit} 
	 * is not a category, then no installable unit will satisfy the query. 
	 * 
	 * @param category The category
	 * @return A query that returns category members
	 */
	public static IQuery<IInstallableUnit> createIUCategoryMemberQuery(IInstallableUnit category) {
		if (QueryUtil.isCategory(category))
			return QueryUtil.createMatchQuery(matchesRequirementsExpression, category.getRequirements());
		return NO_UNITS;
	}

	/**
	 * Creates a query matching every {@link IInstallableUnit} that is a category.
	 * @return The query that matches categories
	 * @since 2.0 
	 */
	public static IQuery<IInstallableUnit> createIUCategoryQuery() {
		return createIUPropertyQuery(QueryUtil.PROP_TYPE_CATEGORY, Boolean.TRUE.toString());
	}

	/**
	 * Creates a query matching every {@link IInstallableUnit} that is a group. 
	 * @return a query that matches all groups
	 */
	public static IQuery<IInstallableUnit> createIUGroupQuery() {
		return createIUPropertyQuery(PROP_TYPE_GROUP, Boolean.TRUE.toString());
	}

	/**
	 * Creates an {@link IInstallableUnit} that will match all patches.
	 * @return The created query
	 */
	public static IQuery<IInstallableUnit> createIUPatchQuery() {
		return createIUPropertyQuery(QueryUtil.PROP_TYPE_PATCH, Boolean.TRUE.toString());
	}

	/**
	 * Creates an {@link IInstallableUnit} that will match all products.
	 * @return The created query
	 * @since 2.2
	 */
	public static IQuery<IInstallableUnit> createIUProductQuery() {
		return createIUPropertyQuery(MetadataFactory.InstallableUnitDescription.PROP_TYPE_PRODUCT, Boolean.TRUE.toString());
	}

	/**
	 * Creates a query that searches for {@link IInstallableUnit} instances that have
	 * a property whose value matches the provided value.  If no property name is 
	 * specified, then all {@link IInstallableUnit} instances are accepted.
	 * @param propertyName The key of the property to match or <code>null</code> to match all
	 * @param propertyValue The value of the property. Can be {@link #ANY} to match all values
	 * except <code>null</code>
	 * @return The query matching properties
	 */
	public static IQuery<IInstallableUnit> createIUPropertyQuery(String propertyName, String propertyValue) {
		if (propertyName == null)
			return QueryUtil.createMatchQuery(ExpressionUtil.TRUE_EXPRESSION);
		if (propertyValue == null)
			return QueryUtil.createMatchQuery(matchIU_propNull, propertyName);
		if (ANY.equals(propertyValue))
			return QueryUtil.createMatchQuery(matchIU_propAny, propertyName);
		if (Boolean.parseBoolean(propertyValue))
			return QueryUtil.createMatchQuery(matchIU_propTrue, propertyName);
		return QueryUtil.createMatchQuery(matchIU_propValue, propertyName, propertyValue);
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id and version.
	 * 
	 * @param versionedId The precise id/version combination that a matching unit must have
	 * @return a query that matches IU's by id and version
	 */
	public static IQuery<IInstallableUnit> createIUQuery(IVersionedId versionedId) {
		return createIUQuery(versionedId.getId(), versionedId.getVersion());
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id, regardless of version.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match any id
	 * @return a query that matches IU's by id
	 */
	public static IQuery<IInstallableUnit> createIUQuery(String id) {
		return id == null ? ALL_UNITS : QueryUtil.createMatchQuery(matchIU_ID, id);
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id and version.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match any id
	 * @param version The precise version that a matching unit must have or <code>null</code>
	 * to match any version
	 * @return a query that matches IU's by id and version
	 */
	public static IQuery<IInstallableUnit> createIUQuery(String id, Version version) {
		if (version == null || version.equals(Version.emptyVersion))
			return createIUQuery(id);
		if (id == null)
			return QueryUtil.createMatchQuery(matchIU_Version, version);
		return QueryUtil.createMatchQuery(matchIU_IDAndVersion, id, version);
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id, and whose version falls in the provided range.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match any id
	 * @param range The version range to match or <code>null</code> to match any range.
	 * @return a query that matches IU's by id and range
	 */
	public static IQuery<IInstallableUnit> createIUQuery(String id, VersionRange range) {
		if (range == null || range.equals(VersionRange.emptyRange))
			return createIUQuery(id);
		if (id == null)
			return QueryUtil.createMatchQuery(matchIU_Range, range);
		return QueryUtil.createMatchQuery(matchIU_IDAndRange, id, range);
	}

	/**
	 * Creates a query that returns the latest version for each unique id of an {@link IVersionedId}.  
	 * All other elements are discarded.
	 * @return A query matching the latest version for each id.
	 */
	public static IQuery<IInstallableUnit> createLatestIUQuery() {
		return QueryUtil.createQuery(ExpressionUtil.getFactory().latest(ExpressionFactory.EVERYTHING));
	}

	/**
	 * Creates a query that returns the latest version for each unique id of an {@link IVersionedId}
	 * from the collection produced by <code>query</code>. 
	 * All other elements are discarded.
	 * @param query The query that precedes the latest query when evaluating.
	 * @return A query matching the latest version for each id.
	 */
	public static <T extends IVersionedId> IQuery<T> createLatestQuery(IQuery<T> query) {
		IContextExpression<T> ctxExpr = ExpressionQuery.createExpression(query);
		IExpressionFactory factory = ExpressionUtil.getFactory();
		@SuppressWarnings("unchecked")
		Class<T> elementClass = (Class<T>) IVersionedId.class;
		return QueryUtil.createQuery(elementClass, factory.latest(((ContextExpression<?>) ctxExpr).operand), ctxExpr.getParameters());
	}

	/**
	 * Creates a limit query that can be used to limit the number of query results returned.  Once
	 * the limit is reached, the query is terminated.
	 * @param query The query that should be limited
	 * @param limit A positive integer denoting the limit
	 * @return A limited query
	 * @since 2.0
	 */
	public static <T> IQuery<T> createLimitQuery(IQuery<T> query, int limit) {
		IContextExpression<T> ctxExpr = ExpressionQuery.createExpression(query);
		IExpressionFactory factory = ExpressionUtil.getFactory();
		return QueryUtil.createQuery(ExpressionQuery.getElementClass(query), factory.limit(((ContextExpression<T>) ctxExpr).operand, limit), ctxExpr.getParameters());
	}

	/**
	 * Creates an {@link IInstallableUnit} query that will iterate over all candidates and discriminate by
	 * applying the boolean <code>matchExpression</code> on each candidate.
	 * @param matchExpression The boolean expression used for filtering one candidate
	 * @param parameters Values for parameter substitution
	 * @return The created query
	 */
	public static IQuery<IInstallableUnit> createMatchQuery(IExpression matchExpression, Object... parameters) {
		return new ExpressionMatchQuery<>(IInstallableUnit.class, matchExpression, parameters);
	}

	/**
	 * Parses the <code>matchExpression</code> and creates an {@link IInstallableUnit} query that will
	 * iterate over all candidates and discriminate by applying the boolean <code>matchExpression</code>
	 * on each candidate.
	 * @param matchExpression The boolean expression used for filtering one candidate
	 * @param parameters Values for parameter substitution
	 * @return The created query
	 */
	public static IQuery<IInstallableUnit> createMatchQuery(String matchExpression, Object... parameters) {
		return new ExpressionMatchQuery<>(IInstallableUnit.class, matchExpression, parameters);
	}

	/**
	 * Creates an query that will iterate over all candidates and discriminate all
	 * candidates that are not instances of <code>matchingClass</code> or for which
	 * the boolean <code>matchExpression</code> returns false.
	 * @param matchingClass The class that matching candidates must be an instance of
	 * @param matchExpression The boolean expression used for filtering one candidate
	 * @param parameters Values for parameter substitution
	 * @return The created query
	 */
	public static <T> IQuery<T> createMatchQuery(Class<? extends T> matchingClass, IExpression matchExpression, Object... parameters) {
		return new ExpressionMatchQuery<>(matchingClass, matchExpression, parameters);
	}

	/**
	 * Parses the <code>matchExpression</code> and creates an query that will iterate over
	 * all candidates and discriminate all candidates that are not instances of
	 * <code>matchingClass</code> or for which the boolean <code>matchExpression</code>
	 * returns false.
	 * 
	 * @param <T> The type of input object that the created query accepts
	 * @param matchingClass The class that matching candidates must be an instance of
	 * @param matchExpression The boolean expression used for filtering one candidate
	 * @param parameters Values for parameter substitution
	 * @return The created query
	 */
	public static <T> IQuery<T> createMatchQuery(Class<? extends T> matchingClass, String matchExpression, Object... parameters) {
		return new ExpressionMatchQuery<>(matchingClass, matchExpression, parameters);
	}

	/**
	 * <p>Creates a piped query based on the provided input queries.</p>
	 * <p>A pipe is a composite query in which each sub-query is executed in succession.  
	 * The results from the ith sub-query are piped as input into the i+1th sub-query. The
	 * query will short-circuit if any query returns an empty result set.</p>
	 * 
	 * @param queries the ordered list of queries to perform
	 * @return A query pipe
	 */
	@SuppressWarnings("unchecked")
	public static <T> IQuery<T> createPipeQuery(Collection<? extends IQuery<? extends T>> queries) {
		IExpressionFactory factory = ExpressionUtil.getFactory();
		int top = queries.size();
		IExpression[] expressions = new IExpression[top];
		int idx = 0;
		for (IQuery<? extends T> query : queries) {
			IExpression expr = query.getExpression();
			if (expr == null)
				expr = factory.toExpression(query);
			expressions[idx++] = expr;
		}
		IExpression pipe = factory.pipe(expressions);
		VariableFinder finder = new VariableFinder(ExpressionFactory.EVERYTHING);
		pipe.accept(finder);
		return finder.isFound() ? QueryUtil.createQuery((Class<T>) Object.class, pipe) : QueryUtil.createMatchQuery((Class<T>) Object.class, pipe);
	}

	/**
	 * <p>Creates a piped query based on the provided input queries.</p>
	 * <p>A pipe is a composite query in which each sub-query is executed in succession.  
	 * The results from the ith sub-query are piped as input into the i+1th sub-query. The
	 * query will short-circuit if any query returns an empty result set.</p>
	 *
	 * @param query1 the first query
	 * @param query2 the second query
	 * @return A query pipe
	 */
	public static <T> IQuery<T> createPipeQuery(IQuery<? extends T> query1, IQuery<? extends T> query2) {
		ArrayList<IQuery<? extends T>> queries = new ArrayList<>(2);
		queries.add(query1);
		queries.add(query2);
		return createPipeQuery(queries);
	}

	/**
	 * Creates an {@link IInstallableUnit} query based on an <code>expression</code> that
	 * uses all candidates as input.
	 * @param expression The query expression
	 * @param parameters Values for parameter substitution
	 * @return The created query
	 */
	public static IQuery<IInstallableUnit> createQuery(IExpression expression, Object... parameters) {
		return new ExpressionQuery<>(IInstallableUnit.class, expression, parameters);
	}

	/**
	 * Parses the <code>expression</code> and creates an {@link IInstallableUnit} query. The
	 * <code>expression</code> is expected to use all candidates as input.
	 * @param expression The query expression
	 * @param parameters Values for parameter substitution
	 * @return The created query
	 */
	public static IQuery<IInstallableUnit> createQuery(String expression, Object... parameters) {
		return new ExpressionQuery<>(IInstallableUnit.class, expression, parameters);
	}

	/**
	 * Creates a query that will limit the result to instances of the <code>matchingClass</code>. The
	 * <code>expression</code> is expected to use all candidates as input.
	 * @param matchingClass The class used as discriminator for the result
	 * @param expression The query expression
	 * @param parameters Values for parameter substitution
	 * @return The created query
	 */
	public static <T> IQuery<T> createQuery(Class<? extends T> matchingClass, IExpression expression, Object... parameters) {
		return new ExpressionQuery<>(matchingClass, expression, parameters);
	}

	/**
	 * Parses the <code>expression</code> and creates a query that will limit the result
	 * to instances of the <code>matchingClass</code>. The <code>expression</code> is expected
	 * to use all candidates as input.
	 * @param matchingClass The class used as discriminator for the result
	 * @param expression The query expression
	 * @param parameters Values for parameter substitution
	 * @return The created query
	 */
	public static <T> IQuery<T> createQuery(Class<? extends T> matchingClass, String expression, Object... parameters) {
		return new ExpressionQuery<>(matchingClass, expression, parameters);
	}

	/**
	 * Test if the {@link IInstallableUnit} is a category. 
	 * @param iu the element being tested.
	 * @return <code>true</code> if the parameter is a category.
	 */
	public static boolean isCategory(IInstallableUnit iu) {
		String value = iu.getProperty(PROP_TYPE_CATEGORY);
		if (value != null && (value.equals(Boolean.TRUE.toString())))
			return true;
		return false;
	}

	/**
	 * Test if the {@link IInstallableUnit} is a fragment. 
	 * @param iu the element being tested.
	 * @return <code>true</code> if the parameter is a fragment.
	 */
	public static boolean isFragment(IInstallableUnit iu) {
		return iu instanceof IInstallableUnitFragment;
	}

	/**
	 * Test if the {@link IInstallableUnit} is a group. 
	 * @param iu the element being tested.
	 * @return <code>true</code> if the parameter is a group.
	 */
	public static boolean isGroup(IInstallableUnit iu) {
		String value = iu.getProperty(PROP_TYPE_GROUP);
		if (value != null && (value.equals(Boolean.TRUE.toString())))
			return true;
		return false;
	}

	/**
	 * Test if the {@link IInstallableUnit} is a product. 
	 * @param iu the element being tested.
	 * @return <code>true</code> if the parameter is a group.
	 * @since 2.2
	 */
	public static boolean isProduct(IInstallableUnit iu) {
		String value = iu.getProperty(MetadataFactory.InstallableUnitDescription.PROP_TYPE_PRODUCT);
		if (value != null && (value.equals(Boolean.TRUE.toString())))
			return true;
		return false;
	}

	/**
	 * Test if the {@link IInstallableUnit} is a patch. 
	 * @param iu the element being tested.
	 * @return <code>true</code> if the parameter is a patch.
	 */
	public static boolean isPatch(IInstallableUnit iu) {
		String value = iu.getProperty(PROP_TYPE_PATCH);
		if (value != null && (value.equals(Boolean.TRUE.toString())))
			return true;
		return false;
	}

	private static IExpression makeContextExpression(IExpressionFactory factory, IExpression expr) {
		VariableFinder finder = new VariableFinder(ExpressionFactory.EVERYTHING);
		expr.accept(finder);
		if (!finder.isFound())
			expr = factory.select(ExpressionFactory.EVERYTHING, factory.lambda(ExpressionFactory.THIS, expr));
		return expr;
	}
}
