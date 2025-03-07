/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.p2.ui.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

abstract class ProvisioningWizardPage extends WizardPage implements ICopyable {

	private final ProvisioningUI ui;
	private final ProvisioningOperationWizard wizard;

	protected ProvisioningWizardPage(String pageName, ProvisioningUI ui, ProvisioningOperationWizard wizard) {
		super(pageName);
		this.wizard = wizard;
		this.ui = ui;
	}

	protected void activateCopy(Control control) {
		CopyUtils.activateCopy(this, control);

	}

	protected ProvisioningOperationWizard getProvisioningWizard() {
		return wizard;
	}

	@Override
	public void copyToClipboard(Control activeControl) {
		String text = getClipboardText(activeControl);
		if (text.length() == 0) {
			return;
		}
		Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
		clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();
	}

	protected abstract String getClipboardText(Control control);

	/**
	 * Save any settings that are related to the bounds of the wizard.
	 * This method is called when the wizard is about to close.
	 */
	public void saveBoundsRelatedSettings() {
		// Default is to do nothing
	}

	protected ProvisioningUI getProvisioningUI() {
		return ui;
	}

	protected Policy getPolicy() {
		return ui.getPolicy();
	}

	String getProfileId() {
		return ui.getProfileId();
	}
}