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
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestSuite;

public class BasicTests extends AbstractReconcilerTest {

	public BasicTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.addTest(new BasicTests("testAddRemove"));
		return suite;
	}

	public void testAddRemove() {
		try {
			// bundle does not exist
			assertFalse("0.1", isInstalled("bbb"));
		} catch (IOException e) {
			fail("0.19", e);
		}

		// copy bundle to dropins
		File jar = getTestData("2.0", "testData/profileSynchronizer/bbb_1.0.0.jar");
		addToDropins("0.2", jar);

		// run reconciler to discover bundle
		reconcile("0.3");

		try {
			// bundle should exist
			assertTrue("0.4", isInstalled("bbb"));
		} catch (IOException e) {
			fail("0.49", e);
		}

		// remove the bundle from the dropins
		removeFromDropins("0.5", "bbb_1.0.0.jar");

		// reconcile to update the configuration
		reconcile("0.6");

		try {
			// bundle should not exist
			assertFalse("0.7", isInstalled("bbb"));
		} catch (IOException e) {
			fail("0.79", e);
		}
	}
}
