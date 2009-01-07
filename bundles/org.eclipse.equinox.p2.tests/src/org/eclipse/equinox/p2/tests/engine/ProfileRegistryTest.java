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
		//		new SimpleProfileRegistry()
		File registryFolder = null;
		try {
			registryFolder = new File(FileLocator.resolve(TestActivator.getContext().getBundle().getEntry("testData/engineTest/bogusRegistryContent/")).getPath());
		} catch (IOException e) {
			fail("Test not properly setup");
		}
		SimpleProfileRegistry bogusRegistry = new SimpleProfileRegistry(registryFolder, null, false);
		IProfile[] profiles = bogusRegistry.getProfiles();
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
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
}
