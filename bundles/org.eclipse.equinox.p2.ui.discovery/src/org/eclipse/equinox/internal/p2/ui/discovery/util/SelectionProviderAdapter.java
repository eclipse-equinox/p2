/*******************************************************************************
 * Copyright (c) 2009, 2017 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.discovery.util;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.*;

/**
 * Provides an simple implementation of {@link ISelectionProvider} that propagates selection events to registered
 * listeners.
 * 
 * @author Steffen Pingel
 */
public class SelectionProviderAdapter extends EventManager implements ISelectionProvider, ISelectionChangedListener {

	private ISelection selection;

	/**
	 * Constructs a <code>SelectionProviderAdapter</code> and initializes the selection to <code>selection</code>.
	 * 
	 * @param selection
	 *            the initial selection
	 * @see #setSelection(ISelection)
	 */
	public SelectionProviderAdapter(ISelection selection) {
		setSelection(selection);
	}

	/**
	 * Constructs a <code>SelectionProviderAdapter</code> with a <code>null</code> selection.
	 */
	public SelectionProviderAdapter() {
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		addListenerObject(listener);
	}

	@Override
	public ISelection getSelection() {
		return selection;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		removeListenerObject(listener);
	}

	@Override
	public void selectionChanged(final SelectionChangedEvent event) {
		this.selection = event.getSelection();
		Object[] listeners = getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ISelectionChangedListener listener = (ISelectionChangedListener) listeners[i];
			SafeRunner.run(new SafeRunnable() {
				@Override
				public void run() {
					listener.selectionChanged(event);
				}
			});
		}
	}

	@Override
	public void setSelection(ISelection selection) {
		selectionChanged(new SelectionChangedEvent(this, selection));
	}

}
