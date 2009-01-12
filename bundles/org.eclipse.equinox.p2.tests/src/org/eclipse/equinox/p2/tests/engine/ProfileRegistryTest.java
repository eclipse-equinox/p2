/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

/**
 * Simple test of the engine API.
 */
public class ProfileRegistryTest extends AbstractProvisioningTest {
	private static final String PROFILE_NAME = "ProfileRegistryTest.profile";
	private IProfileRegistry registry;
	private ServiceReference registryRef;

	public ProfileRegistryTest() {
		super("");
	}

	public ProfileRegistryTest(String name) {
		super(name);
	}

	protected void getServices() {
		registryRef = TestActivator.getContext().getServiceReference(IProfileRegistry.class.getName());
		registry = (IProfileRegistry) TestActivator.getContext().getService(registryRef);
	}

	private void ungetServices() {
		registry = null;
		TestActivator.getContext().ungetService(registryRef);
	}

	private static void saveProfile(IProfileRegistry iRegistry, Profile profile) {
		SimpleProfileRegistry registry = (SimpleProfileRegistry) iRegistry;
		profile.setChanged(false);
		registry.lockProfile(profile);
		try {
			profile.setChanged(true);
			registry.updateProfile(profile);
		} finally {
			registry.unlockProfile(profile);
			profile.setChanged(false);
		}
	}

	private void restart() {
		try {
			ungetServices();
			TestActivator.getBundle("org.eclipse.equinox.p2.exemplarysetup").stop();
			TestActivator.getBundle("org.eclipse.equinox.p2.exemplarysetup").start();
			//ensure artifact repository manager is registered with event bus. See bug 247584
			IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(TestActivator.getContext(), IProvisioningEventBus.SERVICE_NAME);
			IArtifactRepositoryManager repoMan = (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IArtifactRepositoryManager.class.getName());
			bus.addListener((ProvisioningListener) repoMan);
			getServices();
		} catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}

	protected void setUp() throws Exception {
		getServices();
		//ensure we start in a clean state
		registry.removeProfile(PROFILE_NAME);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		ungetServices();
	}

