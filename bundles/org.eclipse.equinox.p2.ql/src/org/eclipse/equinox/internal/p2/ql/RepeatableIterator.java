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
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;

public class RepeatableIterator implements IRepeatableIterator {
	private final List values;
	private int position = -1;

	public static IRepeatableIterator create(Object unknown) {
		if (unknown.getClass().isArray())
			return create((Object[]) unknown);
		if (unknown instanceof Iterator)
			return create((Iterator) unknown);
		if (unknown instanceof List)
			return create((List) unknown);
		if (unknown instanceof Collection)
			return create((Collection) unknown);
		if (unknown instanceof Map)
			return create(((Map) unknown).entrySet());
		if (unknown instanceof Collector)
			return create((Collector) unknown);
		throw new IllegalArgumentException("Cannot convert a " + unknown.getClass().getName() + " into an iterator"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static IRepeatableIterator create(Iterator iterator) {
		return iterator instanceof IRepeatableIterator ? ((IRepeatableIterator) iterator).getCopy() : new ElementRetainingIterator(iterator);
	}

	public static IRepeatableIterator create(List values) {
		return new RepeatableIterator(values);
	}

	public static IRepeatableIterator create(Collection values) {
		return new CollectionIterator(values);
	}

	public static IRepeatableIterator create(Collector values) {
		return new CollectorIterator(values);
	}

	public static IRepeatableIterator create(Object[] values) {
		return new ArrayIterator(values);
	}

	RepeatableIterator(List values) {
		this.values = values;
	}

	public IRepeatableIterator getCopy() {
		return new RepeatableIterator(values);
	}

	public boolean hasNext() {
		return position + 1 < values.size();
	}

	public Object next() {
		if (++position == values.size()) {
			--position;
			throw new NoSuchElementException();
		}
		return values.get(position);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public Object getIteratorProvider() {
		return values;
	}

	void setPosition(int position) {
		this.position = position;
	}

	List getValues() {
		return values;
	}

	static class ArrayIterator implements IRepeatableIterator {
		private final Object[] array;
		private int position = -1;

		public ArrayIterator(Object[] array) {
			this.array = array;
		}

		public Object getIteratorProvider() {
			return array;
		}

		public boolean hasNext() {
			return position + 1 < array.length;
		}

		public Object next() {
			if (++position >= array.length)
				throw new NoSuchElementException();
			return array[position];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public IRepeatableIterator getCopy() {
			return new ArrayIterator(array);
		}
	}

	static class CollectionIterator implements IRepeatableIterator {
		private final Collection collection;

		private final Iterator iterator;

		CollectionIterator(Collection collection) {
			this.collection = collection;
			this.iterator = collection.iterator();
		}

		public IRepeatableIterator getCopy() {
			return new CollectionIterator(collection);
		}

		public Object getIteratorProvider() {
			return collection;
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public Object next() {
			return iterator.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	static class CollectorIterator implements IRepeatableIterator {
		private final Collector collector;

		private final Iterator iterator;

		CollectorIterator(Collector collector) {
			this.collector = collector;
			this.iterator = collector.iterator();
		}

		public IRepeatableIterator getCopy() {
			return new CollectorIterator(collector);
		}

		public Object getIteratorProvider() {
			return collector;
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public Object next() {
			return iterator.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	static class ElementRetainingIterator extends RepeatableIterator {

		private Iterator innerIterator;

		ElementRetainingIterator(Iterator iterator) {
			super(new ArrayList());
			innerIterator = iterator;
		}

		public synchronized boolean hasNext() {
			if (innerIterator != null) {
				if (innerIterator.hasNext())
					return true;
				innerIterator = null;
				setPosition(getValues().size());
			}
			return super.hasNext();
		}

		public synchronized Object next() {
			if (innerIterator != null) {
				Object val = innerIterator.next();
				getValues().add(val);
				return val;
			}
			return super.next();
		}

		public synchronized IRepeatableIterator getCopy() {
			// If the current iterator still exists, we must exhaust it first
			//
			exhaustInnerIterator();
			return super.getCopy();
		}

		public synchronized Object getIteratorProvider() {
			exhaustInnerIterator();
			return super.getIteratorProvider();
		}

		private void exhaustInnerIterator() {
			if (innerIterator != null) {
				List values = getValues();
				int savePos = values.size() - 1;
				while (innerIterator.hasNext())
					values.add(innerIterator.next());
				innerIterator = null;
				setPosition(savePos);
			}
		}
	}
}
