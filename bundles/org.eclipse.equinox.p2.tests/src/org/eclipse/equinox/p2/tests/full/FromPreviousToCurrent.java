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
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.reconciler.dropins.AbstractReconcilerTest;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

/*
 * Take the last release archive and then update to the current build. Then revert back.
 */
public class FromPreviousToCurrent extends AbstractReconcilerTest {

	public FromPreviousToCurrent(String string) {
		super(string);
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite("org.eclipse.equinox.p2.reconciler.tests.last.release.platform.archive");
		suite.addTest(new FromPreviousToCurrent("fromPreviousToCurrent"));
		return suite;
	}

	public void fromPreviousToCurrent() {
		assertInitialized();
		String currentBuildRepo = TestActivator.getContext().getProperty("org.eclipse.equinox.p2.tests.current.build.repo");
		assertNotNull("Need set the \'org.eclipse.equinox.p2.tests.current.build.repo\' property.", currentBuildRepo);
		String lastReleaseBuildRepo = TestActivator.getContext().getProperty("org.eclipse.equinox.p2.tests.last.release.build.repo");
		assertNotNull("Need to set the \'org.eclipse.equinox.p2.tests.last.release.build.repo\' property.", lastReleaseBuildRepo);

		runInitialize("Initializing lastest release to get the profile paths properly setup.");
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), new File(output, "eclipse/p2/org.eclipse.equinox.p2.engine/profileRegistry/"), null, false);
		String initialTimestamp = Long.toString(registry.getProfile("PlatformProfile").getTimestamp());

		//Take the platform archive from the latest release, unzip, run it to update to the current build
		assertEquals(0, runDirectorToUpdate("Updating from last release to current build", currentBuildRepo, "org.eclipse.platform.ide", "org.eclipse.platform.ide"));
		assertEquals(0, installAndRunVerifierBundle(null));

		// revert to the last release
		assertEquals(0, runDirectorToRevert("Reverting from current build to last release", lastReleaseBuildRepo, initialTimestamp));
		assertEquals(0, installAndRunVerifierBundle(null));
	}
}