	public void testAddRemoveProfile() {
		assertNull(registry.getProfile(PROFILE_NAME));
		IProfile test = createProfile(PROFILE_NAME);
		assertEquals(test.getProfileId(), registry.getProfile(PROFILE_NAME).getProfileId());
		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testBasicPeristence() {
		assertNull(registry.getProfile(PROFILE_NAME));
		IProfile test = createProfile(PROFILE_NAME);
		assertEquals(test.getProfileId(), registry.getProfile(PROFILE_NAME).getProfileId());

		restart();
		test = registry.getProfile(PROFILE_NAME);
		assertNotNull(test);
		registry.removeProfile(PROFILE_NAME);

		restart();
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testPropertyPeristence() throws ProvisionException {
		assertNull(registry.getProfile(PROFILE_NAME));
		Properties properties = new Properties();
		properties.put("test", "test");
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME, properties);
		assertTrue(profile.getProperties().containsKey("test"));
		restart();

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertNotNull(profile);
		assertTrue(profile.getProperties().containsKey("test"));
		profile.removeProperty("test");
		assertNull(profile.getProperty("test"));
		saveProfile(registry, profile);
		restart();

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertNull(profile.getProperty("test"));
		registry.removeProfile(PROFILE_NAME);
		restart();

		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testIUPeristence() throws ProvisionException {
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertEquals(0, profile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, profile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		saveProfile(registry, profile);
		restart();

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertEquals(1, profile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		profile.removeInstallableUnit(createIU("test"));
		assertEquals(0, profile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		saveProfile(registry, profile);
		restart();

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertEquals(0, profile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		registry.removeProfile(PROFILE_NAME);
		restart();
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testIUPropertyPeristence() throws ProvisionException {
		Properties properties = new Properties();
		properties.put("test", "test");
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		profile.addInstallableUnit(createIU("test"));
		profile.addInstallableUnitProperties(createIU("test"), properties);
		assertEquals("test", profile.getInstallableUnitProperty(createIU("test"), "test"));
		saveProfile(registry, profile);
		restart();

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertEquals("test", profile.getInstallableUnitProperty(createIU("test"), "test"));
		profile.removeInstallableUnitProperty(createIU("test"), "test");
		assertNull(profile.getInstallableUnitProperty(createIU("test"), "test"));
		saveProfile(registry, profile);
		restart();

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertNull(profile.getInstallableUnitProperty(createIU("test"), "test"));
		registry.removeProfile(PROFILE_NAME);
		restart();
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testBogusRegistry() {
		createAndValidateProfileRegistry("testData/engineTest/bogusRegistryContent/", "SDKProfile");
	}

	public void testTimestampedProfiles() throws ProvisionException {
		assertNull(registry.getProfile(PROFILE_NAME));
		Properties properties = new Properties();
		properties.put("test", "test");
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME, properties);
		long oldtimestamp = profile.getTimestamp();
		assertTrue(profile.getProperties().containsKey("test"));
		long[] timestamps = registry.listProfileTimestamps(PROFILE_NAME);
		assertEquals(1, timestamps.length);

		assertTrue(profile.getProperties().containsKey("test"));
		profile.removeProperty("test");
		assertNull(profile.getProperty("test"));
		saveProfile(registry, profile);
		timestamps = registry.listProfileTimestamps(PROFILE_NAME);
		assertEquals(2, timestamps.length);

		Profile oldProfile = (Profile) registry.getProfile(PROFILE_NAME, oldtimestamp);
		assertTrue(oldProfile.getProperties().containsKey("test"));
		assertFalse(profile.getTimestamp() == oldProfile.getTimestamp());

		registry.removeProfile(PROFILE_NAME);
		assertNull(registry.getProfile(PROFILE_NAME, oldtimestamp));
		timestamps = registry.listProfileTimestamps(PROFILE_NAME);
		assertEquals(0, timestamps.length);
	}

	public void testProfileLockingNested() throws IOException {
		final String SIMPLE_PROFILE = "Simple";
		SimpleProfileRegistry simpleRgy = createAndValidateProfileRegistry("testData/engineTest/SimpleRegistry/", SIMPLE_PROFILE);
		Profile simpleProfile = (Profile) simpleRgy.getProfile(SIMPLE_PROFILE);
		assertNotNull(simpleProfile);

		// Try lock, unlock and then unlock again which should fail
		simpleRgy.lockProfile(simpleProfile);
		simpleRgy.unlockProfile(simpleProfile);
		try {
			simpleRgy.unlockProfile(simpleProfile);
			fail("Should not allow unlock() without calling lock() first");
		} catch (IllegalStateException e) {
			// Expected
		}

		// Try lock, lock, unlock, unlock should pass
		simpleRgy.lockProfile(simpleProfile);
		simpleRgy.lockProfile(simpleProfile);
		simpleRgy.unlockProfile(simpleProfile);
		simpleRgy.unlockProfile(simpleProfile);

		// NOTE: remaining nested tests are commented out for now
		// These tests will work on Win XP and Linux with Java 6. On the Mac and Linux (at least with Java 5) it appears
		// that the class libraries will permit more than one file lock from the same process.
		// It should be noted that the cross-process locking still works correctly and that the profile registry shares
		// the same file lock to prevent these problems.

		/* 		
				// Try nested locks with checks for lock file
				simpleRgy.lockProfile(simpleProfile);
				simpleRgy.lockProfile(simpleProfile);
				simpleRgy.unlockProfile(simpleProfile);
				// Create a lock file to confirm locking

				File lockDirectory = new File(getResourceAsBundleRelFile("testData/engineTest/SimpleRegistry/"), SIMPLE_PROFILE + ".profile");
				File lockFile = new File(lockDirectory, ".lock");
				assertTrue("Lock file does not exist", lockFile.exists());

				ProfileLock profileLock = new ProfileLock(lockDirectory);
				boolean locked = profileLock.lock();
				try {
					assertFalse("Lock file was not locked", locked);
				} finally {
					if (locked)
						profileLock.unlock();
				}
				simpleRgy.unlockProfile(simpleProfile);
				simpleRgy.lockProfile(simpleProfile);
				simpleRgy.unlockProfile(simpleProfile);
				locked = profileLock.lock();
				try {
					assertTrue("Lock file could not be locked", locked);
				} finally {
					if (locked)
						profileLock.unlock();
				}
				assertTrue("Lock file could not removed", lockFile.delete());			
		*/
	}

	public void testProfileLockingMultiProcess() {
		final String SIMPLE_PROFILE = "Simple";
		SimpleProfileRegistry simpleRgy = createAndValidateProfileRegistry("testData/engineTest/SimpleRegistry/", SIMPLE_PROFILE);
		Profile simpleProfile = (Profile) simpleRgy.getProfile(SIMPLE_PROFILE);
		assertNotNull(simpleProfile);

		// Make a dummy change to the profile
		Properties props = new Properties();
		props.put("test", "locking");
		simpleProfile.addProperties(props);

		// Create a lock file to simulate cross-process locking
		MockFileLock mockLock = new MockFileLock("testData/engineTest/SimpleRegistry/", SIMPLE_PROFILE);
		mockLock.createExternalProcessUsedForLocking();

		// Now save profile - this should fail
		try {
			saveProfile(simpleRgy, simpleProfile);
			fail("This should have failed because profile is already locked!");
		} catch (IllegalStateException e) {
			// Expected!
		}

		// Get rid of the lock
		mockLock.shutdownExternalProcessUsedForLocking();
		mockLock = null;

		// Try again, it should succeed
		saveProfile(simpleRgy, simpleProfile);

		// Remove the newly created profile file
		simpleRgy.removeProfile(PROFILE_NAME); // To avoid it locking the latest file
		cleanupProfileArea("testData/engineTest/SimpleRegistry/", SIMPLE_PROFILE);
	}

	private void cleanupProfileArea(String registryRoot, String profileId) {
		File registryFolder = null;
		try {
			registryFolder = getResourceAsBundleRelFile(registryRoot);
		} catch (IOException e) {
			fail("Test not properly setup");
		}
		File profileDir = new File(registryFolder, profileId + ".profile");
		assertTrue(profileDir.exists());
		final String existingProfileFile = "1221176498721.profile";
		File[] profileFiles = profileDir.listFiles();
		for (int i = 0; i < profileFiles.length; i++) {
			if (!existingProfileFile.equals(profileFiles[i].getName())) {
				assertTrue(delete(profileFiles[i]));
			}
		}
	}

	private SimpleProfileRegistry createAndValidateProfileRegistry(String path, String id) {
		File registryFolder = null;
		try {
			registryFolder = getResourceAsBundleRelFile(path);
		} catch (IOException e) {
			fail("Test not properly setup");
		}
		SimpleProfileRegistry simpleRegistry = new SimpleProfileRegistry(registryFolder, null, false);
		IProfile[] profiles = simpleRegistry.getProfiles();
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
		assertEquals(id, profiles[0].getProfileId());
		return simpleRegistry;
	}

	static File getResourceAsBundleRelFile(final String bundleRelPathStr) throws IOException {
		return new File(FileLocator.resolve(TestActivator.getContext().getBundle().getEntry(bundleRelPathStr)).getPath());
	}

	private static class MockFileLock {
		File lockFile;

		MockFileLock(String path, String name) {
			final String lOCK_FILENAME = ".lock";
			File lockDir = null;
			try {
				File profileDir = getResourceAsBundleRelFile(path);
				lockDir = new File(profileDir, name + ".profile");
				lockDir.mkdir();
			} catch (IOException e) {
				fail(e.getMessage());
			}
			lockFile = new File(lockDir, lOCK_FILENAME);
		}

		File getLockFile() {
			return lockFile;
		}

		/**
		 * This assumes that
		 * 	(1) "java" is present in the System path
		 * 	(2) "SimpleFileLockerApp.jar" is present in /org.eclipse.equinox.p2.tests/testData/engineTest
		 * 
		 * @see {@link SimpleFileLockerApp}.
		 */
		void createExternalProcessUsedForLocking() {
			File appJar = null;
			try {
				appJar = getResourceAsBundleRelFile("testData/engineTest/SimpleFileLockerApp.jar");
			} catch (IOException e1) {
				fail(e1.getMessage());
			}
			String lockFileDir = lockFile.getParentFile().getAbsolutePath();
			String[] cmdArray = {"java", "-cp", appJar.getAbsolutePath(), "org/eclipse/equinox/p2/tests/engine/SimpleFileLockerApp", lockFileDir, "10"};
			try {
				Runtime.getRuntime().exec(cmdArray);
				watForInitialization();
			} catch (IOException e) {
				fail(e.getMessage());
			}
		}

		void watForInitialization() {
			waitFor(/*startup=>*/true);
		}

		void waitForCompletion() {
			waitFor(/*startup=>*/false);
		}

		private final int MAX_RETRIES = 10; // A guard against looping indefinitely!

		private void waitFor(boolean startup) {
			int attempts = 0;
			boolean shouldWait = true;
			do {
				sleep(1000);
				shouldWait = startup ? !lockFile.exists() : lockFile.exists();
			} while (attempts++ < MAX_RETRIES && shouldWait);
			String errMsg = "SimpleFileLockerApp hasn't yet " + (startup ? "started up!" : "completed!");
			assertTrue(errMsg, attempts < MAX_RETRIES);
		}

		private void sleep(int millisecs) {
			try {
				Thread.sleep(millisecs);
			} catch (InterruptedException e) {
				// Ignore
			}
		}

		void shutdownExternalProcessUsedForLocking() {
			File done = new File(lockFile.getParentFile(), ".done");
			try {
				done.createNewFile();
				waitForCompletion();
			} catch (IOException e) {
				fail(e.getMessage());
			}
		}
	}
}
