/*******************************************************************************
 * Copyright (c) 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.index;

import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * An extension of the {@link IQuery} that allows use of indexes.
 * @since 2.0
 */
public interface IQueryWithIndex<T> extends IQuery<T> {
	/**
	 * Evaluates the query using the <code>indexProvider</code>. The query
	 * is first analyzed for index candidates (typically expressions like
	 * id == &lt;some value&gt;) and if possible, indexes returned by
	 * {@link IIndexProvider#getIndex(String)} will be used
	 * in place of the iterator returned by {@link IIndexProvider#everything()}. 
	 * 
	 * @param indexProvider The provider of the material to evaluate the query on
	 * @return The results of the query.
	 */
	IQueryResult<T> perform(IIndexProvider<T> indexProvider);
}
