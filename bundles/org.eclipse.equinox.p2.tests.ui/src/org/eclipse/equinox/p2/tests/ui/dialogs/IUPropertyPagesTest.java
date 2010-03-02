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

import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.PropertyDialog;

/**
 * Tests for the install wizard
 */
public class IUPropertyPagesTest extends AbstractProvisioningUITest {

	private static final String GENERAL = "org.eclipse.equinox.p2.ui.sdk.IUGeneralInfoPropertyPage";
	private static final String COPYRIGHT = "org.eclipse.equinox.p2.ui.sdk.IUCopyrightPropertyPage";
	private static final String LICENSE = "org.eclipse.equinox.p2.ui.sdk.IULicensePropertyPage";

	public void testGeneralPage() throws URISyntaxException {
		PropertyDialog dialog = PropertyDialog.createDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), GENERAL, getIU());
		dialog.setBlockOnOpen(false);
		dialog.open();
		try {
			// nothing yet
		} finally {
			dialog.close();
		}
	}

	public void testCopyrightPage() throws URISyntaxException {
		PropertyDialog dialog = PropertyDialog.createDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), COPYRIGHT, getIU());
		dialog.setBlockOnOpen(false);
		dialog.open();
		try {
			// nothing yet
		} finally {
			dialog.close();
		}
	}

	public void testLicensePage() throws URISyntaxException {
		PropertyDialog dialog = PropertyDialog.createDialogOn(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), LICENSE, getIU());
		dialog.setBlockOnOpen(false);
		dialog.open();
		try {
			// nothing yet
		} finally {
			dialog.close();
		}
	}

	private IInstallableUnit getIU() throws URISyntaxException {
		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("TestIU");
		iuDescription.setVersion(Version.createOSGi(1, 0, 0));
		iuDescription.setProperty(IInstallableUnit.PROP_PROVIDER, "Test Cases");
		iuDescription.setProperty(IInstallableUnit.PROP_DESCRIPTION, "A description");
		iuDescription.setProperty(IInstallableUnit.PROP_NAME, "The Biggest Baddest Test IU");
		iuDescription.setLicenses(new ILicense[] {MetadataFactory.createLicense(new URI("http://example.com"), "This is an example license")});
		iuDescription.setCopyright(MetadataFactory.createCopyright(new URI("http://example.com"), "This is an example copyright"));
		return MetadataFactory.createInstallableUnit(iuDescription);
	}
}
