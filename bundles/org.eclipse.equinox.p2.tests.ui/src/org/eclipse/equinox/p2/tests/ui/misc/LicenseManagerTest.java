/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.misc;

import java.net.URI;
import org.eclipse.equinox.internal.p2.ui.sdk.SimpleLicenseManager;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.equinox.p2.ui.LicenseManager;

public class LicenseManagerTest extends AbstractProvisioningUITest {

	class SimpleLicense implements ILicense {
		String body;
		String id;

		SimpleLicense(String id, String body) {
			this.id = id;
			this.body = body;
		}

		public URI getLocation() {
			return null;
		}

		public String getBody() {
			return body;
		}

		public String getUUID() {
			return id;
		}

	}

	public void testLicenseAcceptAndReject() {
		LicenseManager manager = getProvisioningUI().getLicenseManager();
		SimpleLicense foo = new SimpleLicense("foo", "foo");
		SimpleLicense bar = new SimpleLicense("bar", "bar");

		manager.accept(foo);
		manager.accept(bar);

		assertTrue("1.0", manager.hasAcceptedLicenses());
		manager.reject(foo);
		manager.reject(bar);
		assertFalse("1.1", manager.hasAcceptedLicenses());

		manager.accept(foo);
		manager.accept(bar);
		assertTrue("1.2", manager.hasAcceptedLicenses());
	}

	public void testDifferentProfilesDifferentLicenses() {
		LicenseManager manager = getProvisioningUI().getLicenseManager();
		SimpleLicense foo = new SimpleLicense("foo", "foo");
		SimpleLicense bar = new SimpleLicense("bar", "bar");

		manager.accept(foo);
		manager.accept(bar);

		assertTrue("1.0", manager.hasAcceptedLicenses());

		// Ensure that a newly created license manager with the same profile has the
		// same licenses
		SimpleLicenseManager manager2 = new SimpleLicenseManager(TESTPROFILE);
		assertTrue("1.1", manager2.hasAcceptedLicenses());

		// A manager with a different profile would not have the same licenses
		profile = createProfile("ANOTHER");
		manager2 = new SimpleLicenseManager("ANOTHER");
		assertFalse("1.2", manager2.hasAcceptedLicenses());
	}
}
