/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 *     Ericsson AB (Hamdan Msheik) 
 *     Ericsson AB (Pascal Rapicault)
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration;

import org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdatePlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

/**
 * Subclass of WizardDialog that provides bounds saving behavior.
 * @since 3.5
 *
 */
public class MigrationWizardDialog extends WizardDialog {
	private ProvisioningOperationWizard wizard;

	public MigrationWizardDialog(Shell parent, ProvisioningOperationWizard wizard) {
		super(parent, wizard);
		this.wizard = wizard;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = AutomaticUpdatePlugin.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(wizard.getDialogSettingsSectionName());
		if (section == null) {
			section = settings.addNewSection(wizard.getDialogSettingsSectionName());
			// Set initial bound values for the MigrationWizardDialog so that it does not cover all the height of the screen when migrating a large set of IUs.
			section.put("DIALOG_WIDTH", 883);
			section.put("DIALOG_HEIGHT", 691);

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

}
