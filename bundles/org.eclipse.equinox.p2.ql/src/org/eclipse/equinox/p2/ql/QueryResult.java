package org.eclipse.equinox.p2.ql;

import java.lang.reflect.Array;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ql.IRepeatableIterator;
import org.eclipse.equinox.internal.p2.ql.RepeatableIterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;

class QueryResult implements IQueryResult {

	private final IRepeatableIterator iterator;

	QueryResult(Iterator iterator) {
		this.iterator = RepeatableIterator.create(iterator);
	}

	QueryResult(Collection collection) {
		this.iterator = RepeatableIterator.create(collection);
	}

	public boolean isEmpty() {
		return !iterator.hasNext();
	}

	public Iterator iterator() {
		return iterator.getCopy();
	}

	public Object[] toArray(Class clazz) {
		Object provider = iterator.getIteratorProvider();
		if (provider.getClass().isArray())
			return (Object[]) provider;

		if (provider instanceof Collector)
			return ((Collector) provider).toArray(clazz);

		Collection c = (Collection) provider;
		return c.toArray((Object[]) Array.newInstance(clazz, c.size()));
	}

	public Set toSet() {
		Object provider = iterator.getIteratorProvider();
		if (provider.getClass().isArray()) {
			Object[] elems = (Object[]) provider;
			int idx = elems.length;
			HashSet copy = new HashSet(idx);
			while (--idx >= 0)
				copy.add(elems[idx]);
			return copy;
		}
		if (provider instanceof Collector)
			return ((Collector) provider).toSet();
		if (provider instanceof Map)
			return new HashSet(((Map) provider).entrySet());
		return new HashSet((Collection) provider);
	}

	public IQueryResult query(IQuery query, IProgressMonitor monitor) {
		return query.perform(iterator());
	}

	public Set unmodifiableSet() {
		Object provider = iterator.getIteratorProvider();
		if (provider instanceof Collector)
			return ((Collector) provider).unmodifiableSet();

		if (provider instanceof Set)
			return Collections.unmodifiableSet((Set) provider);

		if (provider instanceof Map)
			return Collections.unmodifiableSet(((Map) provider).entrySet());

		return toSet();
	}
}
