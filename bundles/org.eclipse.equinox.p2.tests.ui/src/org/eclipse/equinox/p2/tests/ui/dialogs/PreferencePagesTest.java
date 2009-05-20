/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Tests for the install wizard
 */
public class PreferencePagesTest extends AbstractProvisioningUITest {

	private static final String GENERAL = "org.eclipse.equinox.internal.p2.ui.sdk.ProvisioningPreferencePage";
	private static final String SITES = "org.eclipse.equinox.internal.p2.ui.sdk.SitesPreferencePage";
	private static final String AUTOUPDATES = "org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdatesPreferencePage";

	public void testGeneralPage() {
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, GENERAL, null, null);
		dialog.setBlockOnOpen(false);
		dialog.open();
		try {
			// nothing yet
		} finally {
			dialog.close();
		}
	}

	public void testCopyrightPage() {
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, SITES, null, null);
		dialog.setBlockOnOpen(false);
		dialog.open();
		try {
			// nothing yet
		} finally {
			dialog.close();
		}
	}

	public void testLicensePage() {
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, AUTOUPDATES, null, null);
		dialog.setBlockOnOpen(false);
		dialog.open();
		try {
			// nothing yet
		} finally {
			dialog.close();
		}
	}
}
