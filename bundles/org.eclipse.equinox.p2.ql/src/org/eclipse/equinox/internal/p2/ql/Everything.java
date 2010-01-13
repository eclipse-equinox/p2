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

import org.eclipse.equinox.internal.p2.metadata.expression.IRepeatableIterator;

import org.eclipse.equinox.internal.p2.metadata.expression.RepeatableIterator;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;

/**
 * The immutable context used when evaluating an expression.
 */
public final class Everything<T> extends MatchIteratorFilter<T> implements IRepeatableIterator<T> {
	private boolean atStart = true;

	private final Class<T> elementClass;

	public Everything(Class<T> elementClass, Collection<T> collection) {
		super(RepeatableIterator.<T> create(collection == null ? CollectionUtils.<T> emptyList() : collection));
		this.elementClass = elementClass;
	}

	public Everything(Class<T> elementClass, Iterator<? extends T> iterator, boolean needsRepeat) {
		super(needsRepeat ? RepeatableIterator.create(iterator) : iterator);
		this.elementClass = elementClass;
	}

	public IRepeatableIterator<T> getCopy() {
		Iterator<? extends T> iterator = getInnerIterator();
		if (iterator instanceof IRepeatableIterator<?>)
			return new Everything<T>(elementClass, ((IRepeatableIterator<? extends T>) iterator).getCopy(), false);
		if (atStart)
			return this;
		throw new UnsupportedOperationException();
	}

	public T next() {
		atStart = false;
		return super.next();
	}

	public Object getIteratorProvider() {
		Iterator<? extends T> iterator = getInnerIterator();
		if (iterator instanceof IRepeatableIterator<?>)
			return ((IRepeatableIterator<?>) iterator).getIteratorProvider();
		return this;
	}

	protected boolean isMatch(T val) {
		return elementClass.isInstance(val);
	}
}
