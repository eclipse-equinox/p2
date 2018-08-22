/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public boolean hasNext() {
		return positionNext();
	}

	@Override
	public T next() {
		if (!positionNext())
			throw new NoSuchElementException();

		T nxt = nextObject;
		nextObject = noElement();
		return nxt;
	}

	@Override
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