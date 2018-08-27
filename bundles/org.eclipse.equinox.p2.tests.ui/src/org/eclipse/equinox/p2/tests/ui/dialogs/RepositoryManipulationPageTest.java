/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.equinox.p2.ui.RepositoryManipulationPage;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

/**
 * Tests for the Repository Manipulation page.
 * If nothing else, this test ensures that repository page can be hosted
 * somewhere besides preferences
 */
public class RepositoryManipulationPageTest extends AbstractProvisioningUITest {

	class TestDialog extends TitleAreaDialog {
		public TestDialog() {
			super(null);
		}

		RepositoryManipulationPage page;

		@Override
		protected Control createDialogArea(Composite parent) {
			page = new RepositoryManipulationPage();
			page.init(PlatformUI.getWorkbench());
			page.createControl(parent);
			this.setTitle("Software Sites");
			this.setMessage("The enabled sites will be searched for software.  Disabled sites are ignored.");
			return page.getControl();
		}

		@Override
		protected void okPressed() {
			if (page.performOk())
				super.okPressed();
		}

		@Override
		protected void cancelPressed() {
			if (page.performCancel())
				super.cancelPressed();
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
			// reach in
		} finally {
			dialog.close();
		}
	}
}
