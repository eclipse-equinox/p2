/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository;

import java.util.*;

/**
 * An iterator over values that are provided by iterating over collections.
 */
public class FlatteningIterator<T> implements Iterator<T> {
	private static final Object NO_ELEMENT = new Object();
	private final Iterator<? extends Collection<T>> collectionIterator;
	private Iterator<T> currentIterator;

	private T nextObject = noElement();

	public FlatteningIterator(Iterator<? extends Collection<T>> collectionIterator) {
		this.collectionIterator = collectionIterator;
	}

	public boolean hasNext() {
		return positionNext();
	}

	public T next() {
		if (!positionNext())
			throw new NoSuchElementException();

		T nxt = nextObject;
		nextObject = noElement();
		return nxt;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	private boolean positionNext() {
		if (nextObject != NO_ELEMENT)
			return true;

		while (currentIterator == null || !currentIterator.hasNext()) {
			if (!collectionIterator.hasNext())
				return false;
			currentIterator = collectionIterator.next().iterator();
		}
		nextObject = currentIterator.next();
		return true;
	}

	@SuppressWarnings("unchecked")
	private static <T> T noElement() {
		return (T) NO_ELEMENT;
	}
}