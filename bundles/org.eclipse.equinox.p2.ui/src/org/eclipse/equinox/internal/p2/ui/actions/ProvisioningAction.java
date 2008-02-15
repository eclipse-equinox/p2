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

package org.eclipse.equinox.internal.p2.ui.actions;

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
}