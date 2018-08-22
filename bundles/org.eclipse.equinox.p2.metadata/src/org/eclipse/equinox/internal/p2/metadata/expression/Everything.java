/*******************************************************************************
 * Copyright (c) 2009, 2018 Cloudsmith Inc. and others.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;

/**
 * The immutable context used when evaluating an expression.
 */
public final class Everything<T> extends MatchIteratorFilter<T> implements IRepeatableIterator<T> {
	private boolean atStart = true;

	private final Class<? extends T> elementClass;

	public Everything(Class<? extends T> elementClass, Collection<T> collection) {
		super(RepeatableIterator.create(collection == null ? Collections.emptyList() : collection));
		this.elementClass = elementClass;
	}

	public Everything(Class<? extends T> elementClass, Iterator<? extends T> iterator, Expression expression) {
		this(elementClass, iterator, needsRepeadedAccessToEverything(expression));
	}

	public Everything(Class<? extends T> elementClass, IIndexProvider<? extends T> indexProvider) {
		super(RepeatableIterator.create(indexProvider));
		this.elementClass = elementClass;
	}

	Everything(Class<? extends T> elementClass, Iterator<? extends T> iterator, boolean needsRepeat) {
		super(needsRepeat ? RepeatableIterator.create(iterator) : iterator);
		this.elementClass = elementClass;
	}

	@Override
	public IRepeatableIterator<T> getCopy() {
		Iterator<? extends T> iterator = getInnerIterator();
		if (iterator instanceof IRepeatableIterator<?>)
			return new Everything<>(elementClass, ((IRepeatableIterator<? extends T>) iterator).getCopy(), false);
		if (atStart)
			return this;
		throw new UnsupportedOperationException();
	}

	@Override
	public T next() {
		atStart = false;
		return super.next();
	}

	public Class<? extends T> getElementClass() {
		return elementClass;
	}

	@Override
	public Object getIteratorProvider() {
		Iterator<? extends T> iterator = getInnerIterator();
		if (iterator instanceof IRepeatableIterator<?>)
			return ((IRepeatableIterator<?>) iterator).getIteratorProvider();
		return this;
	}

	@Override
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
