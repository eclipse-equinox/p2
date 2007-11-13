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
	private ServiceReference registryRef;
	private IProfileRegistry registry;

	public ProfileRegistryTest(String name) {
		super(name);
	}

	public ProfileRegistryTest() {
		super("");
	}

	protected void setUp() throws Exception {
		registryRef = TestActivator.getContext().getServiceReference(IProfileRegistry.class.getName());
		registry = (IProfileRegistry) TestActivator.getContext().getService(registryRef);
	}

	protected void tearDown() throws Exception {
		registry = null;
		TestActivator.getContext().ungetService(registryRef);
	}

	public void testAddRemoveProfile() {
		assertNull(registry.getProfile("test"));
		Profile test = createProfile("test");
		registry.addProfile(test);
		assertEquals(test, registry.getProfile("test"));
		registry.removeProfile(test);
		assertNull(registry.getProfile("test"));
	}

	public void testPeristence() {
		assertNull(registry.getProfile("test"));
		Profile test = createProfile("test");
		registry.addProfile(test);
		assertEquals(test, registry.getProfile("test"));

		restart();

		registry.removeProfile(test);
		assertNull(registry.getProfile("test"));

		restart();
		assertNull(registry.getProfile("test"));
	}

	private void restart() {
		try {
			tearDown();
			TestActivator.getBundle("org.eclipse.equinox.p2.exemplarysetup").stop();
			TestActivator.getBundle("org.eclipse.equinox.p2.exemplarysetup").start();
			setUp();
		} catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
}
