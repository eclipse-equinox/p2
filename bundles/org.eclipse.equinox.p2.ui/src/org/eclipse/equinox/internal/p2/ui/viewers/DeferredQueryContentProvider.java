/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.viewers;

import java.util.HashMap;
import java.util.HashSet;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElement;
import org.eclipse.equinox.internal.p2.ui.model.RemoteQueriedElement;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider that retrieves children asynchronously where
 * possible using the IDeferredWorkbenchAdapter and provisioning
 * query mechanisms.
 * 
 * @since 3.4
 * 
 */
public class DeferredQueryContentProvider extends ProvElementContentProvider {

	DeferredQueryTreeContentManager manager;
	Object currentInput;
	HashMap<Object, Object> alreadyQueried = new HashMap<Object, Object>();
	HashSet<Object> queryCompleted = new HashSet<Object>();
	AbstractTreeViewer viewer = null;
	ListenerList listeners = new ListenerList();
	boolean synchronous = false;

	/**
	 * 
	 */
	public DeferredQueryContentProvider() {
		// Default constructor
	}

	public void addListener(IInputChangeListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IInputChangeListener listener) {
		listeners.remove(listener);
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		super.inputChanged(v, oldInput, newInput);

		if (manager != null)
			manager.cancel(oldInput);
		if (v instanceof AbstractTreeViewer) {
			manager = new DeferredQueryTreeContentManager((AbstractTreeViewer) v);
			viewer = (AbstractTreeViewer) v;
			manager.setListener(new IDeferredQueryTreeListener() {

				public void fetchingDeferredChildren(Object parent, Object placeholder) {
					alreadyQueried.put(parent, placeholder);
				}

				public void finishedFetchingDeferredChildren(Object parent, Object placeholder) {
					queryCompleted.add(parent);
				}
			});
		} else
			viewer = null;
		alreadyQueried = new HashMap<Object, Object>();
		queryCompleted = new HashSet<Object>();
		currentInput = newInput;
		Object[] inputListeners = listeners.getListeners();
		for (int i = 0; i < inputListeners.length; i++) {
			((IInputChangeListener) inputListeners[i]).inputChanged(v, oldInput, newInput);
		}
	}

	public Object[] getElements(Object input) {
		if (input instanceof QueriedElement) {
			return getChildren(input);
		}
		return super.getElements(input);
	}

	public void dispose() {
		super.dispose();
		if (manager != null) {
			manager.cancel(currentInput);
		}
	}

	public boolean hasChildren(Object element) {
		if (manager != null) {
			if (manager.isDeferredAdapter(element))
				return manager.mayHaveChildren(element);
		}
		return super.hasChildren(element);
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
		return super.getChildren(parent);
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
