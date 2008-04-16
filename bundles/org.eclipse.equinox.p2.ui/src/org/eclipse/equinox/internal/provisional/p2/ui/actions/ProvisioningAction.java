/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.actions;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;

public abstract class ProvisioningAction extends SelectionProviderAction {

	private Shell shell;

	protected ProvisioningAction(String text, ISelectionProvider selectionProvider, Shell shell) {
		super(selectionProvider, text);
		this.shell = shell;
		// prime the selection validation
		ISelection selection = selectionProvider.getSelection();
		if (selection instanceof IStructuredSelection) {
			selectionChanged((IStructuredSelection) selection);
		} else {
			selectionChanged(selection);
		}
	}

	protected Shell getShell() {
		if (shell == null)
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		return shell;
	}

	/*
	 * Overridden to use the selection from the selection provider, not the one
	 * from the triggering event.  Some selection providers reinterpret the raw selections
	 * (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionProviderAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public final void selectionChanged(IStructuredSelection selection) {
		ISelection providerSelection = getSelectionProvider().getSelection();
		if (providerSelection instanceof IStructuredSelection) {
			checkEnablement(((IStructuredSelection) providerSelection).toArray());
		} else {
			// shouldn't really happen, but a provider could decide to de-structure the selection
			selectionChanged(providerSelection);
		}
	}

	protected void checkEnablement(Object[] selections) {
		// Default is to nothing
	}

	/**
	 * Recheck the enablement.  Called by clients when some condition outside of
	 * the action that may effect its enablement should be changed.
	 */
	public final void checkEnablement() {
		ISelection selection = getSelection();
		if (selection instanceof IStructuredSelection) {
			checkEnablement(((IStructuredSelection) selection).toArray());
		} else {
			selectionChanged(selection);
		}
	}
}