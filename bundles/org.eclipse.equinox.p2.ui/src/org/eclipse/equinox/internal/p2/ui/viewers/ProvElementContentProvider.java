/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.viewers;

import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ProvElement;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.*;

/**
 * Content provider that retrieves children of a ProvElement.
 * 
 * @since 3.5
 * 
 */
public class ProvElementContentProvider implements ITreeContentProvider {

	private boolean fetchInBackground = false;
	Viewer viewer;
	private Job fetchJob;
	// family is used by test cases
	Object fetchFamily = new Object();

	/**
	 * 
	 */
	public ProvElementContentProvider() {
		// Default constructor
	}

	@Override
	public Object[] getElements(final Object input) {
		// Simple deferred fetch handling for table viewers
		if (fetchInBackground && input instanceof IDeferredWorkbenchAdapter && viewer instanceof AbstractTableViewer) {
			final Display display = viewer.getControl().getDisplay();
			final Object pending = new PendingUpdateAdapter();
			if (fetchJob != null)
				fetchJob.cancel();
			fetchJob = new Job(ProvUIMessages.ProvElementContentProvider_FetchJobTitle) {
				@Override
				protected IStatus run(final IProgressMonitor monitor) {
					IDeferredWorkbenchAdapter parent = (IDeferredWorkbenchAdapter) input;
					final ArrayList<Object> children = new ArrayList<>();
					parent.fetchDeferredChildren(parent, new IElementCollector() {
						@Override
						public void add(Object element, IProgressMonitor mon) {
							if (mon.isCanceled())
								return;
							children.add(element);
						}

						@Override
						public void add(Object[] elements, IProgressMonitor mon) {
							if (mon.isCanceled())
								return;
							children.addAll(Arrays.asList(elements));
						}

						@Override
						public void done() {
							// nothing special to do
						}

					}, monitor);
					if (!monitor.isCanceled()) {
						display.asyncExec(() -> {
							AbstractTableViewer tableViewer = (AbstractTableViewer) viewer;
							if (monitor.isCanceled() || tableViewer == null || tableViewer.getControl().isDisposed())
								return;
							tableViewer.getControl().setRedraw(false);
							tableViewer.remove(pending);
							tableViewer.add(children.toArray());
							finishedFetchingElements(input);
							tableViewer.getControl().setRedraw(true);
						});
					}
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					return family == fetchFamily;
				}

			};
			fetchJob.schedule();
			return new Object[] {pending};
		}
		Object[] elements = getChildren(input);
		finishedFetchingElements(input);
		return elements;
	}

	@Override
	public Object getParent(Object child) {
		if (child instanceof ProvElement) {
			return ((ProvElement) child).getParent(child);
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof ProvElement)
			return ((ProvElement) element).hasChildren(element);
		return false;
	}

	@Override
	public Object[] getChildren(final Object parent) {
		if (parent instanceof ProvElement) {
			return ((ProvElement) parent).getChildren(parent);
		}
		return new Object[0];
	}

	@Override
	public void dispose() {
		viewer = null;
		if (fetchJob != null) {
			fetchJob.cancel();
			fetchJob = null;
		}
	}

	@Override
	public void inputChanged(Viewer aViewer, Object oldInput, Object newInput) {
		this.viewer = aViewer;
		if (fetchJob != null) {
			fetchJob.cancel();
			fetchJob = null;
		}
	}

	protected void finishedFetchingElements(Object parent) {
		// do nothing
	}

	public void setFetchInBackground(boolean fetchInBackground) {
		this.fetchInBackground = fetchInBackground;
	}
}
