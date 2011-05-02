/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
import junit.framework.Test;
import junit.framework.TestSuite;

/*
 * Test suite for feature patches.
 */
public class FeaturePatchTest extends AbstractReconcilerTest {

	public FeaturePatchTest(String name) {
		super(name);
	}

	/*
	 * The list of tests for this class. Order is important since some of them rely
	 * on the state from the previous test run.
	 */
	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(FeaturePatchTest.class.getName());
		suite.addTest(new FeaturePatchTest("testPatchingDroppedInFeature"));
		suite.addTest(new FeaturePatchTest("testAddBundle"));
		return suite;
	}

	/*
	 * Test the case where we have a feature in the drop-ins folder and then
	 * we try and apply a feature patch to it.
	 */
	public void testPatchingDroppedInFeature() {
		// TODO enable once we fix being able to patch a feature from the drop-ins
		if (DISABLED)
			return;
		assertInitialized();
		// copy the feature into the dropins folder
		File file = getTestData("1.0", "testData/reconciler/features/myFeature_1.0.0");
		add("1.1", "dropins/features", file);
		file = getTestData("1.2", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("1.3", "dropins/plugins", file);
		// reconcile
		reconcile("1.4");
		// check all is good
		assertExistsInBundlesInfo("1.5", "myBundle", "1.0.0");

		// copy the patch into the dropins folder
		file = getTestData("2.0", "testData/reconciler/features/myFeaturePatch_2.0.0");
		add("2.1", "dropins/features", file);
		file = getTestData("2.2", "testData/reconciler/plugins/myBundle_2.0.0.jar");
		add("2.3", "dropins/plugins", file);
		// reconcile
		reconcile("2.4");
		// check all is good
		assertExistsInBundlesInfo("2.5", "myBundle", "1.0.0");
		assertExistsInBundlesInfo("2.6", "myBundle", "2.0.0");

		// cleanup
		remove("3.0", "dropins/features", "myFeature_1.0.0");
		remove("3.1", "dropins/plugins", "myBundle_1.0.0.jar");
		remove("3.2", "dropins/features", "myFeaturePatch_2.0.0");
		remove("3.3", "dropins/plugins", "myBundle_2.0.0.jar");
		assertDoesNotExistInBundlesInfo("3.4", "myBundle");
	}

	/*
	 * Test the case where the feature patch adds a new bundle (with a new id) to the system.
	 * For more information see bug 240370.
	 */
	public void testAddBundle() {
		// TODO enable this test when bug 240370 is fixed.
		if (DISABLED)
			return;
		assertInitialized();
		File file = getTestData("1.0", "testData/reconciler/features/myFeature_1.0.0");
		add("1.1", "dropins/features", file);
		file = getTestData("1.2", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("1.3", "dropins/plugins", file);
		reconcile("1.4");
		assertExistsInBundlesInfo("1.5", "myBundle", "1.0.0");

		file = getTestData("2.0", "testData/reconciler/features/myFeaturePatch_1.0.0");
		add("2.1", "dropins/features", file);
		file = getTestData("2.2", "testData/reconciler/plugins/mySingletonBundle_1.0.0.jar");
		add("2.3", "dropins/plugins", file);
		reconcile("2.4");
		assertExistsInBundlesInfo("2.5", "myBundle", "1.0.0");
		assertExistsInBundlesInfo("2.6", "mySingletonBundle", "1.0.0");

		remove("3.0", "dropins/features", "myFeature_1.0.0");
		remove("3.1", "dropins/plugins", "myBundle_1.0.0.jar");
		remove("3.2", "dropins/features", "myFeaturePatch_1.0.0");
		remove("3.3", "dropins/plugins", "mySingletonBundle_1.0.0.jar");
		assertDoesNotExistInBundlesInfo("3.4", "myBundle");
		assertDoesNotExistInBundlesInfo("3.5", "mySingletonBundle");
	}

}
