/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Simple test of the engine API.
 */
public class ProfileRegistryTest extends AbstractProvisioningTest {
	private static final String PROFILE_NAME = "ProfileRegistryTest.profile";
	private IProfileRegistry registry;

	public ProfileRegistryTest() {
		super("");
	}

	public ProfileRegistryTest(String name) {
		super(name);
	}

	protected void getServices() {
		registry = (IProfileRegistry) getAgent().getService(IProfileRegistry.SERVICE_NAME);
	}

	private void ungetServices() {
		registry = null;
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
		Map<String, String> properties = new HashMap<String, String>();
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
		assertTrue(profile.query(QueryUtil.createIUAnyQuery(), null).isEmpty());
		profile.addInstallableUnit(createIU("test"));
		assertEquals(1, queryResultSize(profile.query(QueryUtil.createIUAnyQuery(), null)));
		saveProfile(registry, profile);
		restart();

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertEquals(1, queryResultSize(profile.query(QueryUtil.createIUAnyQuery(), null)));
		profile.removeInstallableUnit(createIU("test"));
		assertTrue(profile.query(QueryUtil.createIUAnyQuery(), null).isEmpty());
		saveProfile(registry, profile);
		restart();

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertTrue(profile.query(QueryUtil.createIUAnyQuery(), null).isEmpty());
		registry.removeProfile(PROFILE_NAME);
		restart();
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testIUPropertyPeristence() throws ProvisionException {
		Map<String, String> properties = new HashMap<String, String>();
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
		File testData = getTestData("0.1", "testData/engineTest/bogusRegistryContent");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);
		createAndValidateProfileRegistry(tempFolder, "SDKProfile");
	}

	public void testTimestampedProfiles() throws ProvisionException {
		assertNull(registry.getProfile(PROFILE_NAME));
		Map<String, String> properties = new HashMap<String, String>();
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

	public void testIsCurrent() throws Exception {
		assertNull(registry.getProfile(PROFILE_NAME));
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("test", "test");
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME, properties);

		assertTrue(registry.isCurrent(profile));
		profile.setProperty("x", "1");
		assertFalse(registry.isCurrent(profile));

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertTrue(registry.isCurrent(profile));

		SimpleProfileRegistry simpleRegistry = (SimpleProfileRegistry) registry;
		Profile profile2 = (Profile) registry.getProfile(PROFILE_NAME);

		simpleRegistry.lockProfile(profile2);
		try {
			profile2.setProperty("x", "1");
			simpleRegistry.updateProfile(profile2);
		} finally {
			simpleRegistry.unlockProfile(profile2);
		}

		assertFalse(registry.isCurrent(profile));

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertTrue(registry.isCurrent(profile));
		IAgentLocation agentLocation = getAgentLocation();
		SimpleProfileRegistry simpleRegistry2 = new SimpleProfileRegistry(getAgent(), SimpleProfileRegistry.getDefaultRegistryDirectory(agentLocation));
		profile2 = (Profile) simpleRegistry2.getProfile(PROFILE_NAME);
		simpleRegistry2.lockProfile(profile2);
		try {
			profile2.setProperty("x", "2");
			simpleRegistry2.updateProfile(profile2);
		} finally {
			simpleRegistry2.unlockProfile(profile2);
		}

		assertFalse(registry.isCurrent(profile));

		profile = (Profile) registry.getProfile(PROFILE_NAME);
		assertTrue(registry.isCurrent(profile));

	}

