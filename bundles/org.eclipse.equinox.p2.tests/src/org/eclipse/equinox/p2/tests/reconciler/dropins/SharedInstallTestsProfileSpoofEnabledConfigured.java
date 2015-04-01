/*******************************************************************************
 *  Copyright (c) 2003 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.*;
import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.p2.tests.sharedinstall.AbstractSharedInstallTest;

public class SharedInstallTestsProfileSpoofEnabledConfigured extends SharedInstallTestsProfileSpoofEnabled {

	private File extensions;

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(SharedInstallTestsProfileSpoofEnabledConfigured.class.getName());
		suite.addTest(new SharedInstallTestsProfileSpoofEnabledConfigured("testBasicStartup"));
		suite.addTest(new SharedInstallTestsProfileSpoofEnabledConfigured("testReadOnlyDropinsStartup"));
		suite.addTest(new SharedInstallTestsProfileSpoofEnabledConfigured("testUserDropinsStartup"));
		return suite;
	}

	/*
	 * Constructor for the class.
	 */
	public SharedInstallTestsProfileSpoofEnabledConfigured(String name) {
		super(name);
	}

	public void reconcileReadOnly(String message, File extensions) {
		reconcileReadOnly(message, extensions, false);
	}

	public void reconcileReadOnly(String message, File extensions, boolean debug) {
		File root = new File(Activator.getBundleContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");

		String configuration = new File(userBase, "configuration").getAbsolutePath();
		String command[];
		if (!debug) {
			if (extensions != null) {
				command = new String[] {(new File(output, getExeFolder() + "eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-debug", "-consolelog", "-nosplash", "-application", "org.eclipse.equinox.p2.reconciler.application", "-configuration", configuration, "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-Dp2.fragments=" + extensions.toString()};
			} else {
				command = new String[] {(new File(output, getExeFolder() + "eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-debug", "-consolelog", "-nosplash", "-application", "org.eclipse.equinox.p2.reconciler.application", "-configuration", configuration, "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true"};
			}
		} else {
			if (extensions != null) {
				command = new String[] {(new File(output, getExeFolder() + "eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-debug", "-consolelog", "-nosplash", "-application", "org.eclipse.equinox.p2.reconciler.application", "-configuration", configuration, "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-Dp2.fragments=" + extensions.toString(), "-Xdebug", "-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"};
			} else {
				command = new String[] {(new File(output, getExeFolder() + "eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-debug", "-consolelog", "-nosplash", "-application", "org.eclipse.equinox.p2.reconciler.application", "-configuration", configuration, "-vm", exe.getAbsolutePath(), "-vmArgs", "-Dosgi.checkConfiguration=true", "-Xdebug", "-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y"};
			}
		}
		run(message, command);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		extensions = getTempFolder();
		copy("", getTestData("", "testData/reconciler/extensions/ext1"), extensions);
		setReadOnly(extensions, true);
		AbstractSharedInstallTest.reallyReadOnly(extensions);
	}

	@Override
	protected void tearDown() throws Exception {
		AbstractSharedInstallTest.removeReallyReadOnly(extensions);
		setReadOnly(extensions, false);
		extensions.delete();
		super.tearDown();
	}

	public void testBasicStartup() throws IOException {
		assertInitialized();
		setupReadOnlyInstall();
		try {
			File userBundlesInfo = new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
			File userConfigIni = new File(userBase, "configuration/config.ini");
			assertFalse("0.1", userBundlesInfo.exists());
			assertFalse("0.2", userConfigIni.exists());
			reconcileReadOnly("0.21", extensions);
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

			reconcileReadOnly("0.21", extensions);

			assertTrue("0.3", userBundlesInfo.exists());
			assertTrue("0.4", userConfigIni.exists());

			assertTrue(isInBundlesInfo(userBundlesInfo, "myBundle", null));
			assertTrue(isInBundlesInfo(userBundlesInfo, "zzz", null));

			// remove the bundle from the dropins and reconcile
			setReadOnly(readOnlyBase, false);
			AbstractSharedInstallTest.removeReallyReadOnly(readOnlyBase);
			assertTrue("0.7", readOnlyBase.canWrite());
			remove("1.0", "dropins", "myBundle_1.0.0.jar");
			setReadOnly(readOnlyBase, true);
			AbstractSharedInstallTest.reallyReadOnly(readOnlyBase);

			reconcileReadOnly("0.21", extensions, false);

			assertFalse(isInBundlesInfo(userBundlesInfo, "myBundle", null));
			assertTrue(isInBundlesInfo(userBundlesInfo, "zzz", null));

			reconcileReadOnly("0.2105", null, false);

			// those two will never pass. Disabling extensions while no dropins change
			// causes master profile to be loaded and user bundles *not* touched

			//			assertFalse(isInBundlesInfo(userBundlesInfo, "myBundle", null));
			//			assertFalse(isInBundlesInfo(userBundlesInfo, "zzz", null));

			// only dropin change (or any other p2 operations) causes p2 to write a
			// new, up-to-date bundles.info
			setReadOnly(readOnlyBase, false);
			AbstractSharedInstallTest.removeReallyReadOnly(readOnlyBase);
			assertTrue("0.7", readOnlyBase.canWrite());
			add("0.211", "dropins", jar);
			setReadOnly(readOnlyBase, true);
			AbstractSharedInstallTest.reallyReadOnly(readOnlyBase);

			//no extension - new bundles.info should be written
			reconcileReadOnly("0.22", null, false);

			assertTrue(isInBundlesInfo(userBundlesInfo, "myBundle", null));
			assertFalse(isInBundlesInfo(userBundlesInfo, "zzz", null));
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

			reconcileReadOnly("0.21", extensions);

			assertTrue("0.3", userBundlesInfo.exists());
			assertTrue("0.4", userConfigIni.exists());

			assertTrue(isInBundlesInfo(userBundlesInfo, "myBundle", null));
			assertTrue(isInBundlesInfo(userBundlesInfo, "zzz", null));
			// remove the bundle from the dropins and reconcile
			delete(dropins);

			reconcileReadOnly("0.21", extensions);
			assertFalse(isInBundlesInfo(userBundlesInfo, "myBundle", null));
			assertTrue(isInBundlesInfo(userBundlesInfo, "zzz", null));

			reconcileReadOnly("0.2105", null, false);

			// those two will never pass. Disabling extensions while no dropins change
			// causes master profile to be loaded and user bundles *not* touched

			//			assertFalse(isInBundlesInfo(userBundlesInfo, "myBundle", null));
			//			assertFalse(isInBundlesInfo(userBundlesInfo, "zzz", null));

			// only dropin change (or any other p2 operations) causes p2 to write a
			// new, up-to-date bundles.info

			//no extension - new bundles.info should be written
			dropins.mkdir();
			copy("copying to dropins", jar, new File(dropins, jar.getName()));
			reconcileReadOnly("0.22", null, false);

			assertTrue(isInBundlesInfo(userBundlesInfo, "myBundle", null));
			assertFalse(isInBundlesInfo(userBundlesInfo, "zzz", null));
		} finally {
			delete(dropins);
			cleanupReadOnlyInstall();
		}
	}
}
