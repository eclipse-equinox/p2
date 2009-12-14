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
 * An iterator filter using a boolean {@link #isMatch(Object)} method.
 */
public abstract class MatchIteratorFilter implements Iterator {
	private static final Object NO_ELEMENT = new Object();

	private final Iterator innerIterator;

	private Object nextObject = NO_ELEMENT;

	public MatchIteratorFilter(Iterator iterator) {
		this.innerIterator = iterator;
	}

	public boolean hasNext() {
		return positionNext();
	}

	public Object next() {
		if (!positionNext())
			throw new NoSuchElementException();

		Object nxt = nextObject;
		nextObject = NO_ELEMENT;
		return nxt;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	protected Iterator getInnerIterator() {
		return innerIterator;
	}

	protected abstract boolean isMatch(Object val);

	private boolean positionNext() {
		if (nextObject != NO_ELEMENT)
			return true;

		while (innerIterator.hasNext()) {
			Object nxt = innerIterator.next();
			if (isMatch(nxt)) {
				nextObject = nxt;
				return true;
			}
		}
		return false;
	}
}