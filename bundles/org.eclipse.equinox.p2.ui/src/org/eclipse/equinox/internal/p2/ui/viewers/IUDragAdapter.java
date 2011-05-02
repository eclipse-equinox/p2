/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.viewers;

import java.util.*;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Control;

/**
 * Implements drag behaviour when IU items are dragged from a repository view.
 * 
 * @since 3.4
 */
public class IUDragAdapter extends DragSourceAdapter {

	ISelectionProvider selectionProvider;

	/**
	 * Constructs a new drag adapter.
	 * 
	 * @param provider
	 *            The selection provider
	 */
	public IUDragAdapter(ISelectionProvider provider) {
		selectionProvider = provider;
	}

	/**
	 * Set the drag data to represent the local selection of IU's if possible.
	 * Fallback to using a text description of each IU.
	 */
	public void dragSetData(DragSourceEvent event) {
		IInstallableUnit[] ius = getSelectedIUs();

		if (ius == null || ius.length == 0) {
			return;
		}

		// use local selection transfer if possible
		if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
			event.data = LocalSelectionTransfer.getTransfer().getSelection();
			return;
		}
		// resort to a text transfer
		if (!TextTransfer.getInstance().isSupportedType(event.dataType)) {
			return;
		}

		// Get a text description of each IU and set as the drag data
		final StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < ius.length; i++) {
			buffer.append(ius[i].toString());
			buffer.append('\n');
		}
		event.data = buffer.toString();
	}

	/**
	 * Start the drag only if the selection contains IUs.
	 */
	public void dragStart(DragSourceEvent event) {

		// Focus workaround copied from navigator drag adapter
		DragSource dragSource = (DragSource) event.widget;
		Control control = dragSource.getControl();
		if (control != control.getDisplay().getFocusControl()) {
			event.doit = false;
			return;
		}

		// Check the selection
		IStructuredSelection selection = (IStructuredSelection) selectionProvider.getSelection();
		// No drag if nothing is selected
		if (selection.isEmpty()) {
			event.doit = false;
			return;
		}
		if (!areOnlyIUsSelected(selection)) {
			event.doit = false;
			return;
		}
		LocalSelectionTransfer.getTransfer().setSelection(selection);
		event.doit = true;
	}

	private IInstallableUnit[] getSelectedIUs() {
		List<IInstallableUnit> ius = new ArrayList<IInstallableUnit>();

		ISelection selection = selectionProvider.getSelection();
		if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
			return null;
		}
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;

		Iterator<?> iter = structuredSelection.iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
			if (iu != null) {
				ius.add(iu);
			}
		}
		return ius.toArray(new IInstallableUnit[ius.size()]);
	}

	private boolean areOnlyIUsSelected(IStructuredSelection selection) {
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
			if (iu == null) {
				return false;
			}
		}
		return true;
	}
}
