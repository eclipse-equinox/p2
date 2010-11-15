/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import java.net.MalformedURLException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.update.Configuration;

public class BasicProductTests extends AbstractSharedBundleProductTest {

	public BasicProductTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new SharedBundleProductTestSuite();
		suite.setName(BasicProductTests.class.getName());
		suite.addTest(new BasicProductTests("testAddRemove"));
		suite.addTest(new BasicProductTests("testReplace"));
		return suite;
	}

	public void testAddRemove() {
		// setup
		assertInitialized();
		File jar = getTestData("1.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		File shared = new File(output, "shared/plugins");
		File target = new File(shared, jar.getName());
		copy("1.1", jar, target);
		String targetURLString = null;
		try {
			targetURLString = target.toURI().toURL().toExternalForm();
		} catch (MalformedURLException e) {
			fail("1.99", e);
		}

		// add the bundle to the platform.xml and reconcile
		Configuration config = loadConfiguration();
		addBundleToConfiguration(config, targetURLString);
		saveConfiguration(config);
		reconcile("4.0");
		assertExistsInBundlesInfo("5.0", "myBundle");

		// cleanup/remove the bundle and verify
		assertTrue("6.0", delete(target));
		removeBundleFromConfiguration(config, targetURLString);
		reconcile("6.5");
		assertDoesNotExistInBundlesInfo("7.0", "myBundle");
	}

	public void testReplace() {
		// setup
		assertInitialized();
		File jar = getTestData("1.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		File shared = new File(output, "shared/plugins");
		File target = new File(shared, jar.getName());
		copy("1.1", jar, target);
		String targetURLString = null;
		try {
			targetURLString = target.toURI().toURL().toExternalForm();
		} catch (MalformedURLException e) {
			fail("1.99", e);
		}

		// add bundle to platform.xml and reconcile
		Configuration config = loadConfiguration();
		addBundleToConfiguration(config, targetURLString);
		saveConfiguration(config);
		reconcile("2.0");
		assertExistsInBundlesInfo("2.1", "myBundle", "1.0.0");

		// replace with a higher version and reconcile
		// leave the old version in the shared bundle area
		File higherJAR = getTestData("3.0", "testData/reconciler/plugins/myBundle_2.0.0.jar");
		File higherTarget = new File(shared, higherJAR.getName());
		copy("3.1", higherJAR, higherTarget);
		String higherTargetURLString = null;
		try {
			higherTargetURLString = higherTarget.toURI().toURL().toExternalForm();
		} catch (MalformedURLException e) {
			fail("3.99", e);
		}
		removeBundleFromConfiguration(config, targetURLString);
		addBundleToConfiguration(config, higherTargetURLString);
		saveConfiguration(config);
		reconcile("3.5");
		assertExistsInBundlesInfo("3.6", "myBundle", "2.0.0");
		assertDoesNotExistInBundlesInfo("3.7.0", "myBundle", "1.0.0");

		// cleanup/remove and verify
		assertTrue("6.0", delete(target));
		assertTrue("6.1", delete(higherTarget));
		removeBundleFromConfiguration(config, higherTargetURLString);
		reconcile("6.5");
		assertDoesNotExistInBundlesInfo("7.0", "myBundle");
	}
}
