/*******************************************************************************
 *  Copyright (c) 2011 IBM Corporation and others.
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

public class From37to38 extends AbstractReconcilerTest {

	public From37to38(String string) {
		super(string);
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite("org.eclipse.equinox.p2.reconciler.tests.37.platform.archive");
		suite.addTest(new From37to38("from37To38"));
		return suite;
	}

	public void from37To38() {
		assertInitialized();
		String currentBuildRepo = System.getProperty("org.eclipse.equinox.p2.tests.current.build.repo");
		// TODO this will change once Juno is released:
		// http://download.eclipse.org/eclipse/updates/3.8
		if (currentBuildRepo == null)
			currentBuildRepo = "http://download.eclipse.org/eclipse/updates/3.8-I-builds";
		runInitialize("Initializing 3.7 to get the profile paths properly setup.");
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), new File(output, "eclipse/p2/org.eclipse.equinox.p2.engine/profileRegistry/"), null, false);
		String initialTimestamp = Long.toString(registry.getProfile("PlatformProfile").getTimestamp());

		//Take 3.7 archive, unzip, run it to update to 3.8
		assertEquals(0, runDirectorToUpdate("Updating from 3.7 to 3.8", currentBuildRepo, "org.eclipse.platform.ide", "org.eclipse.platform.ide"));
		assertEquals(0, installAndRunVerifierBundle(null));

		// revert to 3.7
		assertEquals(0, runDirectorToRevert("Reverting from 3.8 to 3.7", "http://download.eclipse.org/eclipse/updates/3.7", initialTimestamp));
		assertEquals(0, installAndRunVerifierBundle(null));
	}
}
