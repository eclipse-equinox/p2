/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.equinox.p2.ui.InstalledSoftwarePage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Tests for the Installation Dialog page.
 */
public class InstalledSoftwarePageTest extends AbstractProvisioningUITest {

	class TestDialog extends Dialog {
		TestDialog() {
			super(ProvUI.getDefaultParentShell());
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			InstalledSoftwarePage page = new InstalledSoftwarePage();
			page.createControl(composite);
			return composite;
		}
	}

	/**
	 * Tests the dialog
	 */
	public void testDialog() {
		TestDialog dialog = new TestDialog();
		dialog.setBlockOnOpen(false);
		dialog.open();
		try {
			// need to reach way in to shell data to find dialog
		} finally {
			dialog.close();
		}
	}
}
