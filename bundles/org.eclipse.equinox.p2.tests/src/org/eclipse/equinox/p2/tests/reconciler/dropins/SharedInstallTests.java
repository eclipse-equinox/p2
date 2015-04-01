/*******************************************************************************
 *  Copyright (c) 2008, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - fragments support added.
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.*;
import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.updatesite.Activator;

public class SharedInstallTests extends AbstractReconcilerTest {

	protected static final boolean WINDOWS = java.io.File.separatorChar == '\\';
	protected static File readOnlyBase;
	protected static File userBase;

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(SharedInstallTests.class.getName());
		suite.addTest(new SharedInstallTests("testBasicStartup"));
		suite.addTest(new SharedInstallTests("testReadOnlyDropinsStartup"));
		suite.addTest(new SharedInstallTests("testUserDropinsStartup"));
		return suite;
	}

	/*
	 * Constructor for the class.
	 */
	public SharedInstallTests(String name) {
		super(name);
	}

	public void reconcileReadOnly(String message) {
		File root = new File(Activator.getBundleContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");

		String configuration = new File(userBase, "configuration").getAbsolutePath();
		String[] command = new String[] {(new File(output, getExeFolder() + "/eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-application", "org.eclipse.equinox.p2.reconciler.application", "-configuration", configuration, "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true"};
		run(message, command);
	}

	public static void setReadOnly(File target, boolean readOnly) {
		if (WINDOWS) {
			String targetPath = target.getAbsolutePath();
			String[] command = new String[] {"attrib", readOnly ? "+r" : "-r", targetPath, "/s", "/d"};
			run("setReadOnly " + readOnly + " failed on" + target.getAbsolutePath(), command);
			if (target.isDirectory()) {
				targetPath += "\\*.*";
				command = new String[] {"attrib", readOnly ? "+r" : "-r", targetPath, "/s", "/d"};
				run("setReadOnly " + readOnly + " failed on" + target.getAbsolutePath(), command);
			}
		} else {
			String[] command = new String[] {"chmod", "-R", readOnly ? "a-w" : "a+w", target.getAbsolutePath()};
			run("setReadOnly " + readOnly + " failed on" + target.getAbsolutePath(), command);
		}
	}

	protected static void cleanupReadOnlyInstall() {
		delete(userBase);
		setReadOnly(readOnlyBase, false);
		assertTrue("0.7", readOnlyBase.canWrite());
	}

	protected void setupReadOnlyInstall() {
		readOnlyBase = new File(output, getRootFolder());
		assertTrue(readOnlyBase.canWrite());
		setReadOnly(readOnlyBase, true);
		userBase = new File(output, "user");
		userBase.mkdir();
	}

	public void testBasicStartup() throws IOException {
		assertInitialized();
		setupReadOnlyInstall();
		try {
			File userBundlesInfo = new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
			File userConfigIni = new File(userBase, "configuration/config.ini");
			assertFalse("0.1", userBundlesInfo.exists());
			assertFalse("0.2", userConfigIni.exists());
			reconcileReadOnly("0.21");
			assertFalse("0.3", userBundlesInfo.exists());
			assertTrue("0.4", userConfigIni.exists());

			Properties props = new Properties();
			InputStream is = new BufferedInputStream(new FileInputStream(userConfigIni));
			try {
				props.load(is);
			} finally {
				is.close();
			}
			assertTrue("0.5", props.containsKey("osgi.sharedConfiguration.area"));
			assertTrue("0.6", props.size() == 1);
		} finally {
			cleanupReadOnlyInstall();
		}
	}

	public void testReadOnlyDropinsStartup() throws IOException {
		if (Platform.getOS().equals(Platform.OS_MACOSX))
			return;

		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.1", "myBundle");
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("0.2", "dropins", jar);
		setupReadOnlyInstall();
		try {
			File userBundlesInfo = new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
			File userConfigIni = new File(userBase, "configuration/config.ini");
			assertFalse("0.1", userBundlesInfo.exists());
			assertFalse("0.2", userConfigIni.exists());

			reconcileReadOnly("0.21");

			assertTrue("0.3", userBundlesInfo.exists());
			assertTrue("0.4", userConfigIni.exists());

			assertTrue(isInBundlesInfo(userBundlesInfo, "myBundle", null));

			// remove the bundle from the dropins and reconcile
			setReadOnly(readOnlyBase, false);
			assertTrue("0.7", readOnlyBase.canWrite());
			remove("1.0", "dropins", "myBundle_1.0.0.jar");
			setReadOnly(readOnlyBase, true);

			reconcileReadOnly("0.21");
			assertFalse(isInBundlesInfo(userBundlesInfo, "myBundle", null));
		} finally {
			cleanupReadOnlyInstall();
			// try to remove it in case an exception was thrown
			remove("1.0", "dropins", "myBundle_1.0.0.jar");
		}
	}

	public void testUserDropinsStartup() throws IOException {
		if (Platform.getOS().equals(Platform.OS_MACOSX))
			return;

		assertInitialized();
		assertDoesNotExistInBundlesInfo("0.1", "myBundle");
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		File dropins = new File(userBase, "dropins");
		setupReadOnlyInstall();
		try {
			dropins.mkdir();

			copy("copying to dropins", jar, new File(dropins, jar.getName()));

			File userBundlesInfo = new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
			File userConfigIni = new File(userBase, "configuration/config.ini");
			assertFalse("0.1", userBundlesInfo.exists());
			assertFalse("0.2", userConfigIni.exists());

			reconcileReadOnly("0.21");

			assertTrue("0.3", userBundlesInfo.exists());
			assertTrue("0.4", userConfigIni.exists());

			assertTrue(isInBundlesInfo(userBundlesInfo, "myBundle", null));

			// remove the bundle from the dropins and reconcile
			delete(dropins);

			reconcileReadOnly("0.21");
			assertFalse(isInBundlesInfo(userBundlesInfo, "myBundle", null));
		} finally {
			delete(dropins);
			cleanupReadOnlyInstall();
		}
	}
}
