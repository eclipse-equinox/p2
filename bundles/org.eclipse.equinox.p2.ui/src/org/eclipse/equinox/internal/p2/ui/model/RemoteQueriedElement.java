/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.equinox.internal.provisional.p2.ui.query.ElementQueryDescriptor;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * Element wrapper class for objects that gets their children using
 * a deferred query.
 * 
 * @since 3.4
 */
public abstract class RemoteQueriedElement extends QueriedElement implements IDeferredWorkbenchAdapter {

	protected RemoteQueriedElement() {
		super(null);
	}

	public Object getParent(Object o) {
		return null;
	}

	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
		try {
			Object[] children = fetchChildren(o, monitor);
			if (!monitor.isCanceled()) {
				collector.add(children, monitor);
			}
		} catch (OperationCanceledException e) {
			// Nothing to do
		}
		collector.done();

	}

	public Object[] getChildren(Object o) {
		return fetchChildren(o, null);
	}

	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		if (getQueryProvider() == null)
			return new Object[0];
		ElementQueryDescriptor queryDescriptor = getQueryProvider().getQueryDescriptor(this, getQueryType());
		if (queryDescriptor == null || !isSufficientForQuery(queryDescriptor))
			return new Object[0];
		queryDescriptor.queryable.query(queryDescriptor.query, queryDescriptor.collector, monitor);
		return queryDescriptor.collector.toArray(Object.class);
	}

	public ISchedulingRule getRule(Object object) {
		return null;
	}

	public boolean isContainer() {
		return true;
	}

	/**
	 * Return whether the query descriptor is sufficient for this element to complete the query.
	 * The default implementation requires the descriptor to be complete.  Subclasses may override.
	 * 
	 * @param queryDescriptor the query descriptor in question
	 * @return <code>true</code> if the descriptor is sufficient, <code>false</code> if it is not.
	 */
	protected boolean isSufficientForQuery(ElementQueryDescriptor queryDescriptor) {
		return queryDescriptor.isComplete();
	}

	/**
	 * Return a boolean indicating whether this element requires 
	 * remote/deferred queries only on the first access of its
	 * children, or on every access.  Elements that cache data or
	 * rely on their model caching the data may only need to use
	 * deferred queries the first time.  Elements that do not cache,
	 * and whose models do not cache, may need to use deferred
	 * queries on every access.
	 * 
	 * @return <code>true</code> if a deferred query is only required
	 * on first access of children, <code>false</code> if deferred
	 * query is required on all accesses.
	 */
	public boolean fetchOnFirstAccessOnly() {
		return true;
	}
}
