/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.actions;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.AddColocatedRepositoryDialog;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.jface.viewers.ISelectionProvider;

public class AddColocatedRepositoryAction extends ProvisioningAction {

	public AddColocatedRepositoryAction(ISelectionProvider selectionProvider) {
		super(ProvUIMessages.AddColocatedRepositoryAction_Label, selectionProvider);
		setToolTipText(ProvUIMessages.AddColocatedRepositoryAction_Tooltip);
		init();
	}

	public void run() {
		new AddColocatedRepositoryDialog(getShell(), IRepositoryManager.REPOSITORIES_NON_SYSTEM).open();
	}
}
