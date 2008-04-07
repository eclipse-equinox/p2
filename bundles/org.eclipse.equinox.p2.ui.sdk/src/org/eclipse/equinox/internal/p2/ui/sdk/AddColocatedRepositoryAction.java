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

package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.ProvisioningAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

public class AddColocatedRepositoryAction extends ProvisioningAction {

	public AddColocatedRepositoryAction(ISelectionProvider selectionProvider, Shell shell) {
		super("Add Site...", selectionProvider, shell);
		setToolTipText("Add a site used to access the available software");
	}

	public void run() {
		new AddColocatedRepositoryDialog(getShell(), IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM).open();
	}
}
