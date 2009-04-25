/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

abstract class ProvisioningWizardPage extends WizardPage implements ICopyable {

	protected ProvisioningWizardPage(String pageName) {
		super(pageName);
	}

	protected void activateCopy(Control control) {
		CopyUtils.activateCopy(this, control);

	}

	public void copyToClipboard(Control activeControl) {
		String text = getClipboardText(activeControl);
		if (text.length() == 0)
			return;
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
}