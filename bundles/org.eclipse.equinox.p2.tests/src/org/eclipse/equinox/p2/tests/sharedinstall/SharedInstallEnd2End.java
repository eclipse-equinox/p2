/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Ericsson AB - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

import java.io.File;
import java.io.IOException;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.IProfile;

public class SharedInstallEnd2End extends AbstractSharedInstallTest {
	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(SharedInstallEnd2End.class.getName());
		suite.addTest(new SharedInstallEnd2End("testInitialRun"));
		suite.addTest(new SharedInstallEnd2End("testInstallInUserSpace"));
		suite.addTest(new SharedInstallEnd2End("testBaseChange"));
		return suite;
	}

	public SharedInstallEnd2End(String name) {
		super(name);
	}

	private String getTestRepo() {
		return getTestData("repo for shared install tests", "testData/sharedInstall/repo").toURI().toString();
	}

	private File getUserBundleInfoTimestamp() {
		return new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/.baseBundlesInfoTimestamp");
	}

	protected File getUserProfileRegistryFolder() {
		return new File(userBase, "p2/org.eclipse.equinox.p2.engine/profileRegistry/");
	}

	private File getUserProfileFolder() {
		return new File(getUserProfileRegistryFolder(), profileId + ".profile");
	}

	private File getBaseProfileRegistryFolder() {
		return new File(output, "eclipse/p2/org.eclipse.equinox.p2.engine/profileRegistry/");
	}

	private long[] getProfileTimestampsFromUser() {
		return new SimpleProfileRegistry(getAgent(), getUserProfileRegistryFolder()).listProfileTimestamps(profileId);
	}

	private long getMostRecentProfileTimestamp(File profileFolder) {
		long[] ts = new SimpleProfileRegistry(getAgent(), profileFolder).listProfileTimestamps(profileId);
		return ts[ts.length - 1];
	}

	private long getMostRecentProfileTimestampFromBase() {
		return getMostRecentProfileTimestamp(getBaseProfileRegistryFolder());
	}

	private void assertProfileStatePropertiesHasValue(File profileFolder, String value) {
		try {
			Properties p = loadProperties(new File(profileFolder, "state.properties"));
			Collection<Object> values = p.values();
			for (Object v : values) {
				if (((String) v).contains(value)) {
					return;
				}
			}
			fail("Value: " + value + " not found.");
		} catch (IOException e) {
			fail("exception while loading profile state properties in " + profileFolder.getAbsolutePath());
		}

	}

	private File getConfigIniTimestamp() {
		return new File(userBase, "configuration/.baseConfigIniTimestamp");
	}

	private void assertProfileStatePropertiesHasKey(File profileFolder, String key) {
		try {
			Properties p = loadProperties(new File(profileFolder, "state.properties"));
			Set<Object> keys = p.keySet();
			for (Object k : keys) {
				if (((String) k).contains(key)) {
					return;
				}
			}
			fail("Key: " + key + " not found.");
		} catch (IOException e) {
			fail("exception while loading profile state properties in " + profileFolder.getAbsolutePath());
		}

	}

	private void installInUser() {
		//TODO Install something into eclipse - make sure that this can be done in an automated setup
		runEclipse("Installing in user", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.director", "-installIU", "p2TestFeature1.feature.group,Verifier.feature.group", "-repository", getTestRepo()});
	}

	private void installInUser2() {
		runEclipse("user2", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.director", "-installIU", "p2TestFeature2.feature.group", "-repository", getTestRepo()});
	}

	private void installVerifierInBase() {
		setReadOnly(readOnlyBase, false);
		runEclipse("Running eclipse", output, new String[] {"-application", "org.eclipse.equinox.p2.director", "-installIU", "Verifier.feature.group", "-repository", getTestRepo()});
		setReadOnly(readOnlyBase, true);
	}

	private boolean isInUserBundlesInfo(String bundleId) {
		try {
			return isInBundlesInfo(getUserBundlesInfo(), bundleId, null, null);
		} catch (IOException e) {
			fail("Problem reading bundles.info");
		}
		//should never be reached
		return false;
	}

	private File getUserBundlesInfo() {
		return new File(userBase, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
	}

	private void startEclipseAsUser() {
		runEclipse("Running eclipse", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.garbagecollector.application", "-profile", "_SELF_"});
	}

	public void testInitialRun() {
		assertInitialized();
		setupReadOnlyInstall();
		//Here we are invoking the GC to force the profile to be loaded.
		startEclipseAsUser();
		assertFalse(getUserBundleInfo().exists());
		assertFalse(getUserBundleInfoTimestamp().exists());
		assertProfileStatePropertiesHasKey(getUserProfileFolder(), IProfile.STATE_PROP_SHARED_INSTALL);
		assertProfileStatePropertiesHasValue(getUserProfileFolder(), IProfile.STATE_SHARED_INSTALL_VALUE_INITIAL);
		assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());
	}

	public void testInstallInUserSpace() {
		assertInitialized();
		setupReadOnlyInstall();

		installInUser();
		assertTrue(isInUserBundlesInfo("p2TestBundle1"));
		assertTrue(isInUserBundlesInfo("org.eclipse.swt")); //this verifies that we have the bundles from the base installed in the user bundles.info 

		assertTrue(getUserBundleInfoTimestamp().exists());
		assertTrue(getConfigIniTimestamp().exists());
		assertProfileStatePropertiesHasKey(getUserProfileFolder(), IProfile.STATE_PROP_SHARED_INSTALL);
		assertProfileStatePropertiesHasValue(getUserProfileFolder(), IProfile.STATE_SHARED_INSTALL_VALUE_INITIAL);
		assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());
		assertEquals(3, getProfileTimestampsFromUser().length);
	}

	public void testBaseChange() {
		assertInitialized();
		setupReadOnlyInstall();
		System.out.println(readOnlyBase);
		System.out.println(userBase);

		{ //install verifier and something else in user and checks there are there
			installInUser();
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("expectedBundleList", "p2TestBundle1,org.eclipse.equinox.p2.tests.verifier");
			verificationProperties.setProperty("checkProfileResetFlag", "false");
			executeVerifier(verificationProperties);

			assertTrue(isInUserBundlesInfo("p2TestBundle1"));
			assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());
		}

		{ //Now change the base. Install the verifier and something else in the base, and run the verifier as a user
			installVerifierInBase();

			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("unexpectedBundleList", "p2TestBundle1");
			verificationProperties.setProperty("checkPresenceOfVerifier", "false");
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier");
			verificationProperties.setProperty("checkProfileResetFlag", "true");
			executeVerifier(verificationProperties);
			assertTrue(isInUserBundlesInfo("p2TestBundle1")); //Despite the reset, the bundles.info is still on-disk unmodified since no provisioning has been done
			assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());

			verificationProperties = new Properties();
			//Execute the verifier another time, to check that the profile reset flag is not set
			verificationProperties.setProperty("checkProfileResetFlag", "false");
			verificationProperties.setProperty("checkPresenceOfVerifier", "false");
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier");
			executeVerifier(verificationProperties);
		}

		{ //Now add something into the user install again
			installInUser2();
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier");
			executeVerifier(verificationProperties);
			assertTrue(isInUserBundlesInfo("p2TestBundle2"));
			assertTrue(isInUserBundlesInfo("org.eclipse.equinox.p2.tests.verifier")); //It is now coming from the base
			assertFalse(isInUserBundlesInfo("p2TestBundle1"));

		}
	}

	private void executeVerifier(Properties verificationProperties) {
		File verifierConfig = new File(getTempFolder(), "verification.properties");
		try {
			writeProperties(verifierConfig, verificationProperties);
		} catch (IOException e) {
			fail("Failing to write out properties to configure verifier", e);
		}
		assertEquals(0, runEclipse("Running verifier", output, new String[] {"-configuration", userBase.getAbsolutePath() + java.io.File.separatorChar + "configuration", "-application", "org.eclipse.equinox.p2.tests.verifier.application", "-verifier.properties", verifierConfig.getAbsolutePath(), "-consoleLog"}));
	}
}
