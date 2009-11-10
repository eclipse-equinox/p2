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

package org.eclipse.equinox.internal.p2.ui.actions;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.RemoveColocatedRepositoryJob;
import org.eclipse.equinox.p2.operations.RepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;

public class RemoveColocatedRepositoryAction extends ColocatedRepositoryAction {

	public RemoveColocatedRepositoryAction(ProvisioningUI ui, ISelectionProvider selectionProvider) {
		super(ui, ProvUIMessages.RemoveColocatedRepositoryAction_Label, ProvUIMessages.RemoveColocatedRepositoryAction_Tooltip, selectionProvider);

	}

	protected RepositoryJob getOperation() {
		return new RemoveColocatedRepositoryJob(ProvUIMessages.RemoveColocatedRepositoryAction_OperationLabel, ui.getSession(), getSelectedLocations(getStructuredSelection().toArray()));
	}
}
