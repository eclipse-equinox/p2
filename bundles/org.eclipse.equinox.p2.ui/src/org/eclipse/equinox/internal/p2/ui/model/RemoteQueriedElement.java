/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.equinox.internal.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * Element wrapper class for objects that gets their children using
 * a deferred query.
 * 
 * @since 3.4
 */
public abstract class RemoteQueriedElement extends QueriedElement implements IDeferredWorkbenchAdapter {

	protected RemoteQueriedElement(Object parent) {
		super(parent);
	}

	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
		try {
			Object[] children = fetchChildren(o, monitor);
			for (Object child : children) {
				if (child instanceof CategoryElement) {
					((CategoryElement) child).fetchChildren(child, monitor);
				}
			}
			if (!monitor.isCanceled()) {
				collector.add(children, monitor);
			}
		} catch (OperationCanceledException e) {
			// Nothing to do
		}
		collector.done();
	}

	public ISchedulingRule getRule(Object object) {
		return null;
	}

	public boolean isContainer() {
		return true;
	}

	/*
	 * Overridden to ensure that we check whether we are using a
	 * QueryableMetadataRepositoryManager as our queryable.  If so, 
	 * we must find out if it is up to date with the real manager.  
	 *
	 * This is necessary to prevent background loading of already loaded repositories
	 * by the DeferredTreeContentManager, which will add redundant children to the
	 * viewer.  
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=229069
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=226343
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=275235
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#hasQueryable()
	 */

	public boolean hasQueryable() {
		if (queryable instanceof QueryableMetadataRepositoryManager)
			return ((QueryableMetadataRepositoryManager) queryable).areRepositoriesLoaded();
		return super.hasQueryable();
	}

}
