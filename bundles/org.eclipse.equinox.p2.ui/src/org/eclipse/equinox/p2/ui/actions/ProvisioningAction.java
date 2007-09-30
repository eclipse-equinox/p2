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

package org.eclipse.equinox.p2.ui.actions;

import org.eclipse.equinox.p2.ui.operations.IOperationConfirmer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;

public abstract class ProvisioningAction extends SelectionProviderAction {

	protected IOperationConfirmer operationConfirmer;
	private Shell shell;

	protected ProvisioningAction(String text, ISelectionProvider selectionProvider, IOperationConfirmer confirmer, Shell shell) {
		super(selectionProvider, text);
		this.operationConfirmer = confirmer;
		this.shell = shell;
		if (this.shell == null) {
			shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		}
	}

	protected Shell getShell() {
		return shell;
	}
}