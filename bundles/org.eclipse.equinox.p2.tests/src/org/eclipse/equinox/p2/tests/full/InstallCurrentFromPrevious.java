/*******************************************************************************
 *  Copyright (c) 2011, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import java.io.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.reconciler.dropins.AbstractReconcilerTest;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

/*
 * Use the last release archive to install the current build.
 */
public class InstallCurrentFromPrevious extends AbstractReconcilerTest {
	public InstallCurrentFromPrevious(String string) {
		super(string);
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite("org.eclipse.equinox.p2.reconciler.tests.last.release.platform.archive");
		suite.addTest(new InstallCurrentFromPrevious("installCurrentFromPrevious"));
		return suite;
	}

	public void installCurrentFromPrevious() throws IOException {
		assertInitialized();
		File installFolder = getTestFolder("installCurrentFromPrevious");
		String repository = TestActivator.getContext().getProperty("org.eclipse.equinox.p2.tests.current.build.repo");
		assertNotNull("Need set the \'org.eclipse.equinox.p2.tests.current.build.repo\' property.", repository);
		int result = runDirectorToInstall("Installing current build from last release", new File(installFolder, "eclipse"), repository, "org.eclipse.platform.ide");
		if (result != 0) {
			File logFile = new File(installFolder, "log.log");
			if (logFile.exists()) {
				StringBuilder fileContents = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
					while (reader.ready()) {
						fileContents.append(reader.readLine());
					}
				}
				fail("runDirector returned " + result + "\n" + fileContents.toString());
			} else {
				fail("runDirector returned " + result);
			}
		}
		assertEquals(0, installAndRunVerifierBundle(installFolder));
	}
}