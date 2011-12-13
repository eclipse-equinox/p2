/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

/**
 * Test for Bug 362692. Inconsistent results from reconciliation when starting
 * with -clean. Data set listed below. Put the bundles in the drop-ins and start
 * with -clean and ensure the highest version of the bundles is loaded.
 * 
 * B v1, v2
 * C v1, v2
 * D v1, v2
 * Both B and C depend on D [1.0.0, any)
 */
public class Bug362692 extends AbstractReconcilerTest {

	public Bug362692(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(Bug362692.class.getName());
		suite.addTest(new Bug362692("testReconcile"));
		return suite;
	}

	public void testReconcile() {
		// assert initial state
		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.1", "b");
		assertDoesNotExistInBundlesInfo("0.2", "c");
		assertDoesNotExistInBundlesInfo("0.3", "d");

		// add bundle to dropins
		File plugins = getTestData("2.0", "testData/bug362692/plugins");
		add("2.1", "dropins", plugins);

		// reconcile + clean
		reconcile("3.0", true);

		// assert highest versions of bundles are installed
		assertExistsInBundlesInfo("4.0", "b", "2.0.0", "dropins");
		assertExistsInBundlesInfo("4.1", "c", "2.0.0", "dropins");
		assertExistsInBundlesInfo("4.2", "d", "2.0.0", "dropins");

		// reconcile + clean
		reconcile("6.0", true);

		// assert highest versions of bundles still are installed
		assertExistsInBundlesInfo("7.0", "b", "2.0.0", "dropins");
		assertExistsInBundlesInfo("7.1", "c", "2.0.0", "dropins");
		assertExistsInBundlesInfo("7.2", "d", "2.0.0", "dropins");

		// cleanup
		remove("99.0", "dropins", "plugins");
		reconcile("99.1", true);
		assertDoesNotExistInBundlesInfo("99.2", "b");
		assertDoesNotExistInBundlesInfo("99.3", "c");
		assertDoesNotExistInBundlesInfo("99.4", "d");
	}
}
