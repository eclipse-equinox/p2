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

import java.lang.reflect.Array;
import java.util.*;

/**
 * A collector is a generic visitor that collects objects passed to it,
 * and can then express the result of the visit in various forms. The collector
 * can also short-circuit a traversal by returning <code>false</code> from
 * its {@link #accept(Object)} method.
 * <p>
 * This default collector just accepts all objects passed to it.  Clients may subclass
 * to perform different processing on the objects passed to it.
 */
public class Collector {
	private ArrayList collected = null;

	/**
	 * Creates a new collector.
	 */
	public Collector() {
		super();
	}

	/**
	 * Accepts an object.
	 * <p>
	 * This default implementation adds the objects to a list. Clients may
	 * override this method to perform additional filtering, add different objects 
	 * to the list, short-circuit the traversal, or process the objects directly without 
	 * collecting them.
	 * 
	 * @param object the object to collect or visit
	 * @return <code>true</code> if the traversal should continue,
	 * or <code>false</code> to indicate the traversal should stop.
	 */
	public boolean accept(Object object) {
		getList().add(object);
		return true;
	}

	/**
	 * Returns the list that is being used to collect results.
	 * @return the list being used to collect results.
	 */
	protected List getList() {
		if (collected == null)
			collected = new ArrayList();
		return collected;
	}

	/**
	 * Returns whether this collector is empty.
	 * @return <code>true</code> if this collector has accepted any results,
	 * and <code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return collected == null || collected.isEmpty();
	}

	/**
	 * Returns an iterator on the collected objects.
	 * 
	 * @return an iterator of the collected objects.
	 */
	public Iterator iterator() {
		return collected == null ? Collections.EMPTY_LIST.iterator() : collected.iterator();
	}

	/**
	 * Returns the number of collected objects.
	 */
	public int size() {
		return collected == null ? 0 : collected.size();
	}

	/**
	 * Returns the collected objects as an array
	 * 
	 * @param clazz The type of array to return
	 * @return The array of results
	 * @throws ArrayStoreException the runtime type of the specified array is
	 *         not a supertype of the runtime type of every collected object
	 */
	public Object[] toArray(Class clazz) {
		int size = collected == null ? 0 : collected.size();
		Object[] result = (Object[]) Array.newInstance(clazz, size);
		if (size != 0)
			collected.toArray(result);
		return result;
	}

	public Collection toCollection() {
		return collected == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(collected);
	}
}
