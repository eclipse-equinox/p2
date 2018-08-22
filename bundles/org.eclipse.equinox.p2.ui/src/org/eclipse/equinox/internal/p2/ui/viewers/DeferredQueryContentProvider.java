/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.viewers;

import java.util.HashMap;
import java.util.HashSet;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.equinox.internal.p2.ui.model.*;
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
	HashMap<Object, Object> alreadyQueried = new HashMap<>();
	HashSet<Object> queryCompleted = new HashSet<>();
	AbstractTreeViewer viewer = null;
	ListenerList<IInputChangeListener> listeners = new ListenerList<>();
	boolean synchronous = false;
	IDeferredQueryTreeListener onFetchingActionListener;

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

	public void addOnFetchingActionListener(IDeferredQueryTreeListener listener) {
		onFetchingActionListener = listener;
	}

	@Override
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		super.inputChanged(v, oldInput, newInput);

		if (manager != null)
			manager.cancel(oldInput);
		if (v instanceof AbstractTreeViewer) {
			manager = new DeferredQueryTreeContentManager((AbstractTreeViewer) v);
			manager.addListener(onFetchingActionListener);
			viewer = (AbstractTreeViewer) v;
			manager.addListener(new IDeferredQueryTreeListener() {

				@Override
				public void fetchingDeferredChildren(Object parent, Object placeholder) {
					alreadyQueried.put(parent, placeholder);
				}

				@Override
				public void finishedFetchingDeferredChildren(Object parent, Object placeholder) {
					queryCompleted.add(parent);
				}
			});
		} else
			viewer = null;
		alreadyQueried = new HashMap<>();
		queryCompleted = new HashSet<>();
		currentInput = newInput;
		for (IInputChangeListener listener : listeners) {
			listener.inputChanged(v, oldInput, newInput);
		}
	}

	@Override
	public Object[] getElements(Object input) {
		if (input instanceof QueriedElement) {
			return getChildren(input);
		}
		return super.getElements(input);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (manager != null) {
			manager.cancel(currentInput);
		}
	}

	@Override
	public boolean hasChildren(Object element) {
		if (manager != null) {
			if (manager.isDeferredAdapter(element))
				return manager.mayHaveChildren(element);
		}
		return super.hasChildren(element);
	}

	@Override
	public Object[] getChildren(final Object parent) {
		if (parent instanceof RemoteQueriedElement) {
			RemoteQueriedElement element = (RemoteQueriedElement) parent;
			// We rely on the assumption that the queryable is the most expensive
			// thing to get vs. the query itself being expensive.
			// (loading a repo vs. querying a repo afterward)
			if (manager != null && !synchronous && (element instanceof MetadataRepositoryElement || element instanceof MetadataRepositories)) {
				if (element.getCachedChildren().length == 0)
					return manager.getChildren(element);
				return element.getChildren(element);
			}
			if (element.hasQueryable())
				return element.getChildren(element);
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
