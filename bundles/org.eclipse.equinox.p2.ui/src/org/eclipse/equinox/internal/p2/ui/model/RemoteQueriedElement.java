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
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.equinox.p2.ui.query.ElementQueryDescriptor;
import org.eclipse.equinox.p2.ui.query.QueriedElement;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * Element wrapper class for objects that gets their children using
 * a deferred query.
 * 
 * @since 3.4
 */
public abstract class RemoteQueriedElement extends QueriedElement implements IDeferredWorkbenchAdapter {

	public Object getParent(Object o) {
		return null;
	}

	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
		collector.add(fetchChildren(o, monitor), monitor);

	}

	public Object[] getChildren(Object o) {
		return fetchChildren(o, null);
	}

	protected Object[] fetchChildren(Object o, IProgressMonitor monitor) {
		if (getQueryProvider() == null)
			return new Object[0];
		ElementQueryDescriptor queryDescriptor = getQueryProvider().getQueryDescriptor(this, getQueryType());
		if (queryDescriptor == null)
			return new Object[0];
		queryDescriptor.queryable.query(queryDescriptor.query, queryDescriptor.result, monitor);
		return queryDescriptor.result.toArray(Object.class);
	}

	public ISchedulingRule getRule(Object object) {
		return null;
	}

	public boolean isContainer() {
		return true;
	}
}
