/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.actions;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;

public class RemoveColocatedRepositoryAction extends ColocatedRepositoryAction {

	public RemoveColocatedRepositoryAction(ProvisioningUI ui, ISelectionProvider selectionProvider) {
		super(ui, ProvUIMessages.RemoveColocatedRepositoryAction_Label, ProvUIMessages.RemoveColocatedRepositoryAction_Tooltip, selectionProvider);

	}

	@Override
	public void run() {
		ui.getRepositoryTracker().removeRepositories(getSelectedLocations(getStructuredSelection().toArray()), ui.getSession());
	}
}
