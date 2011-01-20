/*******************************************************************************
 *  Copyright (c) 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core;

/**
 * A Pool allows semantically equivalent objects to be shared.  To be useful, objects added to the pool should implement
 * a meaningful equals() method.
 * <p>
 * Care must be taken by users that object sharing is appropriate for their application.  It is easy
 * to "over share" objects leading to unexpected and difficult to debug behaviour.
 * </p><p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients. 
 * @since 2.1
 */
public interface IPool<T> {

	/** 
	 * Returns the first object from this pool which is equal to the given object.  If the pool 
	 * contains no such object then the object is added to the pool and returned.  If the object is <code>null</code>, 
	 * <code>null</code> is returned.
	 * 
	 * @param newObject the object to add
	 * @return a shared object that is equal to the given object or <code>null</code>
	 */
	public abstract T add(T newObject);
}