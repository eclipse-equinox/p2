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

package org.eclipse.equinox.internal.p2.ui.actions;

import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;

public abstract class ProvisioningAction extends SelectionProviderAction {
	ProvisioningUI ui;

	protected ProvisioningAction(ProvisioningUI ui, String text, ISelectionProvider selectionProvider) {
		super(selectionProvider, text);
		this.ui = ui;
	}

	/*
	 * perform initialization that should be done after creation.
	 */
	protected void init() {
		// prime the selection validation
		ISelection selection = getSelection();
		if (selection instanceof IStructuredSelection) {
			selectionChanged((IStructuredSelection) selection);
		} else {
			selectionChanged(selection);
		}
	}

	protected Shell getShell() {
		return ProvUI.getDefaultParentShell();
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

	protected ProvisioningSession getSession() {
		return ui.getSession();
	}

	protected Policy getPolicy() {
		return ui.getPolicy();
	}

	protected ProvisioningUI getProvisioningUI() {
		return ui;
	}
}