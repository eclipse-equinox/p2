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
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSite;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
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

	public void testGoodDigest() {
		File site = getTestData("0.1", "/testData/updatesite/digest");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}

		try {
			updatesite.loadFeatures();
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	/*
	 * Test in which we load an update site from a valid site.xml file. Handle
	 * all the variations in the file.
	 */
	public void testNoDigestGoodSite() {
		File site = getTestData("0.1", "/testData/updatesite/site");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
		try {
			updatesite.loadFeatures();
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testBadDigestGoodSite() {
		File site = getTestData("0.1", "/testData/updatesite/baddigestgoodsite");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
		try {
			updatesite.loadFeatures();
		} catch (ProvisionException e) {
			fail("0.4", e);
		}
	}

	public void testBadDigestBadSite() {
		File site = getTestData("0.1", "/testData/updatesite/baddigestbadsite");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// expected
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
	}

	public void testBadSiteXML() {
		// handle the case where the site.xml doesn't parse correctly
		File site = getTestData("0.1", "/testData/updatesite/badSiteXML");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
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
		try {
			UpdateSite.load(temp.toURL(), getMonitor());
			fail("0.2");
		} catch (ProvisionException e) {
			// we expect an exception
		} catch (MalformedURLException e) {
			fail("0.1", e);
		}
	}

	public void testNullSite() {
		try {
			assertNull("1.0", UpdateSite.load(null, getMonitor()));
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
	}

	public void testBadFeatureURL() {
		File site = getTestData("0.1", "/testData/updatesite/badfeatureurl");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
		try {
			int featureCount = updatesite.loadFeatures().length;
			assertEquals(0, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testGoodFeatureURL() {
		File site = getTestData("0.1", "/testData/updatesite/goodfeatureurl");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
		try {
			int featureCount = updatesite.loadFeatures().length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testIncludedFeature() {
		File site = getTestData("0.1", "/testData/updatesite/includedfeature");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
		try {
			int featureCount = updatesite.loadFeatures().length;
			assertEquals(2, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testIncludedFeatureArchive() {
		File site = getTestData("0.1", "/testData/updatesite/includedfeaturearchive");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
		try {
			int featureCount = updatesite.loadFeatures().length;
			assertEquals(2, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testBadIncludedFeatureArchive() {
		File site = getTestData("0.1", "/testData/updatesite/badincludedfeaturearchive");
		UpdateSite updatesite = null;
		try {
			updatesite = UpdateSite.load(site.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.2", e);
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
		try {
			int featureCount = updatesite.loadFeatures().length;
			assertEquals(1, featureCount);
		} catch (ProvisionException e) {
			fail("0.5");
		}
	}

	public void testMirrors() {
		// TODO test the case where the site.xml points to a mirror location
	}
}
