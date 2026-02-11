/*******************************************************************************
 * Copyright (c) 2008, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import java.io.IOException;
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
	public void testPatchingDroppedInFeature() throws IOException {
		// TODO enable once we fix being able to patch a feature from the drop-ins
		if (DISABLED) {
			return;
		}
		assertInitialized();
		// copy the feature into the dropins folder
		File file = getTestData("1.0", "testData/reconciler/features/myFeature_1.0.0");
		add("dropins/features", file);
		file = getTestData("1.2", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("dropins/plugins", file);
		// reconcile
		reconcile();
		// check all is good
		assertExistsInBundlesInfo("myBundle", "1.0.0");

		// copy the patch into the dropins folder
		file = getTestData("2.0", "testData/reconciler/features/myFeaturePatch_2.0.0");
		add("dropins/features", file);
		file = getTestData("2.2", "testData/reconciler/plugins/myBundle_2.0.0.jar");
		add("dropins/plugins", file);
		// reconcile
		reconcile();
		// check all is good
		assertExistsInBundlesInfo("myBundle", "1.0.0");
		assertExistsInBundlesInfo("myBundle", "2.0.0");

		// cleanup
		remove("dropins/features", "myFeature_1.0.0");
		remove("dropins/plugins", "myBundle_1.0.0.jar");
		remove("dropins/features", "myFeaturePatch_2.0.0");
		remove("dropins/plugins", "myBundle_2.0.0.jar");
		assertDoesNotExistInBundlesInfo("myBundle");
	}

	/*
	 * Test the case where the feature patch adds a new bundle (with a new id) to the system.
	 * For more information see bug 240370.
	 */
	public void testAddBundle() throws IOException {
		// TODO enable this test when bug 240370 is fixed.
		if (DISABLED) {
			return;
		}
		assertInitialized();
		File file = getTestData("1.0", "testData/reconciler/features/myFeature_1.0.0");
		add("dropins/features", file);
		file = getTestData("1.2", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("dropins/plugins", file);
		reconcile();
		assertExistsInBundlesInfo("myBundle", "1.0.0");

		file = getTestData("2.0", "testData/reconciler/features/myFeaturePatch_1.0.0");
		add("dropins/features", file);
		file = getTestData("2.2", "testData/reconciler/plugins/mySingletonBundle_1.0.0.jar");
		add("dropins/plugins", file);
		reconcile();
		assertExistsInBundlesInfo("myBundle", "1.0.0");
		assertExistsInBundlesInfo("mySingletonBundle", "1.0.0");

		remove("dropins/features", "myFeature_1.0.0");
		remove("dropins/plugins", "myBundle_1.0.0.jar");
		remove("dropins/features", "myFeaturePatch_1.0.0");
		remove("dropins/plugins", "mySingletonBundle_1.0.0.jar");
		assertDoesNotExistInBundlesInfo("myBundle");
		assertDoesNotExistInBundlesInfo("mySingletonBundle");
	}

}
