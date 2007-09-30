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

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.equinox.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ListSelectionDialog;

public class UpdateAction extends ProfileModificationAction {

	private final class UpdateListSelectionDialog extends ListSelectionDialog {
		UpdateListSelectionDialog(Shell parentShell, Object input, IStructuredContentProvider contentProvider, ILabelProvider labelProvider, String message) {
			super(parentShell, input, contentProvider, labelProvider, message);
		}

		protected Control createDialogArea(Composite parent) {
			Control control = super.createDialogArea(parent);
			Table table = getViewer().getTable();
			table.setHeaderVisible(true);
			TableColumn tc = new TableColumn(table, SWT.LEFT, 0);
			tc.setResizable(true);
			tc.setWidth(200);
			tc = new TableColumn(table, SWT.LEFT, 1);
			tc.setWidth(200);
			tc.setResizable(true);
			getViewer().setInput(new Object());
			return control;
		}
	}

	private final class UpdateContentProvider implements IStructuredContentProvider {
		private final Object[] elements;

		UpdateContentProvider(Object[] elements) {
			this.elements = elements;
		}

		public Object[] getElements(Object inputElement) {
			return elements;
		}

		public void dispose() {
			// nothing to dispose
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// input is static
		}
	}

	public UpdateAction(String text, ISelectionProvider selectionProvider, IOperationConfirmer confirmer, Profile profile, IProfileChooser chooser, Shell shell) {
		super(text, selectionProvider, confirmer, profile, chooser, shell);
	}

	protected ProfileModificationOperation validateAndGetOperation(IInstallableUnit[] ius, Profile targetProfile, IProgressMonitor monitor) {
		// Collect the replacements for each IU individually so that 
		// the user can decide what to update
		try {
			Collection[] replacements = new Collection[ius.length];
			ArrayList iusWithUpdates = new ArrayList();
			for (int i = 0; i < ius.length; i++) {
				replacements[i] = ProvisioningUtil.updatesFor(ius[i], monitor);
				if (replacements[i].size() > 0)
					iusWithUpdates.add(ius[i]);
			}
			if (iusWithUpdates.size() > 0) {
				final Object[] elements = iusWithUpdates.toArray();
				ListSelectionDialog dialog = new UpdateListSelectionDialog(getShell(), new Object(), new UpdateContentProvider(elements), new IUDetailsLabelProvider(), ProvUIMessages.UpdateAction_UpdatesAvailableMessage);
				dialog.setInitialSelections(elements);
				dialog.setTitle(ProvUIMessages.UpdateAction_UpdatesAvailableTitle);
				int ret = dialog.open();
				IInstallableUnit[] iusToUpdate = new IInstallableUnit[0];
				if (ret != Window.CANCEL) {
					Object[] result = dialog.getResult();
					if (result != null && result.length > 0) {
						iusToUpdate = (IInstallableUnit[]) Arrays.asList(dialog.getResult()).toArray(new IInstallableUnit[result.length]);
						IInstallableUnit[] replacementIUs = ProvisioningUtil.updatesFor(iusToUpdate, targetProfile, monitor, ProvUI.getUIInfoAdapter(getShell()));
						if (replacementIUs.length > 0) {
							return new UpdateOperation(ProvUIMessages.Ops_UpdateIUOperationLabel, targetProfile.getProfileId(), iusToUpdate, replacementIUs);
						}
					}
				}
			} else {
				MessageDialog.openInformation(getShell(), ProvUIMessages.UpdateAction_UpdateInformationTitle, ProvUIMessages.UpdateOperation_NothingToUpdate);
			}
		} catch (ProvisionException e) {
			// fall through and return null
		}
		return null;
	}
}