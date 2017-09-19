/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import java.util.*;
import org.easymock.Capture;

/**
 * A capture that captures multiple values
 */
public class MultiCapture<T> extends Capture<T> implements Collection<T> {
	private static final long serialVersionUID = 1L;
	private final List<T> captured = new ArrayList<>();

	@Override
	public boolean add(T arg0) {
		return captured.add(arg0);
	}

	@Override
	public boolean addAll(Collection<? extends T> arg0) {
		return captured.addAll(arg0);
	}

	@Override
	public void clear() {
		captured.clear();
	}

	@Override
	public boolean contains(Object arg0) {
		return captured.contains(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return captured.containsAll(arg0);
	}

	@Override
	public boolean isEmpty() {
		return captured.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return captured.iterator();
	}

	@Override
	public boolean remove(Object arg0) {
		return captured.remove(arg0);
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		return captured.removeAll(arg0);
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		return captured.retainAll(arg0);
	}

	@Override
	public void setValue(T value) {
		captured.add(value);
		super.setValue(value);
	}

	@Override
	public int size() {
		return captured.size();
	}

	@Override
	public Object[] toArray() {
		return captured.toArray();
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		return captured.toArray(arg0);
	}
}
