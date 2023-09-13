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
 *******************************************************************************/
package org.eclipse.equinox.p2.query;

import org.eclipse.core.runtime.IProgressMonitor;

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
}
