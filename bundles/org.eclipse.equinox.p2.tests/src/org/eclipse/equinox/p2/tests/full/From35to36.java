/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.tests.reconciler.dropins.AbstractReconcilerTest;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

public class From35to36 extends AbstractReconcilerTest {

	public From35to36(String string) {
		super(string);
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite("org.eclipse.equinox.p2.reconciler.tests.35.platform.archive");
		suite.addTest(new From35to36("from35To36"));
		return suite;
	}

	public void from35To36() {
		String currentBuildRepo = System.getProperty("org.eclipse.equinox.p2.tests.current.build.repo");
		if (currentBuildRepo == null)
			currentBuildRepo = "http://download.eclipse.org/eclipse/updates/3.6-I-builds";
		runInitialize("Initializing 3.5 to get the profile paths properly setup.");
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), new File(output, "eclipse/p2/org.eclipse.equinox.p2.engine/profileRegistry/"), null, false);
		String initialTimestamp = Long.toString(registry.getProfile("PlatformProfile").getTimestamp());

		//Take 3.5 archive, unzip, run it to update to 3.6
		assertEquals(0, runDirectorToUpdate("Updating from 3.5 to 3.6", currentBuildRepo, "org.eclipse.platform.ide", "org.eclipse.platform.ide"));
		assertEquals(0, installAndRunVerifierBundle(null));

		// revert to 3.5
		assertEquals(0, runDirectorToRevert("Reverting from 3.6 to 3.5", "http://download.eclipse.org/eclipse/updates/3.5", initialTimestamp));
		assertEquals(0, installAndRunVerifierBundle35(null));
	}
}
