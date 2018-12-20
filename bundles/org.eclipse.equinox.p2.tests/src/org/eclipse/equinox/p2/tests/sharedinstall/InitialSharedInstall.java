/*******************************************************************************
 * Copyright (c) 2013, 2018 Ericsson AB and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import java.io.File;
import java.io.FilenameFilter;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

//This test verifies that when eclipse runs for the first time and it has been run previously, then the migration wizard is presented.

//This test is separated in two classes.
//This class is responsible for setting up eclipse once
//The second class (InitialSharedInstallReadTest) is responsible for carrying out the real test
//The test is setup this way in order to reuse all the testing infrastructure without having the modify much of it.
public class InitialSharedInstall extends AbstractSharedInstallTest {

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();

		suite.setName(InitialSharedInstall.class.getName());
		suite.addTest(new InitialSharedInstall("setupRun"));

		TestSuite suite2 = new ReconcilerTestSuite();
		suite2.addTest(new InitialSharedInstallRealTest("testImportFromPreviousInstall"));

		suite.addTest(suite2);
		return suite;
	}

	public InitialSharedInstall(String name) {
		super(name);
	}

	public void setupRun() {
		cleanupDotEclipseFolder();
		assertInitialized();
		replaceDotEclipseProductFile(new File(output, getRootFolder()), "p2.automated.test", "1.0.0");
		setupReadOnlyInstall();
		reallyReadOnly(readOnlyBase);
		System.out.println(readOnlyBase);
		System.out.println(userBase);

		{
			//This causes an installation of Eclipse to be created in the default location (~/.eclipse/...)
			installFeature1InUserWithoutSpecifyingConfiguration();
		}
		removeReallyReadOnly(readOnlyBase);
	}

	private void cleanupDotEclipseFolder() {
		File userHome = new File(System.getProperty("user.home"));
		File dotEclipse = new File(userHome, ".eclipse");
		if (!dotEclipse.exists())
			// nothing to clean up
			return;

		File[] toDelete = dotEclipse.listFiles((FilenameFilter) (dir, name) -> {
			if (name.startsWith("p2.automated.test"))
				return true;
			return false;
		});
		for (File file : toDelete) {
			delete(file);
		}
	}
}