	public void testProfileLockingNested() {
		File testData = getTestData("0.1", "testData/engineTest/SimpleRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

		final String SIMPLE_PROFILE = "Simple";

		SimpleProfileRegistry simpleRgy = createAndValidateProfileRegistry(tempFolder, SIMPLE_PROFILE);
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
		try {
			simpleRgy.lockProfile(simpleProfile);
			fail("Profile does not permit reentrant locking");
		} catch (IllegalStateException e) {
			// expected
		}
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

	public void testProfileLockingInProcessMultiThreads() {
		File testData = getTestData("0.1", "testData/engineTest/SimpleRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

		final String SIMPLE_PROFILE = "Simple";
		final SimpleProfileRegistry simpleRgy = createAndValidateProfileRegistry(tempFolder, SIMPLE_PROFILE);
		final Profile simpleProfile = (Profile) simpleRgy.getProfile(SIMPLE_PROFILE);
		assertNotNull(simpleProfile);

		// Lock and then lock/unlock from another thread which should fail
		simpleRgy.lockProfile(simpleProfile);
		final Object lock = new Object();
		Thread t1 = new Thread() {
			public void run() {
				try {
					simpleRgy.unlockProfile(simpleProfile);
					fail("This thread is not the owner and unlock should have failed!");
				} catch (IllegalStateException e) {
					// Expected!
				}
				synchronized (lock) {
					lock.notify();
				}
			}
		};
		synchronized (lock) {
			t1.start();
			try {
				lock.wait(5000);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		Thread t2 = new Thread() {
			public void run() {
				try {
					simpleRgy.lockProfile(simpleProfile);
				} catch (IllegalStateException e) {
					fail("The profile should let the thread wait until it can be locked!");
				} finally {
					simpleRgy.unlockProfile(simpleProfile);
				}
				synchronized (lock) {
					lock.notify();
				}
			}
		};
		synchronized (lock) {
			t2.start();
			yieldAndWait();
			simpleRgy.unlockProfile(simpleProfile);
			try {
				lock.wait(500);
			} catch (InterruptedException e) {
				// ignore
			}
		}

		// Remove the newly created profile file
		simpleRgy.removeProfile(PROFILE_NAME); // To avoid it locking the latest file
	}

	//	public void testProfileLockingMultiProcesses() {
	//		File testData = getTestData("0.1", "testData/engineTest/SimpleRegistry");
	//		File tempFolder = getTempFolder();
	//		copy("0.2", testData, tempFolder);
	//
	//		final String SIMPLE_PROFILE = "Simple";
	//
	//		SimpleProfileRegistry simpleRgy = createAndValidateProfileRegistry(tempFolder, SIMPLE_PROFILE);
	//		Profile simpleProfile = (Profile) simpleRgy.getProfile(SIMPLE_PROFILE);
	//		assertNotNull(simpleProfile);
	//
	//		// Make a dummy change to the profile
	//		Properties props = new Properties();
	//		props.put("test", "locking");
	//		simpleProfile.addProperties(props);
	//
	//		// Create a lock file to simulate cross-process locking
	//		MockFileLock mockLock = new MockFileLock(tempFolder, SIMPLE_PROFILE);
	//		mockLock.createExternalProcessUsedForLocking();
	//
	//		// Now save profile - this should fail
	//		try {
	//			saveProfile(simpleRgy, simpleProfile);
	//			fail("This should have failed because profile is already locked!");
	//		} catch (IllegalStateException e) {
	//			// Expected!
	//		}
	//
	//		// Get rid of the lock
	//		mockLock.shutdownExternalProcessUsedForLocking();
	//		mockLock = null;
	//
	//		// Try again, it should succeed
	//		saveProfile(simpleRgy, simpleProfile);
	//
	//		// Remove the newly created profile file
	//		simpleRgy.removeProfile(PROFILE_NAME); // To avoid it locking the latest file
	//	}

	private SimpleProfileRegistry createAndValidateProfileRegistry(File registryFolder, String id) {
		SimpleProfileRegistry simpleRegistry = new SimpleProfileRegistry(getAgent(), registryFolder, null, false);
		IProfile[] profiles = simpleRegistry.getProfiles();
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
		assertEquals(id, profiles[0].getProfileId());
		return simpleRegistry;
	}

	static File getResourceAsBundleRelFile(final String bundleRelPathStr) throws IOException {
		return new File(FileLocator.resolve(TestActivator.getContext().getBundle().getEntry(bundleRelPathStr)).getPath());
	}

	static void yieldAndWait() {
		Thread.yield();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			fail(e1.getMessage());
		}
	}

	//	private static class MockFileLock {
	//		File lockFile;
	//
	//		MockFileLock(File registryFolder, String name) {
	//			final String lOCK_FILENAME = ".lock";
	//			File lockDir = null;
	//			lockDir = new File(registryFolder, name + ".profile");
	//			lockDir.mkdir();
	//			lockFile = new File(lockDir, lOCK_FILENAME);
	//		}
	//
	//		File getLockFile() {
	//			return lockFile;
	//		}
	//
	//		/**
	//		 * This assumes that
	//		 * 	(1) "java" is present in the System path
	//		 * 	(2) "SimpleFileLockerApp.jar" is present in /org.eclipse.equinox.p2.tests/testData/engineTest
	//		 *
	//		 * @see {@link SimpleFileLockerApp}.
	//		 */
	//		void createExternalProcessUsedForLocking() {
	//			File appJar = null;
	//			try {
	//				appJar = getResourceAsBundleRelFile("testData/engineTest/SimpleFileLockerApp.jar");
	//			} catch (IOException e1) {
	//				fail(e1.getMessage());
	//			}
	//			String lockFileDir = lockFile.getParentFile().getAbsolutePath();
	//			String[] cmdArray = {"java", "-cp", appJar.getAbsolutePath(), "org/eclipse/equinox/p2/tests/engine/SimpleFileLockerApp", lockFileDir, "10"};
	//			try {
	//				Runtime.getRuntime().exec(cmdArray);
	//				watForInitialization();
	//			} catch (IOException e) {
	//				fail(e.getMessage());
	//			}
	//		}
	//
	//		void watForInitialization() {
	//			waitFor(/*startup=>*/true);
	//		}
	//
	//		void waitForCompletion() {
	//			waitFor(/*startup=>*/false);
	//		}
	//
	//		private final int MAX_RETRIES = 10; // A guard against looping indefinitely!
	//
	//		private void waitFor(boolean startup) {
	//			int attempts = 0;
	//			boolean shouldWait = true;
	//			do {
	//				sleep(1000);
	//				shouldWait = startup ? !lockFile.exists() : lockFile.exists();
	//			} while (attempts++ < MAX_RETRIES && shouldWait);
	//			String errMsg = "SimpleFileLockerApp hasn't yet " + (startup ? "started up!" : "completed!");
	//			assertTrue(errMsg, attempts < MAX_RETRIES);
	//		}
	//
	//		private void sleep(int millisecs) {
	//			try {
	//				Thread.sleep(millisecs);
	//			} catch (InterruptedException e) {
	//				// Ignore
	//			}
	//		}
	//
	//		void shutdownExternalProcessUsedForLocking() {
	//			File done = new File(lockFile.getParentFile(), ".done");
	//			try {
	//				done.createNewFile();
	//				waitForCompletion();
	//			} catch (IOException e) {
	//				fail(e.getMessage());
	//			}
	//		}
	//	}

	public void testPersistenceFormatNotGzipped() {
		//in a profile with an engine version from 3.5.0 or earlier, we must not gzip the profile registry
		IInstallableUnit engineIU = createEclipseIU("org.eclipse.equinox.p2.engine", Version.create("1.0.100.v20090605"));
		File folder = getTempFolder();
		folder.mkdirs();
		SimpleProfileRegistry profileRegistry = new SimpleProfileRegistry(getAgent(), folder, null, false);
		Profile profile = new Profile(getAgent(), getName(), null, null);
		profile.addInstallableUnit(engineIU);
		Method saveMethod;
		try {
			saveMethod = registry.getClass().getDeclaredMethod("saveProfile", new Class[] {Profile.class});
			saveMethod.setAccessible(true);
			saveMethod.invoke(profileRegistry, new Object[] {profile});
		} catch (SecurityException e) {
			fail();
		} catch (NoSuchMethodException e) {
			fail();
		} catch (IllegalArgumentException e) {
			fail();
		} catch (IllegalAccessException e) {
			fail();
		} catch (InvocationTargetException e) {
			fail();
		}
		File profileFolder = new File(folder, getName() + ".profile");
		File[] filesFound = profileFolder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".profile");
			}
		});
		assertEquals(1, filesFound.length);
		filesFound = profileFolder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".profile.gz");
			}
		});
		assertEquals(0, filesFound.length);
	}

	/**
	 * Asserts that the profile registry persistence honours the system property for controlling
	 * the profile format. See bug 285774.
	 */
	public void testPersistenceFormatOverride() {
		try {
			IInstallableUnit engineIU = createEclipseIU("org.eclipse.equinox.p2.engine", Version.create("55.2"));
			final String[] values = new String[] {"", "blort", null, EngineActivator.PROFILE_FORMAT_UNCOMPRESSED};
			for (int i = 0; i < values.length; i++) {
				final String currentValue = values[i];
				if (currentValue == null)
					System.getProperties().remove(EngineActivator.PROP_PROFILE_FORMAT);
				else
					System.getProperties().put(EngineActivator.PROP_PROFILE_FORMAT, currentValue);
				File folder = getTempFolder();
				folder.mkdirs();
				SimpleProfileRegistry profileRegistry = new SimpleProfileRegistry(getAgent(), folder, null, false);
				Profile profile = new Profile(getAgent(), getName(), null, null);
				profile.addInstallableUnit(engineIU);
				Method saveMethod;
				try {
					saveMethod = registry.getClass().getDeclaredMethod("saveProfile", new Class[] {Profile.class});
					saveMethod.setAccessible(true);
					saveMethod.invoke(profileRegistry, new Object[] {profile});
				} catch (SecurityException e) {
					fail("1.0", e);
				} catch (NoSuchMethodException e) {
					fail("1.1", e);
				} catch (IllegalArgumentException e) {
					fail("1.2", e);
				} catch (IllegalAccessException e) {
					fail("1.3", e);
				} catch (InvocationTargetException e) {
					fail("1.4", e);
				}
				File profileFolder = new File(folder, getName() + ".profile");
				profileFolder.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						if (pathname.getName().endsWith(".profile"))
							assertEquals("2.0." + currentValue, EngineActivator.PROFILE_FORMAT_UNCOMPRESSED, currentValue);
						else if (pathname.getName().endsWith(".profile.gz"))
							assertFalse("2.1." + currentValue, EngineActivator.PROFILE_FORMAT_UNCOMPRESSED.equals(currentValue));
						return false;
					}
				});
			}
		} finally {
			System.getProperties().remove(EngineActivator.PROP_PROFILE_FORMAT);
		}
	}

	public void testPersistenceFormatGzipped() {
		//in a profile with an engine version from 3.5.1 or later, we gzip the profile registry
		IInstallableUnit engineIU = createEclipseIU("org.eclipse.equinox.p2.engine", Version.create("1.0.101"));
		File folder = getTempFolder();
		folder.mkdirs();
		SimpleProfileRegistry profileRegistry = new SimpleProfileRegistry(getAgent(), folder, null, false);
		Profile profile = new Profile(getAgent(), getName(), null, null);
		profile.addInstallableUnit(engineIU);
		Method saveMethod;
		try {
			saveMethod = registry.getClass().getDeclaredMethod("saveProfile", new Class[] {Profile.class});
			saveMethod.setAccessible(true);
			saveMethod.invoke(profileRegistry, new Object[] {profile});
		} catch (SecurityException e) {
			fail();
		} catch (NoSuchMethodException e) {
			fail();
		} catch (IllegalArgumentException e) {
			fail();
		} catch (IllegalAccessException e) {
			fail();
		} catch (InvocationTargetException e) {
			fail();
		}
		File profileFolder = new File(folder, getName() + ".profile");
		File[] filesFound = profileFolder.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".profile.gz");
			}
		});
		assertEquals(1, filesFound.length);
	}

	public void testRemoveProfileTimestamps() throws ProvisionException {
		assertNull(registry.getProfile(PROFILE_NAME));
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("test", "test");
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME, properties);
		assertTrue(profile.getProperties().containsKey("test"));
		long[] timestamps = registry.listProfileTimestamps(PROFILE_NAME);
		assertEquals(1, timestamps.length);

		assertTrue(profile.getProperties().containsKey("test"));
		profile.removeProperty("test");
		assertNull(profile.getProperty("test"));
		saveProfile(registry, profile);
		timestamps = registry.listProfileTimestamps(PROFILE_NAME);
		assertEquals(2, timestamps.length);

		profile.setProperty("test2", "test2");
		saveProfile(registry, profile);
		timestamps = registry.listProfileTimestamps(PROFILE_NAME);

		// We have three timestamps and should be able to remove two
		// of them (but not the current)
		assertEquals(3, timestamps.length);
		int fail = 0;

		for (int i = 0; i < timestamps.length; i++) {
			try {
				registry.removeProfile(PROFILE_NAME, timestamps[i]);
			} catch (ProvisionException e) {
				fail++;
			}
		}
		timestamps = registry.listProfileTimestamps(PROFILE_NAME);
		assertEquals(1, timestamps.length);
		assertEquals(1, fail);
	}

	public void testSetProfileStateProperties() throws ProvisionException {
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		long[] states = registry.listProfileTimestamps(profile.getProfileId());
		long goodTimestamp = states[0];
		long badTimestamp = goodTimestamp + 1;
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("foo", "bar");

		// Test invalid arguments handled as per contract
		try {
			registry.setProfileStateProperties(null, goodTimestamp, properties);
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		try {
			registry.setProfileStateProperties(profile.getProfileId(), goodTimestamp, null);
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		assertNotOK(registry.setProfileStateProperties(profile.getProfileId(), badTimestamp, properties));

		// Test single set.
		assertOK(registry.setProfileStateProperties(profile.getProfileId(), goodTimestamp, properties));

		Map<String, String> result = registry.getProfileStateProperties(profile.getProfileId(), goodTimestamp);
		assertEquals(1, result.size());
		assertEquals("bar", result.get("foo"));

		// Test reset and multiple set
		properties.put("foo", "newBar");
		properties.put("two", "three");

		assertOK(registry.setProfileStateProperties(profile.getProfileId(), goodTimestamp, properties));
		result = registry.getProfileStateProperties(profile.getProfileId(), goodTimestamp);
		assertEquals(2, result.size());
		assertEquals("newBar", result.get("foo"));
		assertEquals("three", result.get("two"));
	}

	public void testSetProfileStateProperty() throws ProvisionException {
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		long[] states = registry.listProfileTimestamps(profile.getProfileId());
		long goodTimestamp = states[0];
		long badTimestamp = goodTimestamp + 1;

		// Test invalid arguments handled as per contract
		try {
			registry.setProfileStateProperty(null, goodTimestamp, "foo", "bar");
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		try {
			registry.setProfileStateProperty(profile.getProfileId(), goodTimestamp, null, "bar");
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		try {
			registry.setProfileStateProperty(profile.getProfileId(), goodTimestamp, "foo", null);
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		assertNotOK(registry.setProfileStateProperty(profile.getProfileId(), badTimestamp, "foo", "bar"));

		// Test single set.
		assertOK(registry.setProfileStateProperty(profile.getProfileId(), goodTimestamp, "foo", "bar"));

		Map<String, String> result = registry.getProfileStateProperties(profile.getProfileId(), goodTimestamp);
		assertEquals(1, result.size());
		assertEquals("bar", result.get("foo"));

		// Test reset and multiple set
		assertOK(registry.setProfileStateProperty(profile.getProfileId(), goodTimestamp, "foo", "newBar"));
		assertOK(registry.setProfileStateProperty(profile.getProfileId(), goodTimestamp, "two", "three"));
		result = registry.getProfileStateProperties(profile.getProfileId(), goodTimestamp);
		assertEquals(2, result.size());
		assertEquals("newBar", result.get("foo"));
		assertEquals("three", result.get("two"));
	}

	public void testPruneProfileStateProperties() throws ProvisionException {
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);

		Map<String, String> profileProperties = new HashMap<String, String>();
		profileProperties.put("profileFoo", "profileBar");

		profile.addProperties(profileProperties);

		saveProfile(registry, profile);

		long[] states = registry.listProfileTimestamps(profile.getProfileId());
		assertEquals(2, registry.listProfileTimestamps(profile.getProfileId()).length);

		Map<String, String> stateProperties1 = new HashMap<String, String>();
		Map<String, String> stateProperties2 = new HashMap<String, String>();

		stateProperties1.put("one", "two");
		stateProperties1.put("a", "b");
		stateProperties1.put("z", "y");

		stateProperties2.put("one", "three");
		stateProperties2.put("a", "c");
		stateProperties2.put("zz", "yy");

		// Check regular states are what we expect.
		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[0], stateProperties1));
		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[1], stateProperties2));

		Map<String, String> result = registry.getProfileStateProperties(profile.getProfileId(), states[0]);
		assertEquals(3, result.size());
		assertEquals("two", result.get("one"));
		assertEquals("b", result.get("a"));
		assertEquals("y", result.get("z"));

		result = registry.getProfileStateProperties(profile.getProfileId(), states[1]);
		assertEquals(3, result.size());
		assertEquals("three", result.get("one"));
		assertEquals("c", result.get("a"));
		assertEquals("yy", result.get("zz"));

		result = registry.getProfileStateProperties(profile.getProfileId(), states[1] + 1);
		assertEquals(0, result.size());

		// Remove a profile
		registry.removeProfile(PROFILE_NAME, states[0]);
		assertEquals(1, registry.listProfileTimestamps(profile.getProfileId()).length);

		// Ensure all properties for the first state were removed when we removed the state
		assertTrue(registry.getProfileStateProperties(profile.getProfileId(), states[0]).isEmpty());

		// Ensure we still have properties for the other state.
		result = registry.getProfileStateProperties(profile.getProfileId(), states[1]);
		assertEquals(3, result.size());
		assertEquals("three", result.get("one"));
		assertEquals("c", result.get("a"));
		assertEquals("yy", result.get("zz"));

		result = registry.getProfileStateProperties(profile.getProfileId(), states[1] + 1);
		assertEquals(0, result.size());

		// Force the states to be pruned by causing a new state to be written.
		Map<String, String> stateProperties3 = new HashMap<String, String>();
		stateProperties3.put("AmIPruned", "yes");
		registry.setProfileStateProperties(profile.getProfileId(), states[1], stateProperties3);

		// Check results.  We expect that state properties for the first state are now gone.
		result = registry.getProfileStateProperties(profile.getProfileId(), states[0]);
		assertEquals(0, result.size());

		result = registry.getProfileStateProperties(profile.getProfileId(), states[1]);
		assertEquals(4, result.size());
		assertEquals("three", result.get("one"));
		assertEquals("c", result.get("a"));
		assertEquals("yy", result.get("zz"));
		assertEquals("yes", result.get("AmIPruned"));

		result = registry.getProfileStateProperties(profile.getProfileId(), states[1] + 1);
		assertEquals(0, result.size());

	}

	public void testGetProfileStateProperties() throws ProvisionException {
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);

		Map<String, String> profileProperties = new HashMap<String, String>();
		profileProperties.put("profileFoo", "profileBar");

		profile.addProperties(profileProperties);

		saveProfile(registry, profile);

		long[] states = registry.listProfileTimestamps(profile.getProfileId());

		Map<String, String> stateProperties1 = new HashMap<String, String>();
		Map<String, String> stateProperties2 = new HashMap<String, String>();

		stateProperties1.put("one", "two");
		stateProperties1.put("a", "b");
		stateProperties1.put("z", "y");

		stateProperties2.put("one", "three");
		stateProperties2.put("a", "c");
		stateProperties2.put("zz", "yy");

		// Test invalid arguments handled as per contract
		try {
			registry.getProfileStateProperties(null, states[0]);
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		// Test getting before any sets. (I.E. file does not exist)
		Map<String, String> result = registry.getProfileStateProperties(profile.getProfileId(), states[0]);
		assertEquals(0, result.size());

		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[0], stateProperties1));
		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[1], stateProperties2));

		result = registry.getProfileStateProperties(profile.getProfileId(), states[0]);
		assertEquals(3, result.size());
		assertEquals("two", result.get("one"));
		assertEquals("b", result.get("a"));
		assertEquals("y", result.get("z"));

		result = registry.getProfileStateProperties(profile.getProfileId(), states[1]);
		assertEquals(3, result.size());
		assertEquals("three", result.get("one"));
		assertEquals("c", result.get("a"));
		assertEquals("yy", result.get("zz"));

		result = registry.getProfileStateProperties(profile.getProfileId(), states[1] + 1);
		assertEquals(0, result.size());
	}

	public void testGetProfileStateProperties2() throws ProvisionException {
		assertNull(registry.getProfile(getName()));
		Profile profile = (Profile) registry.addProfile(getName());

		Map<String, String> profileProperties = new HashMap<String, String>();
		profileProperties.put("profileFoo", "profileBar");

		profile.addProperties(profileProperties);

		saveProfile(registry, profile);

		long[] states = registry.listProfileTimestamps(profile.getProfileId());

		Map<String, String> stateProperties1 = new HashMap<String, String>();
		Map<String, String> stateProperties2 = new HashMap<String, String>();

		stateProperties1.put("one", "two");
		stateProperties1.put("a", "b");
		stateProperties1.put("z", "y");

		stateProperties2.put("one", "three");
		stateProperties2.put("a", "c");
		stateProperties2.put("zz", "yy");

		// Test invalid arguments handled as per contract
		try {
			registry.getProfileStateProperties(null, "foo");
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		try {
			registry.getProfileStateProperties(profile.getProfileId(), null);
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		// Test getting before any sets. (I.E. file does not exist)
		Map<String, String> result = registry.getProfileStateProperties(profile.getProfileId(), "one");
		assertEquals(0, result.size());

		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[0], stateProperties1));
		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[1], stateProperties2));

		result = registry.getProfileStateProperties(profile.getProfileId(), "one");
		assertEquals(2, result.size());
		assertEquals("two", result.get(String.valueOf(states[0])));
		assertEquals("three", result.get(String.valueOf(states[1])));

		result = registry.getProfileStateProperties(profile.getProfileId(), "a");
		assertEquals(2, result.size());
		assertEquals("b", result.get(String.valueOf(states[0])));
		assertEquals("c", result.get(String.valueOf(states[1])));

		result = registry.getProfileStateProperties(profile.getProfileId(), "z");
		assertEquals(1, result.size());
		assertEquals("y", result.get(String.valueOf(states[0])));

		result = registry.getProfileStateProperties(profile.getProfileId(), "zz");
		assertEquals(1, result.size());
		assertEquals("yy", result.get(String.valueOf(states[1])));

		result = registry.getProfileStateProperties(profile.getProfileId(), "none");
		assertEquals(0, result.size());
	}

	public void testRemoveProfileStateProperties() throws ProvisionException {
		assertNull(registry.getProfile(getName()));
		Profile profile = (Profile) registry.addProfile(getName());

		Map<String, String> profileProperties = new HashMap<String, String>();
		profileProperties.put("profileFoo", "profileBar");

		profile.addProperties(profileProperties);

		saveProfile(registry, profile);

		long[] states = registry.listProfileTimestamps(profile.getProfileId());

		Map<String, String> stateProperties1 = new HashMap<String, String>();
		Map<String, String> stateProperties2 = new HashMap<String, String>();

		stateProperties1.put("one", "two");
		stateProperties1.put("a", "b");
		stateProperties1.put("z", "y");

		stateProperties2.put("one", "three");
		stateProperties2.put("a", "c");
		stateProperties2.put("zz", "yy");

		List keys = Arrays.asList(new String[] {"one", "a", "none"});

		// Test removing before any sets. (I.E. file does not exist)
		assertOK(registry.removeProfileStateProperties(profile.getProfileId(), 1, keys));

		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[0], stateProperties1));
		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[1], stateProperties2));

		// Test invalid arguments handled as per contract
		try {
			registry.removeProfileStateProperties(null, states[0], keys);
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		// Remove from one state
		assertOK(registry.removeProfileStateProperties(profile.getProfileId(), states[0], keys));
		Map<String, String> result = registry.getProfileStateProperties(profile.getProfileId(), states[0]);
		assertEquals(1, result.size());
		assertEquals("y", result.get("z"));

		result = registry.getProfileStateProperties(profile.getProfileId(), states[1]);
		assertEquals(3, result.size());
		assertEquals("three", result.get("one"));
		assertEquals("c", result.get("a"));
		assertEquals("yy", result.get("zz"));
	}

	public void testRemoveProfileStateProperties2() throws ProvisionException {
		assertNull(registry.getProfile(getName()));
		Profile profile = (Profile) registry.addProfile(getName());

		Map<String, String> profileProperties = new HashMap<String, String>();
		profileProperties.put("profileFoo", "profileBar");

		profile.addProperties(profileProperties);

		saveProfile(registry, profile);

		long[] states = registry.listProfileTimestamps(profile.getProfileId());

		Map<String, String> stateProperties1 = new HashMap<String, String>();
		Map<String, String> stateProperties2 = new HashMap<String, String>();

		stateProperties1.put("one", "two");
		stateProperties1.put("a", "b");
		stateProperties1.put("z", "y");

		stateProperties2.put("one", "three");
		stateProperties2.put("a", "c");
		stateProperties2.put("zz", "yy");

		// Test removing before any sets. (I.E. file does not exist)
		assertOK(registry.removeProfileStateProperties(profile.getProfileId(), 1, null));

		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[0], stateProperties1));
		assertOK(registry.setProfileStateProperties(profile.getProfileId(), states[1], stateProperties2));

		// ensure everything was set correctly
		Map<String, String> result = registry.getProfileStateProperties(profile.getProfileId(), states[0]);
		assertEquals(3, result.size());
		result = registry.getProfileStateProperties(profile.getProfileId(), states[1]);
		assertEquals(3, result.size());

		// Test invalid arguments handled as per contract
		try {
			registry.removeProfileStateProperties(null, states[0], null);
			fail("Expected a NullPointerException.");
		} catch (NullPointerException e) {
			// expected
		}

		// Remove from one state
		assertOK(registry.removeProfileStateProperties(profile.getProfileId(), states[0], null));
		result = registry.getProfileStateProperties(profile.getProfileId(), states[0]);
		assertEquals(0, result.size());

		result = registry.getProfileStateProperties(profile.getProfileId(), states[1]);
		assertEquals(3, result.size());
		assertEquals("three", result.get("one"));
		assertEquals("c", result.get("a"));
		assertEquals("yy", result.get("zz"));
	}
}
