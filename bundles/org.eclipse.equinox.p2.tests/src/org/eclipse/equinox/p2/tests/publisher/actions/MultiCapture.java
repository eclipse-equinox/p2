/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
	private final List captured = new ArrayList();

	public boolean add(T arg0) {
		return captured.add(arg0);
	}

	public boolean addAll(Collection arg0) {
		return captured.addAll(arg0);
	}

	public void clear() {
		captured.clear();
	}

	public boolean contains(Object arg0) {
		return captured.contains(arg0);
	}

	public boolean containsAll(Collection arg0) {
		return captured.containsAll(arg0);
	}

	public boolean isEmpty() {
		return captured.isEmpty();
	}

	public Iterator iterator() {
		return captured.iterator();
	}

	public boolean remove(Object arg0) {
		return captured.remove(arg0);
	}

	public boolean removeAll(Collection arg0) {
		return captured.removeAll(arg0);
	}

	public boolean retainAll(Collection arg0) {
		return captured.retainAll(arg0);
	}

	@Override
	public void setValue(T value) {
		captured.add(value);
		super.setValue(value);
	}

	public int size() {
		return captured.size();
	}

	public Object[] toArray() {
		return captured.toArray();
	}

	public Object[] toArray(Object[] arg0) {
		return captured.toArray(arg0);
	}
}
