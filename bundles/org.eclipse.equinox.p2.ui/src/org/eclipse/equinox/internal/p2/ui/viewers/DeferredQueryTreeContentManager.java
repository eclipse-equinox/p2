/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.viewers;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.*;

/**
 * DeferredQueryTreeContentManager is an extension of DeferredTreeContentManager
 * that associates pending placeholders with their parent elements, so that
 * clients know when a particular parent element is finished fetching
 * its contents.
 * 
 * @since 3.4
 *
 */
public class DeferredQueryTreeContentManager extends DeferredTreeContentManager {

	class ElementPendingUpdateAdapter extends PendingUpdateAdapter {
		Object element;

		ElementPendingUpdateAdapter(Object element) {
			super();
			this.element = element;
		}

		@Override
		public boolean isRemoved() {
			return super.isRemoved();
		}
	}

	Object elementRequested;
	ListenerList<IDeferredQueryTreeListener> listeners;

	public DeferredQueryTreeContentManager(AbstractTreeViewer viewer) {
		super(viewer);
		listeners = new ListenerList<>();
	}

	/*
	 * Overridden to keep track of the current request long enough
	 * to put it in the pending update adapter.
	 * (non-Javadoc)
	 * @see org.eclipse.ui.progress.DeferredTreeContentManager#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(final Object parent) {
		elementRequested = parent;
		return super.getChildren(parent);
	}

	/*
	 * Overridden to signal the start of a fetch
	 * (non-Javadoc)
	 * @see org.eclipse.ui.progress.DeferredTreeContentManager#startFetchingDeferredChildren(java.lang.Object, org.eclipse.ui.progress.IDeferredWorkbenchAdapter, org.eclipse.ui.progress.PendingUpdateAdapter)
	 */
	@Override
	protected void startFetchingDeferredChildren(final Object parent, final IDeferredWorkbenchAdapter adapter, final PendingUpdateAdapter placeholder) {
		if (placeholder instanceof ElementPendingUpdateAdapter)
			notifyListener(true, (ElementPendingUpdateAdapter) placeholder);
		super.startFetchingDeferredChildren(parent, adapter, placeholder);
	}

	@Override
	protected void runClearPlaceholderJob(final PendingUpdateAdapter placeholder) {
		if (placeholder instanceof ElementPendingUpdateAdapter) {
			if (((ElementPendingUpdateAdapter) placeholder).isRemoved() || !PlatformUI.isWorkbenchRunning())
				return;
			notifyListener(false, (ElementPendingUpdateAdapter) placeholder);
		}
		super.runClearPlaceholderJob(placeholder);
	}

	@Override
	protected PendingUpdateAdapter createPendingUpdateAdapter() {
		return new ElementPendingUpdateAdapter(elementRequested);
	}

	public void addListener(IDeferredQueryTreeListener listener) {
		if (listener != null) {
			this.listeners.add(listener);
		}
	}

	private void notifyListener(boolean starting, ElementPendingUpdateAdapter placeholder) {
		if (listeners == null || listeners.isEmpty())
			return;
		if (starting) {
			for (IDeferredQueryTreeListener deferredQueryTreeListener : listeners) {
				deferredQueryTreeListener.fetchingDeferredChildren(placeholder.element, placeholder);
			}
		} else {
			for (IDeferredQueryTreeListener deferredQueryTreeListener : listeners) {
				deferredQueryTreeListener.finishedFetchingDeferredChildren(placeholder.element, placeholder);
			}
		}
	}
}
