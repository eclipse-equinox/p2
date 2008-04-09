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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.RemoveColocatedRepositoryOperation;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

public class RemoveColocatedRepositoryAction extends ProvisioningAction {

	public RemoveColocatedRepositoryAction(ISelectionProvider selectionProvider, Shell shell) {
		super(ProvSDKMessages.RemoveColocatedRepositoryAction_Label, selectionProvider, shell);
		setToolTipText(ProvSDKMessages.RemoveColocatedRepositoryAction_Tooltip);
	}

	public void run() {
		RemoveColocatedRepositoryOperation op = new RemoveColocatedRepositoryOperation(ProvSDKMessages.RepositoryManipulationDialog_RemoveOperationLabel, getSelectedURLs(getStructuredSelection().toArray()));
		ProvisioningOperationRunner.run(op, getShell());
	}

	private URL[] getSelectedURLs(Object[] selectionArray) {
		List urls = new ArrayList();
		for (int i = 0; i < selectionArray.length; i++) {
			if (selectionArray[i] instanceof MetadataRepositoryElement)
				urls.add(((MetadataRepositoryElement) selectionArray[i]).getLocation());
		}
		return (URL[]) urls.toArray(new URL[urls.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.actions.ProvisioningAction#structuredSelectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void structuredSelectionChanged(IStructuredSelection selection) {
		setEnabled(getSelectedURLs(selection.toArray()).length > 0);
	}
}
