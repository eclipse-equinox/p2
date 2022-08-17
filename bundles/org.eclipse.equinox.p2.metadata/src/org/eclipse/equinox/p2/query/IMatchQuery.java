/******************************************************************************* 
* Copyright (c) 2009, 2017 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.query;

/**
 * A query in which the elements can be evaluated by calling isMatch on. Each
 * element can be evaluated independently of all other elements. Match queries
 * can be evaluated in parallel as each call {@link #isMatch(Object)} is
 * mutually
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients creating custom queries must extend
 *              {@link ExpressionMatchQuery}.
 * @deprecated If possible, use one of the predefined queries in
 *             {@link QueryUtil} or use the
 *             {@link QueryUtil#createMatchQuery(String, Object...)} to create a
 *             custom expression based query. If the query cannot be expressed
 *             using the p2QL, then use a predefined or custom expression query
 *             as a first filter (in worst case, use
 *             {@link QueryUtil#createIUAnyQuery()}) and then provide further
 *             filtering like so:
 * 
 *             <pre>
 *             for (iter = queryable.query(someExpressionQuery).iterator(); iter.hasNext();) {
 *             	// do your match here
 *             }
 *             </pre>
 * 
 * @since 2.0
 */
@Deprecated
public interface IMatchQuery<T> extends IQuery<T> {

	/**
	 * Returns whether the given object satisfies the parameters of this query.
	 * 
	 * @param candidate The object to perform the query against
	 * @return <code>true</code> if the unit satisfies the parameters of this query,
	 *         and <code>false</code> otherwise
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 2.0
	 */
	public boolean isMatch(T candidate);
}
