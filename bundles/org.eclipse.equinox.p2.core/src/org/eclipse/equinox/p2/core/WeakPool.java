/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.p2.core;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * An object pool backed by weak references.  Objects stored in this pool
 * will be garbage collected once all strong references to the objects are broken.
 * <p>
 * Since {@link WeakReference} are not particularly light-weight, a client could use a {@link StrongPool}
 * if the pool will be short lived and explicitly nulled by the client.
 * </p>
 * @since 2.1
 */
public class WeakPool<T> implements IPool<T> {
	private final Map<T, WeakReference<T>> pool = new WeakHashMap<>();

	@Override
	public T add(T newObject) {
		if (newObject == null) {
			return null;
		}

		WeakReference<T> weakReference = pool.get(newObject);
		if (weakReference != null) {
			T reference = weakReference.get();
			if (reference != null) {
				return reference;
			}
		}
		pool.put(newObject, new WeakReference<>(newObject));
		return newObject;
	}
}
