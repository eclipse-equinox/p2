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

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.*;

/**
 * DeferredQueryTreeContentManager is an extension of DeferredTreeContentManager
 * that associates pending placeholders with their parent elements, so that
 * clients know when a particular parent element is finished fetching its
 * contents.
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

		@Override
		public void setRemoved(boolean removedValue) {
			super.setRemoved(removedValue);
		}

	}

	Object elementRequested;
	ListenerList<IDeferredQueryTreeListener> listeners;
	private AbstractTreeViewer treeViewer;

	public DeferredQueryTreeContentManager(AbstractTreeViewer viewer) {
		super(viewer);
		this.treeViewer = viewer;
		listeners = new ListenerList<>();
	}

	@Override
	protected void addChildren(final Object parent, final Object[] children, IProgressMonitor monitor) {
		// Overridden from original implementation to prevent the use of workbench job!
		Job updateJob = org.eclipse.e4.ui.progress.UIJob.create("Adding children", updateMonitor -> { //$NON-NLS-1$
			// Cancel the job if the tree viewer got closed
			if (treeViewer.getControl().isDisposed() || updateMonitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			treeViewer.add(parent, children);
			return Status.OK_STATUS;
		});
		updateJob.setSystem(true);
		updateJob.schedule();
	}

	/*
	 * Overridden to keep track of the current request long enough to put it in the
	 * pending update adapter.
	 */
	@Override
	public Object[] getChildren(final Object parent) {
		elementRequested = parent;
		return super.getChildren(parent);
	}

	/*
	 * Overridden to signal the start of a fetch
	 */
	@Override
	protected void startFetchingDeferredChildren(final Object parent, final IDeferredWorkbenchAdapter adapter,
			final PendingUpdateAdapter placeholder) {
		if (placeholder instanceof ElementPendingUpdateAdapter)
			notifyListener(true, (ElementPendingUpdateAdapter) placeholder);
		super.startFetchingDeferredChildren(parent, adapter, placeholder);
	}

	@Override
	protected void runClearPlaceholderJob(final PendingUpdateAdapter placeholder) {
		if (placeholder instanceof ElementPendingUpdateAdapter) {
			ElementPendingUpdateAdapter pendingUpdate = (ElementPendingUpdateAdapter) placeholder;
			if (pendingUpdate.isRemoved()) {
				return;
			}
			notifyListener(false, (ElementPendingUpdateAdapter) placeholder);
			Job clearJob = org.eclipse.e4.ui.progress.UIJob.create("Clearing", monitor -> { //$NON-NLS-1$
				if (!pendingUpdate.isRemoved()) {
					Control control = treeViewer.getControl();
					if (control.isDisposed()) {
						return Status.CANCEL_STATUS;
					}
					treeViewer.remove(placeholder);
					pendingUpdate.setRemoved(true);
				}
				return Status.OK_STATUS;
			});
			clearJob.setSystem(true);
			// See bug 470554 if IElementCollector.done() is called immediately
			// after IElementCollector.add(), SWT/GTK seem to be confused.
			// Delay tree element deletion to avoid race conditions with GTK code
			long timeout = Util.isGtk() ? 100 : 0;
			clearJob.schedule(timeout);
			return;
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
