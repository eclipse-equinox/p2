/*******************************************************************************
 * Copyright (c) 2009 - 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator filter using a boolean {@link #isMatch(Object)} method.
 */
public abstract class MatchIteratorFilter<T> implements Iterator<T> {
	private static final Object NO_ELEMENT = new Object();

	private final Iterator<? extends T> innerIterator;

	private T nextObject = noElement();

	public MatchIteratorFilter(Iterator<? extends T> iterator) {
		this.innerIterator = iterator;
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

	protected Iterator<? extends T> getInnerIterator() {
		return innerIterator;
	}

	protected abstract boolean isMatch(T val);

	private boolean positionNext() {
		if (nextObject != NO_ELEMENT)
			return true;

		while (innerIterator.hasNext()) {
			T nxt = innerIterator.next();
			if (isMatch(nxt)) {
				nextObject = nxt;
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static <T> T noElement() {
		return (T) NO_ELEMENT;
	}
}