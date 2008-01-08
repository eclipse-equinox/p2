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

package org.eclipse.equinox.p2.ui.viewers;

import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.ui.query.IProvElementQueryProvider;
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
	ArrayList runningJobs = new ArrayList();

	public AvailableIUContentProvider(IProvElementQueryProvider queryProvider) {
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
		allChildren = new HashSet();
		cancelJobs();
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
				startFetchingElements(elements[i], placeholder);
				allChildren.add(placeholder);
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
				if (element instanceof ProvElement) {
					if (element instanceof RemoteQueriedElement) {
						IElementCollector collector = new IElementCollector() {
							boolean added = false;

							public void add(Object o, IProgressMonitor pm) {
								allChildren.add(o);
								added = true;
							}

							public void add(Object[] objs, IProgressMonitor pm) {
								for (int i = 0; i < objs.length; i++) {
									add(objs[i], pm);
								}
							}

							public void done() {
								if (added)
									allChildren.remove(placeholder);
								else
									placeholder.failed = true;
							}
						};
						((RemoteQueriedElement) element).fetchDeferredChildren(element, collector, SubMonitor.convert(monitor));
						collector.done();
					} else {
						Object[] children = ((ProvElement) element).getChildren(element);
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
							treeViewer.refresh();
						}
					});
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.LONG);
		job.setSystem(true);
		runningJobs.add(job);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				runningJobs.remove(job);
			}
		});
		job.schedule();
	}

	void cancelJobs() {
		// copy to array since we delete as we go
		Job[] jobs = (Job[]) runningJobs.toArray(new Job[runningJobs.size()]);
		for (int i = 0; i < jobs.length; i++)
			jobs[i].cancel();
	}
}