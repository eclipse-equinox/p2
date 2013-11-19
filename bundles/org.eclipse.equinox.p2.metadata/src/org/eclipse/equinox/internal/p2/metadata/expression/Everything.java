/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.*;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;

/**
 * The immutable context used when evaluating an expression.
 */
public final class Everything<T> extends MatchIteratorFilter<T> implements IRepeatableIterator<T> {
	private boolean atStart = true;

	private final Class<? extends T> elementClass;

	public Everything(Class<? extends T> elementClass, Collection<T> collection) {
		super(RepeatableIterator.<T> create(collection == null ? Collections.<T> emptyList() : collection));
		this.elementClass = elementClass;
	}

	public Everything(Class<? extends T> elementClass, Iterator<? extends T> iterator, Expression expression) {
		this(elementClass, iterator, needsRepeadedAccessToEverything(expression));
	}

	public Everything(Class<? extends T> elementClass, IIndexProvider<? extends T> indexProvider) {
		super(RepeatableIterator.<T> create(indexProvider));
		this.elementClass = elementClass;
	}

	Everything(Class<? extends T> elementClass, Iterator<? extends T> iterator, boolean needsRepeat) {
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

	public Class<? extends T> getElementClass() {
		return elementClass;
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

	/**
	 * Checks if the expression will make repeated requests for the 'everything' iterator.
	 * @return <code>true</code> if repeated requests will be made, <code>false</code> if not.
	 */
	private static boolean needsRepeadedAccessToEverything(Expression expression) {
		return expression.countAccessToEverything() > 1;
	}
}
