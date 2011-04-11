/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core;

import java.util.HashMap;
import java.util.Map;

/**
 * An object pool backed by strong references.  Objects stored in this pool
 * will not be garbage collected as they refer to themselves.  The client is responsible for 
 * nulling all references to the pool instance when it is no longer needed so that
 * the contained objects can be garbage collected.  
 * <p>
 * If a long lived, memory managed pool is required use {@link org.eclipse.equinox.p2.core.WeakPool}.
 * </p>
 * @since 2.1
 */
public class StrongPool<T> implements IPool<T> {
	private Map<T, T> pool = new HashMap<T, T>();

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.core.IPool#add(T)
	 */
	public T add(T newObject) {
		if (newObject == null) {
			return null;
		}

		T reference = pool.get(newObject);
		if (reference == null) {
			pool.put(newObject, newObject);
			return newObject;
		}
		return reference;
	}
}
