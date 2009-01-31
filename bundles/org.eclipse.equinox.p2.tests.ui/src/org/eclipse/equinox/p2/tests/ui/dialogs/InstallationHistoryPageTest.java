/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.RevertProfilePage;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Tests for the Installation History page.
 * If nothing else, this test ensures that installation history can be hosted
 * somewhere besides the about dialog
 */
public class InstallationHistoryPageTest extends AbstractProvisioningUITest {

	class TestDialog extends Dialog {
		RevertProfilePage page;

		TestDialog() {
			super(ProvUI.getDefaultParentShell());
		}

		protected Control createDialogArea(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			page = new RevertProfilePage();
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

		} finally {
			dialog.close();
		}
	}
}
