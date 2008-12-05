/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.query;

import java.util.Iterator;

/**
 * The superclass of all queries that can be performed on an {@link IQueryable}.
 * <p>
 * This class may be subclassed by clients. Subclasses should specify the type
 * of object they support querying on. Subclasses are also encouraged to clearly
 * specify their match algorithm, and expose the parameters involved in the match
 * computation, to allow {@link IQueryable} implementations to optimize their
 * execution of the query.
 */
public abstract class Query {
	/**
	 * Creates a new query.
	 */
	public Query() {
		super();
	}

	/**
	 * Returns whether the given object satisfies the parameters of this query.
	 * 
	 * @param candidate The object to perform the query against
	 * @return <code>true</code> if the unit satisfies the parameters
	 * of this query, and <code>false</code> otherwise
	 */
	public abstract boolean isMatch(Object candidate);

	/**
	 * Performs this query on the given iterator, passing all objects in the iterator 
	 * that match the criteria of this query to the given result.
	 */
	public Collector perform(Iterator iterator, Collector result) {
		while (iterator.hasNext()) {
			Object candidate = iterator.next();
			if (isMatch(candidate))
				if (!result.accept(candidate))
					break;
		}
		return result;
	}
}
