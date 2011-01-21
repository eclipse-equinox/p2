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

import java.io.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.reconciler.dropins.AbstractReconcilerTest;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

//Install 3.7 using 3.6
public class Install37from36 extends AbstractReconcilerTest {
	public Install37from36(String string) {
		super(string);
	}

	public int runDirectorToInstall(String message, File installFolder, String sourceRepo, String iuToInstall) {
		File root = new File(TestActivator.getContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");
		String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", //
				"-consoleLog", "-application", "org.eclipse.equinox.p2.director", "-vm", exe.getAbsolutePath(), //
				"-repository", sourceRepo, "-installIU", iuToInstall, //
				"-destination", installFolder.getAbsolutePath(), //
				"-bundlepool", installFolder.getAbsolutePath(), //
				"-roaming", "-profile", "PlatformProfile", "-profileProperties", "org.eclipse.update.install.features=true", // 
				"-p2.os", Platform.getOS(), "-p2.ws", Platform.getWS(), "-p2.arch", Platform.getOSArch(), //
				"-vmArgs", "-Dosgi.checkConfiguration=true", "-Xms40m", "-Xmx256m", //
		//, "-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" //for debugging	
		};

		return run(message, command, new File(installFolder.getParentFile(), "log.log"));
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite("org.eclipse.equinox.p2.reconciler.tests.lastrelease.platform.archive");
		suite.addTest(new Install37from36("install37From36"));
		return suite;
	}

	public void install37From36() throws IOException {
		assertInitialized();
		//Create a new installation of 3.7 using 3.6
		File installFolder = getTestFolder("install37From36");
		System.out.println(installFolder);
		int result = runDirectorToInstall("Installing 3.7 from 3.6", new File(installFolder, "eclipse"), "http://download.eclipse.org/eclipse/updates/3.7-I-builds", "org.eclipse.platform.ide");
		if (result != 0) {
			File logFile = new File(installFolder, "log.log");
			if (logFile.exists()) {
				StringBuffer fileContents = new StringBuffer();
				BufferedReader reader = new BufferedReader(new FileReader(logFile));
				while (reader.ready())
					fileContents.append(reader.readLine());
				reader.close();
				fail("runDirector returned " + result + "\n" + fileContents.toString());
			} else {
				fail("runDirector returned " + result);
			}
		}
		assertEquals(0, installAndRunVerifierBundle(installFolder));
	}
}