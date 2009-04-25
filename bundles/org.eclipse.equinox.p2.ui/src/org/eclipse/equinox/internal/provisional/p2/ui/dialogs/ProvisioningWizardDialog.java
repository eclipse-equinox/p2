/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

/**
 * Subclass of WizardDialog that provides bounds saving behavior.
 * @since 3.5
 *
 */
public class ProvisioningWizardDialog extends WizardDialog {
	private static final String WIZARD_SETTINGS_SECTION = "P2Wizard"; //$NON-NLS-1$

	public ProvisioningWizardDialog(Shell parent, IWizard wizard) {
		super(parent, wizard);

		setShellStyle(getShellStyle() | SWT.RESIZE);
		setMinimumPageSize(700, 500);
	}

	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(WIZARD_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(WIZARD_SETTINGS_SECTION);
		}
		return section;
	}
}
