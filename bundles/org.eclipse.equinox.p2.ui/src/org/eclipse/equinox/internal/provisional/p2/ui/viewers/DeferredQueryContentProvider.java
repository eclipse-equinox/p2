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

package org.eclipse.equinox.internal.provisional.p2.ui.viewers;

import java.util.HashMap;
import java.util.HashSet;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.internal.p2.ui.viewers.DeferredQueryTreeContentManager;
import org.eclipse.equinox.internal.p2.ui.viewers.DeferredQueryTreeListener;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Display;

/**
 * Content provider that retrieves children asynchronously where
 * possible using the IDeferredWorkbenchAdapter and provisioning
 * query mechanisms.
 * 
 * @since 3.4
 * 
 */
public class DeferredQueryContentProvider implements ITreeContentProvider {

	DeferredQueryTreeContentManager manager;
	IQueryProvider queryProvider;
	Object currentInput;
	HashMap alreadyQueried = new HashMap();
	HashSet queryCompleted = new HashSet();
	AbstractTreeViewer viewer = null;
	ListenerList listeners = new ListenerList();
	boolean synchronous = false;
	TableViewer tableViewer = null;
	Job profileFetchJob = null;
	Display display = null;
	String placeholder;
	Object[] realChildren;

	public DeferredQueryContentProvider(IQueryProvider queryProvider) {
		this.queryProvider = queryProvider;
	}

	public void addListener(DeferredQueryContentListener listener) {
		listeners.add(listener);
	}

	public void removeListener(DeferredQueryContentListener listener) {
		listeners.remove(listener);
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		// cache display while in UI thread
		display = v.getControl().getDisplay();
		if (manager != null)
			manager.cancel(oldInput);
		if (profileFetchJob != null) {
			profileFetchJob.cancel();
			profileFetchJob = null;
		}
		if (v instanceof AbstractTreeViewer) {
			manager = new DeferredQueryTreeContentManager((AbstractTreeViewer) v);
			viewer = (AbstractTreeViewer) v;
			tableViewer = null;
			manager.setListener(new DeferredQueryTreeListener() {

				public void fetchingDeferredChildren(Object parent, Object placeholder) {
					alreadyQueried.put(parent, placeholder);
				}

				public void finishedFetchingDeferredChildren(Object parent, Object placeholder) {
					queryCompleted.add(parent);
				}
			});
		} else {
			viewer = null;
			if (v instanceof TableViewer)
				tableViewer = (TableViewer) v;
		}
		alreadyQueried = new HashMap();
		queryCompleted = new HashSet();
		currentInput = newInput;
		Object[] inputListeners = listeners.getListeners();
		for (int i = 0; i < inputListeners.length; i++) {
			((DeferredQueryContentListener) inputListeners[i]).inputChanged(v, oldInput, newInput);
		}

	}

	public Object[] getElements(final Object input) {
		if (input instanceof ProfileElement) {
			if (profileFetchJob == null && tableViewer != null) {
				final ProfileElement element = (ProfileElement) input;
				element.setQueryProvider(queryProvider);
				placeholder = ProvUIMessages.DeferredQueryContentProvider_Pending;
				profileFetchJob = new Job(ProvUIMessages.DeferredQueryContentProvider_FetchJobName) {
					public IStatus run(IProgressMonitor monitor) {
						if (monitor.isCanceled())
							return Status.CANCEL_STATUS;
						realChildren = element.fetchChildren(input, monitor);
						return Status.OK_STATUS;
					}
				};
				profileFetchJob.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						// whether finished or cancelled, get rid of the placeholder.
						if (display == null || placeholder == null)
							return;
						final boolean addChildren = event.getResult().isOK();
						display.asyncExec(new Runnable() {
							public void run() {
								if (!tableViewer.getControl().isDisposed()) {
									tableViewer.remove(placeholder);
									if (addChildren) {
										tableViewer.add(realChildren);
										// Set the selection explicitly so listeners can
										// change their validation
										tableViewer.setSelection(StructuredSelection.EMPTY);
									}
								}
								placeholder = null;
							}
						});
					}
				});
				profileFetchJob.schedule();
				realChildren = new Object[] {placeholder};
			}
			// realChildren could contain the placeholder or the real stuff
			return realChildren;
		}
		if (input instanceof QueriedElement) {
			QueriedElement element = (QueriedElement) input;
			element.setQueryProvider(queryProvider);
			return getChildren(element);
		}
		return null;
	}

	public void dispose() {
		if (manager != null) {
			manager.cancel(currentInput);
		}
		if (profileFetchJob != null) {
			profileFetchJob.cancel();
			profileFetchJob = null;
		}
	}

	public Object getParent(Object child) {
		if (child instanceof ProvElement) {
			return ((ProvElement) child).getParent(child);
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (manager != null) {
			if (manager.isDeferredAdapter(element))
				return manager.mayHaveChildren(element);
		}
		if (element instanceof ProvElement)
			return ((ProvElement) element).hasChildren(element);
		return false;
	}

	public Object[] getChildren(final Object parent) {
		if (parent instanceof RemoteQueriedElement) {
			RemoteQueriedElement element = (RemoteQueriedElement) parent;
			// We rely on the assumption that the queryable is the most expensive
			// thing to get vs. the query itself being expensive.
			// (loading a repo vs. querying a repo afterward)
			if (element.hasQueryable())
				return element.getChildren(element);

			if (manager != null && !synchronous) {
				return manager.getChildren(parent);
			}
		}
		// Either it's not a remote element or we are in synchronous mode
		if (parent instanceof ProvElement) {
			return ((ProvElement) parent).getChildren(parent);
		}
		return null;
	}

	public void setSynchronous(boolean sync) {
		if (sync == true && manager != null)
			manager.cancel(currentInput);
		this.synchronous = sync;
	}

	public boolean getSynchronous() {
		return synchronous;
	}
}
