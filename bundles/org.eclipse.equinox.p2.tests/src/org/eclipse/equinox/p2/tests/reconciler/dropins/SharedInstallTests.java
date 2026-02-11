/*******************************************************************************
 *  Copyright (c) 2008, 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - fragments support added.
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
		if (!exe.exists()) {
			exe = new File(root, "java");
		}

		String configuration = new File(userBase, "configuration").getAbsolutePath();
		String[] command = { (new File(output, getExeFolder() + "/eclipse")).getAbsolutePath(),
				"--launcher.suppressErrors", "-nosplash", "-application",
				"org.eclipse.equinox.p2.reconciler.application", "-configuration", configuration, "-vm",
				exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true" };
		run(message, command);
	}

	public static void setReadOnly(File target, boolean readOnly) {
		if (WINDOWS) {
			String targetPath = target.getAbsolutePath();
			String[] command = { "attrib", readOnly ? "+r" : "-r", targetPath, "/s", "/d" };
			run("setReadOnly " + readOnly + " failed on" + target.getAbsolutePath(), command);
			if (target.isDirectory()) {
				targetPath += "\\*.*";
				command = new String[] {"attrib", readOnly ? "+r" : "-r", targetPath, "/s", "/d"};
				run("setReadOnly " + readOnly + " failed on" + target.getAbsolutePath(), command);
			}
		} else {
			String[] command = { "chmod", "-R", readOnly ? "a-w" : "a+w", target.getAbsolutePath() };
			run("setReadOnly " + readOnly + " failed on" + target.getAbsolutePath(), command);
		}
	}

	protected static void cleanupReadOnlyInstall() {
		delete(userBase);
		setReadOnly(readOnlyBase, false);
		assertTrue(readOnlyBase.canWrite());
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
			assertFalse(userBundlesInfo.exists());
			assertFalse(userConfigIni.exists());
			reconcileReadOnly("0.21");
			assertFalse(userBundlesInfo.exists());
			assertTrue(userConfigIni.exists());

			Properties props = new Properties();
			try (InputStream is = Files.newInputStream(userConfigIni.toPath())) {
				props.load(is);
			}
			assertTrue(props.containsKey("osgi.sharedConfiguration.area"));
			assertEquals(1, props.size());
		} finally {
			cleanupReadOnlyInstall();
		}
	}

	public void testReadOnlyDropinsStartup() throws IOException {
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			return;
		}

		assertInitialized();
		assertDoesNotExistInBundlesInfo("myBundle");
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		add("dropins", jar);
		setupReadOnlyInstall();
		try {
			File userBundlesInfo = new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
			File userConfigIni = new File(userBase, "configuration/config.ini");
			assertFalse(userBundlesInfo.exists());
			assertFalse(userConfigIni.exists());

			reconcileReadOnly("0.21");

			assertTrue(userBundlesInfo.exists());
			assertTrue(userConfigIni.exists());

			assertTrue(isInBundlesInfo(userBundlesInfo, "myBundle", null));

			// remove the bundle from the dropins and reconcile
			setReadOnly(readOnlyBase, false);
			assertTrue(readOnlyBase.canWrite());
			remove("dropins", "myBundle_1.0.0.jar");
			setReadOnly(readOnlyBase, true);

			reconcileReadOnly("0.21");
			assertFalse(isInBundlesInfo(userBundlesInfo, "myBundle", null));
		} finally {
			cleanupReadOnlyInstall();
			// try to remove it in case an exception was thrown
			remove("dropins", "myBundle_1.0.0.jar");
		}
	}

	public void testUserDropinsStartup() throws IOException {
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			return;
		}

		assertInitialized();
		assertDoesNotExistInBundlesInfo("myBundle");
		File jar = getTestData("2.0", "testData/reconciler/plugins/myBundle_1.0.0.jar");
		File dropins = new File(userBase, "dropins");
		setupReadOnlyInstall();
		try {
			dropins.mkdir();
			// copying to dropins
			copy(jar, new File(dropins, jar.getName()));

			File userBundlesInfo = new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
			File userConfigIni = new File(userBase, "configuration/config.ini");
			assertFalse(userBundlesInfo.exists());
			assertFalse(userConfigIni.exists());

			reconcileReadOnly("0.21");

			assertTrue(userBundlesInfo.exists());
			assertTrue(userConfigIni.exists());

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
