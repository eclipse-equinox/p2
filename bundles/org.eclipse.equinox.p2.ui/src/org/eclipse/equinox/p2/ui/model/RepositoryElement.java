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
package org.eclipse.equinox.p2.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * Element wrapper class for repository that gets its contents
 * in a deferred manner.
 * 
 * @since 3.4
 */
public abstract class RepositoryElement extends ProvElement implements IDeferredWorkbenchAdapter {

	IRepository repo;

	public RepositoryElement(IRepository repo) {
		this.repo = repo;
	}

	public Object getParent(Object o) {
		return null;
	}

	public String getLabel(Object o) {
		String name = repo.getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		return repo.getLocation().toExternalForm();

	}

	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
		collector.add(fetchChildren(o, monitor), monitor);

	}

	public Object[] getChildren(Object o) {
		return fetchChildren(o, null);
	}

	protected abstract Object[] fetchChildren(Object o, IProgressMonitor monitor);

	public Object getAdapter(Class adapter) {
		if (adapter == IRepository.class)
			return repo;
		return super.getAdapter(adapter);
	}

	public ISchedulingRule getRule(Object object) {
		return null;
	}

	public boolean isContainer() {
		return true;
	}
}
