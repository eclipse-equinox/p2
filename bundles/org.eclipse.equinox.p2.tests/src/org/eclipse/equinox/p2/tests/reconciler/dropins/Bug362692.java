/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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

	public void testReconcile() throws IOException {
		// assert initial state
		assertInitialized();
		assertDoesNotExistInBundlesInfo("b");
		assertDoesNotExistInBundlesInfo("c");
		assertDoesNotExistInBundlesInfo("d");

		// add bundle to dropins
		File plugins = getTestData("2.0", "testData/bug362692/plugins");
		add("dropins", plugins);

		// reconcile + clean
		reconcile(true);

		// assert highest versions of bundles are installed
		assertExistsInBundlesInfo("b", "2.0.0", "dropins");
		assertExistsInBundlesInfo("c", "2.0.0", "dropins");
		assertExistsInBundlesInfo("d", "2.0.0", "dropins");

		// reconcile + clean
		reconcile(true);

		// assert highest versions of bundles still are installed
		assertExistsInBundlesInfo("b", "2.0.0", "dropins");
		assertExistsInBundlesInfo("c", "2.0.0", "dropins");
		assertExistsInBundlesInfo("d", "2.0.0", "dropins");

		// cleanup
		remove("dropins", "plugins");
		reconcile(true);
		assertDoesNotExistInBundlesInfo("b");
		assertDoesNotExistInBundlesInfo("c");
		assertDoesNotExistInBundlesInfo("d");
	}
}
