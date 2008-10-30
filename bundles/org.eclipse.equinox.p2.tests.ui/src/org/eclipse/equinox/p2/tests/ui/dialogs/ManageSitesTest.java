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
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.RepositoryManipulationDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;

/**
 * Tests for the install wizard
 */
public class ManageSitesTest extends AbstractProvisioningUITest {

	/**
	 * Tests the dialog
	 */
	public void testDialog() {
		RepositoryManipulationDialog dialog = new RepositoryManipulationDialog(ProvUI.getDefaultParentShell(), Policy.getDefault());
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			// reach in and perform tests.
		} finally {
			dialog.getShell().close();
		}
	}
}
