/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core.helpers;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayUtils {
	/**
	 * Find the index of the first occurrence of object in array, or -1.
	 * Use Arrays.binarySearch if array is big and sorted.
	 */
	public static int indexOf(Object[] array, Object object) {
		for (int i = 0; i < array.length; i += 1) {
			if (object == null ? array[i] == null : object.equals(array[i]))
				return i;
		}
		return -1;
	}

	/**
	 * Iterate over an array.
	 */
	public static class ArrayIterator implements Iterator {
		private final Object[] array;
		private int next; // next element to return

		public ArrayIterator(Object[] array) {
			this.array = array;
			this.next = 0;
		}

		public boolean hasNext() {
			return this.next < this.array.length;
		}

		public Object next() throws NoSuchElementException {
			try {
				return this.array[this.next++];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
