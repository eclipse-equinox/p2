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
public final class ExpressionContext implements IRepeatableIterator {
	private static final Object[] noParameters = new Object[0];

	private final Iterator iterator;

	private final Class instanceClass;

	private final Object[] parameters;

	private boolean atStart = true;

	public ExpressionContext(Class instanceClass, Object[] parameters, Collection collection) {
		this.instanceClass = instanceClass;
		this.parameters = parameters == null ? noParameters : parameters;
		this.iterator = RepeatableIterator.create(collection == null ? Collections.EMPTY_LIST : collection);
	}

	public ExpressionContext(Class instanceClass, Object[] parameters, final Iterator iterator, boolean needsRepeat) {
		this.instanceClass = instanceClass;
		this.parameters = parameters;
		this.iterator = needsRepeat ? RepeatableIterator.create(iterator) : iterator;
	}

	public Class getInstanceClass() {
		return instanceClass;
	}

	public Object getParameter(int position) {
		return parameters[position];
	}

	public Object getParameter(String key) {
		return parameters.length == 1 && parameters[0] instanceof Map ? ((Map) parameters[0]).get(key) : null;
	}

	public IRepeatableIterator getCopy() {
		if (iterator instanceof IRepeatableIterator)
			return ((IRepeatableIterator) iterator).getCopy();
		if (atStart)
			return this;
		throw new UnsupportedOperationException();
	}

	public boolean hasNext() {
		return iterator.hasNext();
	}

	public Object next() {
		atStart = false;
		return iterator.next();
	}

	public void remove() {
		iterator.remove();
	}

	public Object getIteratorProvider() {
		if (iterator instanceof IRepeatableIterator)
			return ((IRepeatableIterator) iterator).getIteratorProvider();
		return this;
	}
}
