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
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.p2.tests.reconciler.dropins.AbstractReconcilerTest;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

public class From35to36 extends AbstractReconcilerTest {

	public int runDirectorToUpdate(String message, String sourceRepo, String iuToInstall, String iuToUninstall) {
		File root = new File(Activator.getBundleContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");
		String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-application", "org.eclipse.equinox.p2.director", "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-repository", sourceRepo, "-installIU", iuToInstall, "-uninstallIU", iuToUninstall};
		// command-line if you want to run and allow a remote debugger to connect
		// String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-application", "org.eclipse.equinox.p2.director", "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-repository", sourceRepo, "-installIU", iuToInstall, "-uninstallIU", iuToUninstall, "-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"};
		return run(message, command);
	}

	public int runDirectorToRevert(String message, String sourceRepo) {
		File root = new File(Activator.getBundleContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");
		String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-application", "org.eclipse.equinox.p2.director", "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-repository", sourceRepo, "-revert"};
		// command-line if you want to run and allow a remote debugger to connect
		// String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-application", "org.eclipse.equinox.p2.director", "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-repository", sourceRepo, "-revert", "-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"};
		return run(message, command);
	}

	public From35to36(String string) {
		super(string);
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite("org.eclipse.equinox.p2.reconciler.tests.35.platform.archive");
		suite.addTest(new From35to36("from35To36"));
		return suite;
	}

	public void from35To36() {
		//TODO Have a variable that points to the repo of teh build being tested
		assertEquals(0, runDirectorToUpdate("Updating 3.5 to 3.6", "http://download.eclipse.org/eclipse/updates/3.6-I-builds", "org.eclipse.platform.ide", "org.eclipse.platform.ide"));
		//What do we check?
		assertEquals(0, runDirectorToRevert("Reverting from 3.6 to 3.5", "http://download.eclipse.org/eclipse/updates/3.5"));
	}
}
