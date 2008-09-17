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
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
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

	private void restart() {
		try {
			ungetServices();
			TestActivator.getBundle("org.eclipse.equinox.p2.exemplarysetup").stop();
			TestActivator.getBundle("org.eclipse.equinox.p2.exemplarysetup").start();
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

	public void testPeristence() {
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
}
