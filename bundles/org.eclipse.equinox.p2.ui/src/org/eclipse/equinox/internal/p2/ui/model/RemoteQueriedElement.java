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
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * Element wrapper class for objects that gets their children using
 * a deferred query.
 * 
 * @since 3.4
 */
public abstract class RemoteQueriedElement extends QueriedElement implements IDeferredWorkbenchAdapter {

	boolean alreadyCollected = false;

	protected RemoteQueriedElement(Object parent) {
		super(parent);
	}

	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
		try {
			Object[] children = fetchChildren(o, monitor);

			if (!monitor.isCanceled() && !alreadyCollected) {
				collector.add(children, monitor);
				alreadyCollected = true;
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

}
