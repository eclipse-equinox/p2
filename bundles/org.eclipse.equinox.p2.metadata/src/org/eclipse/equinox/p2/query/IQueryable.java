/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - Add IQueryable.contains(T) method and implement overrides where suitable 
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;

/**
 * An IQueryable contains objects, and is able to perform queries on those
 * objects.
 * <p>
 * This interface may be implemented by clients.
 * 
 * @since 2.0
 */
public interface IQueryable<T> {
	/**
	 * Performs a query, passing any objects that satisfy the query to the provided
	 * collector.
	 * <p>
	 * This method is long-running; progress and cancellation are provided by the
	 * given progress monitor.
	 * </p>
	 * 
	 * @param query   The query to perform
	 * @param monitor a progress monitor, or <code>null</code> if progress reporting
	 *                is not desired
	 * @return The collector argument
	 */
	public IQueryResult<T> query(IQuery<T> query, IProgressMonitor monitor);

	/**
	 * Returns true if this queryable contains the given element, else false.
	 *
	 * @param element the element to query for
	 * @return true if the given element is found in this queryable
	 * @since 2.8
	 */
	default boolean contains(T element) {
		return !query(new IQuery<>() {

			@Override
			public IQueryResult<T> perform(Iterator<T> iterator) {
				while (iterator.hasNext()) {
					T t = iterator.next();
					if (Objects.equals(t, element)) {
						return new CollectionResult<>(List.of(t));
					}
				}
				return new CollectionResult<>(List.of());
			}

			@Override
			public IExpression getExpression() {
				return ExpressionUtil.TRUE_EXPRESSION;
			}
		}, null).isEmpty();
	}
}
