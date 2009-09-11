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

//Install 3.6 using 3.5
public class Install36from35 extends AbstractReconcilerTest {

	public int runDirectorToInstall(String message, String sourceRepo, String iuToInstall) {
		File root = new File(Activator.getBundleContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");
		String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-application", "org.eclipse.equinox.p2.director", "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-repository", sourceRepo, "-installIU", iuToInstall};

		//		"-destination" d:/eclipse/"
		//		   "-profile" SDKProfile
		//		   "-profileProperties" org.eclipse.update.install.features=true
		//		   "-bundlepool" d:/eclipse/
		//		   "-p2.os" linux
		//		  "-p2.ws" gtk
		//		   "-p2.arch" x86
		//		   "-roaming"

		// command-line if you want to run and allow a remote debugger to connect
		// String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-application", "org.eclipse.equinox.p2.director", "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-repository", sourceRepo, "-installIU", iuToInstall, "-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"};
		return run(message, command);
	}

	public Install36from35(String string) {
		super(string);
	}

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.addTest(new Install36from35("Install 3.6 from 3.5"));
		return suite;
	}

	public void from35To36() {
		assertEquals(0, runDirectorToInstall("Installing 3.6 from 3.5", "http://download.eclipse.org/eclipse/updates/3.6-I-builds", "org.eclipse.platform.ide"));
	}
}
