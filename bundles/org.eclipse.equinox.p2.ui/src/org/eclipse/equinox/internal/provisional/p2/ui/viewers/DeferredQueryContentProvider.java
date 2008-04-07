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
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.progress.DeferredTreeContentManager;

/**
 * Content provider that retrieves children asynchronously where
 * possible using the IDeferredWorkbenchAdapter and provisioning
 * query mechanisms.
 * 
 * @since 3.4
 * 
 */
public class DeferredQueryContentProvider implements ITreeContentProvider {

	DeferredTreeContentManager manager;
	IQueryProvider queryProvider;
	Object currentInput;
	HashMap alreadyQueried = new HashMap();
	HashSet queryCompleted = new HashSet();
	AbstractTreeViewer viewer = null;
	ListenerList listeners = new ListenerList();
	boolean synchronous = false;

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
		if (manager != null)
			manager.cancel(oldInput);
		if (v instanceof AbstractTreeViewer) {
			manager = new DeferredTreeContentManager((AbstractTreeViewer) v);
			viewer = (AbstractTreeViewer) v;
		} else
			viewer = null;
		alreadyQueried = new HashMap();
		currentInput = newInput;
		Object[] inputListeners = listeners.getListeners();
		for (int i = 0; i < inputListeners.length; i++) {
			((DeferredQueryContentListener) inputListeners[i]).inputChanged(v, oldInput, newInput);
		}

	}

	public Object[] getElements(Object input) {
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
			if (element.fetchOnFirstAccessOnly() && queryCompleted.contains(element))
				return element.getChildren(element);

			if (manager != null && !synchronous) {
				if (!alreadyQueried.containsKey(parent)) {
					alreadyQueried.put(parent, null);
					Object[] fetchListeners = listeners.getListeners();
					for (int i = 0; i < fetchListeners.length; i++) {
						((DeferredQueryContentListener) fetchListeners[i]).fetchingDeferredChildren(parent);
					}
					manager.addUpdateCompleteListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								queryCompleted.add(parent);
							}
							Object[] finishedListeners = listeners.getListeners();
							for (int i = 0; i < finishedListeners.length; i++) {
								((DeferredQueryContentListener) finishedListeners[i]).finishedFetchingDeferredChildren(parent, event.getResult());
							}

						}
					});
					Object[] children = manager.getChildren(parent);
					if (children != null) {
						// This will be a placeholder to indicate 
						// that the real children are being fetched
						alreadyQueried.put(parent, children);
						return children;
					}
				} else {
					// We have already asked the manager, just bail out quickly here using the
					// same placeholder as before
					Object children = alreadyQueried.get(parent);
					if (children == null)
						return null;
					return (Object[]) children;
				}
			}
		}
		// Either we had no deferred manager or we are operating
		// in synchronous mode
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
