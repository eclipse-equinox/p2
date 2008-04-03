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
package org.eclipse.equinox.p2.tests.updatesite;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSite;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.Feature;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * @since 1.0
 */
public class UpdateSiteTest extends AbstractProvisioningTest {
	/*
	 * Constructor for the class.
	 */
	public UpdateSiteTest(String name) {
		super(name);
	}

	/*
	 * Run all the tests in this class.
	 */
	public static Test suite() {
		return new TestSuite(UpdateSiteTest.class);
	}

	public void testBadDigest() {
		// TODO test the case where we have a site which contains a bad digest file
		// and default to a good site.xml

		// TODO test the case where we have a site which contains a bad digest file
		// and try default to a site.xml but it is bad too

	}

	public void testGoodDigest() {
		// TODO handle the case where we have a site with a good digest file
	}

	public void testBadSiteXML() {
		// handle the case where the site.xml doesn't parse correctly
		File site = getTestData("0.1", "/testData/updatesite/badSiteXML");
		try {
			UpdateSite.load(site.toURL(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// expected exception
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
	}

	/*
	 * Test the case where we don't have a digest or site.xml.
	 */
	public void testNoSite() {
		// ensure we have a validate, empty location
		File temp = getTempFolder();
		temp.mkdirs();
		URL location = null;
		try {
			location = temp.toURL();
		} catch (MalformedURLException e) {
			fail("0.1", e);
		}
		try {
			UpdateSite.load(location, getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// we expect an exception
		}

		try {
			assertNull("1.0", UpdateSite.load(null, getMonitor()));
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
	}

	/*
	 * Test in which we load an update site from a valid site.xml file. Handle
	 * all the variations in the file.
	 */
	public void testSite() {
		File siteLocation = getTestData("0.1", "/testData/updatesite/site");
		UpdateSite site = null;
		try {
			site = UpdateSite.load(siteLocation.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
		Feature[] features = null;
		try {

			features = site.loadFeatures();
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testMirrors() {
		// TODO test the case where the site.xml points to a mirror location
	}
}
