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

import java.util.*;

/**
 * The immutable context used when evaluating an expression.
 */
public final class Everything extends MatchIteratorFilter implements IRepeatableIterator {
	private boolean atStart = true;

	private final Class elementClass;

	public Everything(Class elementClass, Collection collection) {
		super(RepeatableIterator.create(collection == null ? Collections.EMPTY_LIST : collection));
		this.elementClass = elementClass;
	}

	public Everything(Class elementClass, Iterator iterator, boolean needsRepeat) {
		super(needsRepeat ? RepeatableIterator.create(iterator) : iterator);
		this.elementClass = elementClass;
	}

	public IRepeatableIterator getCopy() {
		Iterator iterator = getInnerIterator();
		if (iterator instanceof IRepeatableIterator)
			return new Everything(elementClass, ((IRepeatableIterator) iterator).getCopy(), false);
		if (atStart)
			return this;
		throw new UnsupportedOperationException();
	}

	public Object next() {
		atStart = false;
		return super.next();
	}

	public Object getIteratorProvider() {
		Iterator iterator = getInnerIterator();
		if (iterator instanceof IRepeatableIterator)
			return ((IRepeatableIterator) iterator).getIteratorProvider();
		return this;
	}

	protected boolean isMatch(Object val) {
		return elementClass.isInstance(val);
	}
}
