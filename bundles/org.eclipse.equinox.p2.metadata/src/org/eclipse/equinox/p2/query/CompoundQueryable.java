/******************************************************************************* 
* Copyright (c) 2009, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.query;

import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.expression.CompoundIterator;
import org.eclipse.equinox.internal.p2.metadata.index.CompoundIndex;
import org.eclipse.equinox.internal.p2.metadata.index.IndexProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.KeyWithLocale;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.index.IIndex;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;

/**
 * A queryable that holds a number of other IQueryables and provides
 * a mechanism for querying the entire set.
 * @since 2.0
 */
public final class CompoundQueryable<T> extends IndexProvider<T> {

	static class PassThroughIndex<T> implements IIndex<T> {
		private final Iterator<T> iterator;

		public PassThroughIndex(Iterator<T> iterator) {
			this.iterator = iterator;
		}

		public Iterator<T> getCandidates(IEvaluationContext ctx, IExpression variable, IExpression booleanExpr) {
			return iterator;
		}
	}

	private IQueryable<T>[] queryables;

	public CompoundQueryable(IQueryable<T>[] queryables) {
		this.queryables = queryables;
	}

	/**
	 * Creates a queryable that combines the given collection of input queryables
	 * 
	 * @param queryables The collection of queryables to be combined
	 */
	CompoundQueryable(Collection<? extends IQueryable<T>> queryables) {
		// don't suppress the warning as it will cause warnings in the official build
		// see bug 423628. Write this without unchecked conversion.
		this(queryables.toArray(new IQueryable[queryables.size()]));
	}

	/**
	 * Creates a queryable that combines the two provided input queryables
	 * 
	 * @param query1 The first queryable
	 * @param query2 The second queryable
	 */
	@SuppressWarnings("unchecked")
	CompoundQueryable(IQueryable<T> query1, IQueryable<T> query2) {
		this(new IQueryable[] {query1, query2});
	}

	public IIndex<T> getIndex(String memberName) {
		// Check that at least one of the queryable can present an index
		// for the given member.
		boolean found = false;
		for (IQueryable<T> queryable : queryables) {
			if (queryable instanceof IIndexProvider<?>) {
				@SuppressWarnings("unchecked")
				IIndexProvider<T> ip = (IIndexProvider<T>) queryable;
				if (ip.getIndex(memberName) != null) {
					found = true;
					break;
				}
			}
		}

		if (!found)
			// Nobody had an index for this member
			return null;

		ArrayList<IIndex<T>> indexes = new ArrayList<IIndex<T>>(queryables.length);
		for (IQueryable<T> queryable : queryables) {
			if (queryable instanceof IIndexProvider<?>) {
				@SuppressWarnings("unchecked")
				IIndexProvider<T> ip = (IIndexProvider<T>) queryable;
				IIndex<T> index = ip.getIndex(memberName);
				if (index != null)
					indexes.add(index);
				else
					indexes.add(new PassThroughIndex<T>(ip.everything()));
			} else {
				indexes.add(new PassThroughIndex<T>(getIteratorFromQueryable(queryable)));
			}
		}
		return indexes.size() == 1 ? indexes.get(0) : new CompoundIndex<T>(indexes);
	}

	public Iterator<T> everything() {
		if (queryables.length == 0)
			return Collections.<T> emptySet().iterator();

		if (queryables.length == 1)
			return getIteratorFromQueryable(queryables[0]);

		ArrayList<Iterator<T>> iterators = new ArrayList<Iterator<T>>(queryables.length);
		for (IQueryable<T> queryable : queryables)
			iterators.add(getIteratorFromQueryable(queryable));
		return new CompoundIterator<T>(iterators.iterator());
	}

	public Object getManagedProperty(Object client, String memberName, Object key) {
		for (IQueryable<T> queryable : queryables) {
			if (queryable instanceof IIndexProvider<?>) {
				@SuppressWarnings("unchecked")
				IIndexProvider<T> ip = (IIndexProvider<T>) queryable;
				Object value = ip.getManagedProperty(client, memberName, key);
				if (value != null)
					return value;
			}
		}

		// When asked for translatedProperties we should return from the IU when the property is not found.
		if (client instanceof IInstallableUnit && memberName.equals(InstallableUnit.MEMBER_TRANSLATED_PROPERTIES)) {
			IInstallableUnit iu = (IInstallableUnit) client;
			return key instanceof KeyWithLocale ? iu.getProperty(((KeyWithLocale) key).getKey()) : iu.getProperty(key.toString());
		}
		return null;
	}

	static class IteratorCapture<T> implements IQuery<T> {
		private Iterator<T> capturedIterator;

		public IQueryResult<T> perform(Iterator<T> iterator) {
			capturedIterator = iterator;
			return Collector.emptyCollector();
		}

		public IExpression getExpression() {
			return null;
		}

		Iterator<T> getCapturedIterator() {
			return capturedIterator == null ? Collections.<T> emptySet().iterator() : capturedIterator;
		}
	}

	private static <T> Iterator<T> getIteratorFromQueryable(IQueryable<T> queryable) {
		if (queryable instanceof IIndexProvider<?>) {
			@SuppressWarnings("unchecked")
			IIndexProvider<T> ip = (IIndexProvider<T>) queryable;
			return ip.everything();
		}
		IteratorCapture<T> capture = new IteratorCapture<T>();
		queryable.query(capture, new NullProgressMonitor());
		return capture.getCapturedIterator();
	}
}
