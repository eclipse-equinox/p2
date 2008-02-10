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

package org.eclipse.equinox.internal.p2.ui.viewers;

import java.util.HashSet;
import java.util.Hashtable;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.query.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.RepositoryContentProvider;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * Content provider for available software in any number of repos.
 * 
 * @since 3.4
 * 
 */
public class AvailableIUContentProvider extends RepositoryContentProvider {

	class RepositoryPlaceholder extends ProvElement {
		String identifier;
		boolean failed = false;

		RepositoryPlaceholder(String identifier) {
			this.identifier = identifier;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object o) {
			return new Object[0];
		}

		/*
		 * 
		 */
		public String getImageId(Object o) {
			return ProvUIImages.IMG_METADATA_REPOSITORY;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
		 */
		public String getLabel(Object o) {
			if (failed)
				return NLS.bind(ProvUIMessages.AvailableIUContentProvider_FailureRetrievingContents, identifier);
			return NLS.bind(ProvUIMessages.AvailableIUContentProvider_PlaceholderLabel, identifier);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
		 */
		public Object getParent(Object o) {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
		 */
		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class) {
				return this;
			}
			return null;
		}
	}

	AbstractTreeViewer treeViewer;
	HashSet allChildren = new HashSet();
	Hashtable runningJobs = new Hashtable();

	public AvailableIUContentProvider(IQueryProvider queryProvider) {
		super(queryProvider);
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		if (v instanceof AbstractTreeViewer) {
			treeViewer = (AbstractTreeViewer) v;
			treeViewer.getControl().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					cancelJobs();
				}
			});
		}
		cancelJobs();
		allChildren = new HashSet();
		super.inputChanged(v, oldInput, newInput);
	}

	public Object[] getElements(Object input) {
		if (allChildren.isEmpty()) {
			// Overridden to get the children of each element as the elements.
			Object[] elements = super.getElements(input);
			RepositoryPlaceholder placeholder;
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] instanceof ProvElement)
					placeholder = new RepositoryPlaceholder(((ProvElement) elements[i]).getLabel(elements[i]));
				else
					placeholder = new RepositoryPlaceholder(elements[i].toString());
				allChildren.add(placeholder);
				startFetchingElements(elements[i], placeholder);
			}
		}
		return allChildren.toArray();
	}

	private void startFetchingElements(final Object element, final RepositoryPlaceholder placeholder) {
		final Job job = new Job(NLS.bind(ProvUIMessages.AvailableIUContentProvider_JobName, placeholder.identifier)) {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.core.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			public IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				if (element instanceof ProvElement) {
					if (element instanceof RemoteQueriedElement) {
						IElementCollector collector = new IElementCollector() {
							HashSet fetchedChildren = new HashSet();

							public void add(Object o, IProgressMonitor pm) {
								fetchedChildren.add(o);
							}

							public void add(Object[] objs, IProgressMonitor pm) {
								for (int i = 0; i < objs.length; i++) {
									add(objs[i], pm);
								}
							}

							public void done() {
								if (fetchedChildren.size() > 0) {
									allChildren.addAll(fetchedChildren);
									allChildren.remove(placeholder);
								} else
									placeholder.failed = true;
							}
						};
						((RemoteQueriedElement) element).fetchDeferredChildren(element, collector, SubMonitor.convert(monitor));
						// Check whether we were cancelled during fetch.  If so, remove the placeholder without adding the
						// fetched children.
						if (monitor.isCanceled()) {
							allChildren.remove(placeholder);
							return Status.CANCEL_STATUS;
						}
						collector.done();
					} else {
						Object[] children = ((ProvElement) element).getChildren(element);
						if (monitor.isCanceled())
							return Status.CANCEL_STATUS;
						for (int i = 0; i < children.length; i++)
							allChildren.add(children[i]);
						if (children.length == 0)
							placeholder.failed = true;
						else
							allChildren.remove(placeholder);
					}
				}
				if (!treeViewer.getControl().isDisposed()) {
					treeViewer.getControl().getDisplay().asyncExec(new Runnable() {
						public void run() {
							if (treeViewer.getControl().isDisposed())
								return;
							treeViewer.refresh();
						}
					});
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.LONG);
		job.setSystem(true);
		runningJobs.put(placeholder.identifier, job);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				runningJobs.remove(job);
			}
		});
		job.schedule();
	}

	void cancelJobs() {
		// copy to keys to an array since we delete as we go
		String[] identifiers = (String[]) runningJobs.keySet().toArray(new String[runningJobs.size()]);
		for (int i = 0; i < identifiers.length; i++) {
			((Job) runningJobs.get(identifiers[i])).cancel();
			runningJobs.remove(identifiers[i]);
		}
	}
}