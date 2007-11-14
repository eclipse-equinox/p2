/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
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
		Profile profile = registry.getProfile(PROFILE_NAME);
		if (profile != null)
			registry.removeProfile(profile);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		ungetServices();
	}

	public void testAddRemoveProfile() {
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile test = createProfile(PROFILE_NAME);
		registry.addProfile(test);
		assertEquals(test, registry.getProfile(PROFILE_NAME));
		registry.removeProfile(test);
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	public void testPeristence() {
		assertNull(registry.getProfile(PROFILE_NAME));
		Profile test = createProfile(PROFILE_NAME);
		registry.addProfile(test);
		assertEquals(test, registry.getProfile(PROFILE_NAME));

		restart();
		test = registry.getProfile(PROFILE_NAME);
		assertNotNull(test);
		registry.removeProfile(test);

		restart();
		assertNull(registry.getProfile(PROFILE_NAME));
	}

	private void ungetServices() {
		registry = null;
		TestActivator.getContext().ungetService(registryRef);
	}
}
