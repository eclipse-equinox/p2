/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Ericsson AB - initial API and implementation
 *     Red Hat, Inc. - fragment support
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import java.io.File;
import java.io.IOException;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

public class BaseChangeExtendedConflicts extends BaseChange {

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(BaseChangeExtendedConflicts.class.getName());
		suite.addTest(new BaseChangeExtendedConflicts("testBundlesSpecifiedMultipleTimes"));
		return suite;
	}

	private File extensions;

	public BaseChangeExtendedConflicts(String name) {
		super(name);
	}

	protected void realExecuteVerifier(Properties verificationProperties, boolean withConfigFlag, File... extensions) {
		File verifierConfig = new File(getTempFolder(), "verification.properties");
		try {
			writeProperties(verifierConfig, verificationProperties);
		} catch (IOException e) {
			fail("Failing to write out properties to configure verifier", e);
		}

		String[] args = null;

		if (withConfigFlag) {
			args = new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.tests.verifier.application", "-verifier.properties", verifierConfig.getAbsolutePath(), "-consoleLog"};
		} else {
			args = new String[] {"-application", "org.eclipse.equinox.p2.tests.verifier.application", "-verifier.properties", verifierConfig.getAbsolutePath(), "-consoleLog"};
		}

		assertEquals(0, runEclipse("Running verifier", output, args, extensions));
	}

	protected void executeVerifier(Properties verificationProperties, File... extensions) {
		realExecuteVerifier(verificationProperties, true, extensions);
	}

	protected int runEclipse(String message, File location, String[] args, File... extensions) {
		File root = new File(Activator.getBundleContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");
		assertTrue("Java executable not found in: " + exe.getAbsolutePath(), exe.exists());
		List<String> command = new ArrayList<String>();
		Collections.addAll(command, new String[] {(new File(location == null ? output : location, getExeFolder() + "eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-vm", exe.getAbsolutePath()});
		Collections.addAll(command, args);
		Collections.addAll(command, new String[] {"-vmArgs", "-Dosgi.checkConfiguration=true"});
		if (extensions != null) {
			String extensionParameter = "";
			for (File f : extensions) {
				extensionParameter += f.toString() + ",";
			}
			extensionParameter = extensionParameter.substring(0, extensionParameter.length() - 1);
			Collections.addAll(command, new String[] {"-Dp2.fragments=" + extensionParameter});
		}

		// command-line if you want to run and allow a remote debugger to connect
		if (debug)
			Collections.addAll(command, new String[] {"-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"});
		int result = run(message, command.toArray(new String[command.size()]));
		// 13 means that we wrote something out in the log file.
		// so try and parse it and fail via that message if we can.
		if (result == 13)
			parseExitdata(message);
		return result;
	}

	protected void installFeature1AndVerifierInUser(File... extension) {
		//TODO Install something into eclipse - make sure that this can be done in an automated setup
		runEclipse("Installing in user", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.director", "-installIU", "p2TestFeature1.feature.group,Verifier.feature.group", "-repository", getTestRepo()}, extension);
	}

	protected void installFeature2InUser(File... extension) {
		runEclipse("user2", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.director", "-installIU", "p2TestFeature2.feature.group", "-repository", getTestRepo()}, extension);
	}

	protected void installVerifierInBase() {
		setReadOnly(readOnlyBase, false);
		runEclipse("Running eclipse", output, new String[] {"-application", "org.eclipse.equinox.p2.director", "-installIU", "Verifier.feature.group", "-repository", getTestRepo()});
		setReadOnly(readOnlyBase, true);
	}

	@Override
	protected void tearDown() throws Exception {
		setReadOnly(extensions, false);
		extensions.delete();
		super.tearDown();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		extensions = getTestFolder("ext");
		copy("", getTestData("", "testData/reconciler/extensions/ext1"), extensions);
		setReadOnly(extensions, true);
	}

	public void testBundlesSpecifiedMultipleTimes() {
		assertInitialized();
		setupReadOnlyInstall();

		{ //install verifier and something else in user and checks there are there
			// when extensions are enabled and configured
			installFeature1AndVerifierInUser(extensions, extensions);
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("expectedBundleList", "p2TestBundle1,org.eclipse.equinox.p2.tests.verifier,zzz");
			verificationProperties.setProperty("checkProfileResetFlag", "false");
			verificationProperties.setProperty("not.sysprop.eclipse.ignoreUserConfiguration", "");
			executeVerifier(verificationProperties, extensions);

			assertTrue(isInUserBundlesInfo("p2TestBundle1"));
			assertTrue(isInUserBundlesInfo("zzz"));
			assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());
		}

		{ //Now drop extensions.

			// this is dummy call as verifier *was* dropped, and it can't fail in that case
			//			executeVerifier(verificationProperties);

			//install verifier again
			installVerifierInBase();

			// next start is with extensions, should find zzz and verifier, and no other bundles 
			// as base was changed and user configuration is ignored
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("checkPresenceOfVerifier", "true");
			verificationProperties.setProperty("unexpectedBundleList", "p2TestBundle1,yyy");
			verificationProperties.setProperty("checkPresenceOfVerifier", "false");
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier,zzz");
			verificationProperties.setProperty("checkProfileResetFlag", "false");
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "true");
			executeVerifier(verificationProperties, extensions, extensions);

			assertTrue(isInUserBundlesInfo("p2TestBundle1")); //Despite the reset, the bundles.info is still on-disk unmodified since no provisioning has been done
			assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());

			verificationProperties = new Properties();
			//Execute the verifier another time, to check that the profile reset flag is not set
			verificationProperties.setProperty("checkProfileResetFlag", "false");
			verificationProperties.setProperty("checkPresenceOfVerifier", "false");
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier,zzz");
			executeVerifier(verificationProperties, extensions, extensions);
		}

		{ //Now add something into the user install again
			installFeature2InUser(extensions, extensions);
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier,zzz,p2TestBundle2");
			executeVerifier(verificationProperties, extensions, extensions);

			assertFalse(isInUserBundlesInfo("p2TestBundle1")); // was dropped some time ago
			assertTrue(isInUserBundlesInfo("p2TestBundle2")); // was installed recently
			assertTrue(isInUserBundlesInfo("org.eclipse.equinox.p2.tests.verifier")); //It is now coming from the base
			assertTrue(isInUserBundlesInfo("zzz")); // from extensions

			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier,zzz,p2TestBundle2");
			executeVerifier(verificationProperties, extensions, extensions);

			//verifier without extensions should drop all except verifier in the base
			Properties newVerificationProperties = new Properties();
			newVerificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier");
			newVerificationProperties.setProperty("unexpectedBundleList", "zzz,p2TestBundle2,p2TestBundle1");
			executeVerifier(newVerificationProperties);

			// zzz again present, verifier present
			// p2Test bundle visible, because all timestamp match properly again!
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier,zzz,p2TestBundle2,");
			verificationProperties.setProperty("unexpectedBundleList", "p2TestBundle1");
			executeVerifier(verificationProperties, extensions, extensions);
		}
	}
}
