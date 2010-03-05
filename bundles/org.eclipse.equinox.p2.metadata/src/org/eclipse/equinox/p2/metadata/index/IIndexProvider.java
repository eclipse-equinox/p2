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

import java.util.Iterator;

/**
 * An index provider is typically an IQueryable.
 * @since 2.0
 */
public interface IIndexProvider<T> {
	/**
	 * Return an index optimized for producing candidates based on values
	 * for a <code>memberName</code> that denotes a member of the index
	 * type. 
	 * @param memberName A member of type <code>T</code>.
	 * @return An index or <code>null</code> if this provider does not support
	 * this index.
	 */
	IIndex<T> getIndex(String memberName);

	/**
	 * Return the iterator that delivers all rows that the target query should
	 * consider. This is used when no index can be found for any possible
	 * member.
	 * @return An iterator. Possibly empty but never <code>null</code>.
	 */
	Iterator<T> everything();

	/**
	 * Returns a property that this index manages on behalf of a <code>client</code> object.
	 * Examples of this is the properties that a profile manages for installable
	 * units.
	 * @param client The client for which the property is managed. Typically an IU.
	 * @param memberName The name of the managed properties, i.e. &quot;profileProperties&quot;
	 * @param key The property key
	 * @return The managed property value or <code>null</code> if no value could be found.
	 */
	Object getManagedProperty(Object client, String memberName, Object key);
}
