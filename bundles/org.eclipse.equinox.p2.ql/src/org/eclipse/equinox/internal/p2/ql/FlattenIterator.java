/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ql;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A FlattenIterator will assume that its operand is an iterator that will produce
 * elements that can be represented as iterators. The elements of those iterators
 * will be returned in sequence, thus removing one iterator dimension.
 */
public class FlattenIterator<T> implements Iterator<T> {
	private static final Object NO_ELEMENT = new Object();
	private final Iterator<? extends Object> iteratorIterator;
	private Iterator<T> currentIterator;

	private T nextObject = noElement();

	public FlattenIterator(Iterator<? extends Object> iterator) {
		this.iteratorIterator = iterator;
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

	@SuppressWarnings("unchecked")
	private boolean positionNext() {
		if (nextObject != NO_ELEMENT)
			return true;

		while (currentIterator == null || !currentIterator.hasNext()) {
			if (!iteratorIterator.hasNext())
				return false;

			Object nextItor = iteratorIterator.next();
			currentIterator = (nextItor instanceof Iterator<?>) ? (Iterator<T>) nextItor : RepeatableIterator.<T> create(nextItor);
		}
		nextObject = currentIterator.next();
		return true;
	}

	@SuppressWarnings("unchecked")
	private static <T> T noElement() {
		return (T) NO_ELEMENT;
	}
}