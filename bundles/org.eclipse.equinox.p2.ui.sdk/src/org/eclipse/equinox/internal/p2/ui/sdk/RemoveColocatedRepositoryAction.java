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

import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.RemoveColocatedRepositoryOperation;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;

public class RemoveColocatedRepositoryAction extends ColocatedRepositoryAction {

	public RemoveColocatedRepositoryAction(ISelectionProvider selectionProvider, Shell shell) {
		super(ProvSDKMessages.RemoveColocatedRepositoryAction_Label, ProvSDKMessages.RemoveColocatedRepositoryAction_Tooltip, selectionProvider, shell);

	}

	protected ProvisioningOperation getOperation() {
		return new RemoveColocatedRepositoryOperation(ProvSDKMessages.RepositoryManipulationDialog_RemoveOperationLabel, getSelectedURLs(getStructuredSelection().toArray()));
	}
}
