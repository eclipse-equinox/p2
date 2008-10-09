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
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.RemoveColocatedRepositoryOperation;
import org.eclipse.jface.viewers.ISelectionProvider;

public class RemoveColocatedRepositoryAction extends ColocatedRepositoryAction {

	public RemoveColocatedRepositoryAction(ISelectionProvider selectionProvider) {
		super(ProvUIMessages.RemoveColocatedRepositoryAction_Label, ProvUIMessages.RemoveColocatedRepositoryAction_Tooltip, selectionProvider);

	}

	protected ProvisioningOperation getOperation() {
		return new RemoveColocatedRepositoryOperation(ProvUIMessages.RemoveColocatedRepositoryAction_OperationLabel, getSelectedLocations(getStructuredSelection().toArray()));
	}
}
