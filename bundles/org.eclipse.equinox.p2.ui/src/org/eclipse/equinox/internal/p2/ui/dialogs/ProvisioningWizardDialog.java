/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 506432
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

/**
 * Subclass of WizardDialog that provides bounds saving behavior.
 * @since 3.5
 *
 */
public class ProvisioningWizardDialog extends WizardDialog {
	private ProvisioningOperationWizard wizard;

	public ProvisioningWizardDialog(Shell parent, ProvisioningOperationWizard wizard) {
		super(parent, wizard);
		this.wizard = wizard;
		setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(false);
	}

	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(wizard.getDialogSettingsSectionName());
		if (section == null) {
			section = settings.addNewSection(wizard.getDialogSettingsSectionName());
		}
		return section;
	}

	/**
	 * @see org.eclipse.jface.window.Window#close()
	 */
	public boolean close() {
		if (getShell() != null && !getShell().isDisposed()) {
			wizard.saveBoundsRelatedSettings();
		}
		return super.close();
	}

	/**
	 * This method is provided only for automated testing.
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public Button testGetButton(int id) {
		return getButton(id);
	}
}
