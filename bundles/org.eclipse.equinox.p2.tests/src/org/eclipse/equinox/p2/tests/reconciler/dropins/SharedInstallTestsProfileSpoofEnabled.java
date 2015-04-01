/*******************************************************************************
 *  Copyright (c) 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.updatesite.Activator;

public class SharedInstallTestsProfileSpoofEnabled extends SharedInstallTests {

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(SharedInstallTestsProfileSpoofEnabled.class.getName());
		suite.addTest(new SharedInstallTestsProfileSpoofEnabled("testBasicStartup"));
		suite.addTest(new SharedInstallTestsProfileSpoofEnabled("testReadOnlyDropinsStartup"));
		suite.addTest(new SharedInstallTestsProfileSpoofEnabled("testUserDropinsStartup"));
		return suite;
	}

	/*
	 * Constructor for the class.
	 */
	public SharedInstallTestsProfileSpoofEnabled(String name) {
		super(name);
	}

	public void reconcileReadOnly(String message) {
		File root = new File(Activator.getBundleContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");

		String configuration = new File(userBase, "configuration").getAbsolutePath();
		String[] command = new String[] {(new File(output, getExeFolder() + "eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-debug", "-consolelog", "-nosplash", "-application", "org.eclipse.equinox.p2.reconciler.application", "-configuration", configuration, "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-Dp2.simpleconfigurator.extensions=true"};
		//			String[] command = new String[] {(new File(output, "eclipse/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-debug", "-consolelog", "-nosplash", "-application", "org.eclipse.equinox.p2.reconciler.application", "-configuration", configuration, "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-Dp2.simpleconfigurator.extended=true", "-Xdebug", "-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"};
		run(message, command);
	}

	//	public void testBasicStartup() throws IOException {
	//		assertInitialized();
	//		setupReadOnlyInstall();
	//		try {
	//			File userBundlesInfo = new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
	//			File userConfigIni = new File(userBase, "configuration/config.ini");
	//			assertFalse("0.1", userBundlesInfo.exists());
	//			assertFalse("0.2", userConfigIni.exists());
	//			reconcileReadOnly("0.21");
	//			//this is a change from the profile scenario - bundles.info will be always created as there are p2 operations!
	//			assertTrue("0.3", userBundlesInfo.exists());
	//			assertTrue("0.4", userConfigIni.exists());
	//
	//			Properties props = new Properties();
	//			InputStream is = new BufferedInputStream(new FileInputStream(userConfigIni));
	//			try {
	//				props.load(is);
	//			} finally {
	//				is.close();
	//			}
	//			assertTrue("0.5", props.containsKey("osgi.sharedConfiguration.area"));
	//			// eclipse.p2.data.area doesn't appear in the nonextended tests, but I can't see why it's wrong
	//			assertEquals("0.6", 2, props.size());
	//		} finally {
	//			cleanupReadOnlyInstall();
	//		}
	//	}
}
