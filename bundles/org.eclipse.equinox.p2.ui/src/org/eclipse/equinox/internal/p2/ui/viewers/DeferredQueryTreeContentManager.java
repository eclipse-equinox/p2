/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.viewers;

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

		public boolean isRemoved() {
			return super.isRemoved();
		}
	}

	Object elementRequested;
	IDeferredQueryTreeListener listener;

	public DeferredQueryTreeContentManager(AbstractTreeViewer viewer) {
		super(viewer);
	}

	/*
	 * Overridden to keep track of the current request long enough
	 * to put it in the pending update adapter.
	 * (non-Javadoc)
	 * @see org.eclipse.ui.progress.DeferredTreeContentManager#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(final Object parent) {
		elementRequested = parent;
		return super.getChildren(parent);
	}

	/*
	 * Overridden to signal the start of a fetch
	 * (non-Javadoc)
	 * @see org.eclipse.ui.progress.DeferredTreeContentManager#startFetchingDeferredChildren(java.lang.Object, org.eclipse.ui.progress.IDeferredWorkbenchAdapter, org.eclipse.ui.progress.PendingUpdateAdapter)
	 */
	protected void startFetchingDeferredChildren(final Object parent, final IDeferredWorkbenchAdapter adapter, final PendingUpdateAdapter placeholder) {
		if (placeholder instanceof ElementPendingUpdateAdapter)
			notifyListener(true, (ElementPendingUpdateAdapter) placeholder);
		super.startFetchingDeferredChildren(parent, adapter, placeholder);
	}

	protected void runClearPlaceholderJob(final PendingUpdateAdapter placeholder) {
		if (placeholder instanceof ElementPendingUpdateAdapter) {
			if (((ElementPendingUpdateAdapter) placeholder).isRemoved() || !PlatformUI.isWorkbenchRunning())
				return;
			notifyListener(false, (ElementPendingUpdateAdapter) placeholder);
		}
		super.runClearPlaceholderJob(placeholder);
	}

	protected PendingUpdateAdapter createPendingUpdateAdapter() {
		return new ElementPendingUpdateAdapter(elementRequested);
	}

	public void setListener(IDeferredQueryTreeListener listener) {
		this.listener = listener;
	}

	private void notifyListener(boolean starting, ElementPendingUpdateAdapter placeholder) {
		if (listener == null)
			return;
		if (starting)
			listener.fetchingDeferredChildren(placeholder.element, placeholder);
		else
			listener.finishedFetchingDeferredChildren(placeholder.element, placeholder);
	}
}
