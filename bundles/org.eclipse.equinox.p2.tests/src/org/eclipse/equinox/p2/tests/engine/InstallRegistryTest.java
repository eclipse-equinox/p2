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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.installregistry.IInstallRegistry;
import org.eclipse.equinox.internal.p2.installregistry.IProfileInstallRegistry;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Simple test of the engine API.
 */
public class InstallRegistryTest extends AbstractProvisioningTest {
	private ServiceReference registryRef;
	private IInstallRegistry registry;
	private ServiceReference engineRef;
	private Engine engine;

	public InstallRegistryTest(String name) {
		super(name);
	}

	public InstallRegistryTest() {
		super("");
	}

	protected void setUp() throws Exception {
		registryRef = TestActivator.getContext().getServiceReference(IInstallRegistry.class.getName());
		registry = (IInstallRegistry) TestActivator.getContext().getService(registryRef);
		engineRef = TestActivator.getContext().getServiceReference(Engine.class.getName());
		engine = (Engine) TestActivator.getContext().getService(engineRef);
	}

	protected void tearDown() throws Exception {
		engine = null;
		TestActivator.getContext().ungetService(engineRef);
		registry = null;
		TestActivator.getContext().ungetService(registryRef);
	}

	public void testAddRemoveIU() {
		PhaseSet phaseSet = new DefaultPhaseSet();
		Profile profile = createProfile("testProfile");
		String profileId = profile.getProfileId();
		assertEquals(0, getRegistrySize(profileId));
		engine.perform(profile, phaseSet, new Operand[] {new Operand(null, createTestIU())}, null, new NullProgressMonitor());
		assertEquals(1, getRegistrySize(profileId));
		engine.perform(profile, phaseSet, new Operand[] {new Operand(createTestIU(), null)}, null, new NullProgressMonitor());
		assertEquals(0, getRegistrySize(profileId));
		registry.getProfileInstallRegistries().remove(profile);
	}

	protected int getRegistrySize(String profileId) {
		IProfileInstallRegistry profileInstallRegistry = registry.getProfileInstallRegistry(profileId);
		return profileInstallRegistry == null ? 0 : profileInstallRegistry.getInstallableUnits().length;
	}

	public void testPeristence() {
		PhaseSet phaseSet = new DefaultPhaseSet();
		Profile profile = createProfile("testProfile");
		String profileId = profile.getProfileId();
		assertEquals(0, getRegistrySize(profileId));
		engine.perform(profile, phaseSet, new Operand[] {new Operand(null, createTestIU())}, null, new NullProgressMonitor());
		assertEquals(1, getRegistrySize(profileId));

		restart();

		assertEquals(1, getRegistrySize(profileId));
		engine.perform(profile, phaseSet, new Operand[] {new Operand(createTestIU(), null)}, null, new NullProgressMonitor());
		assertEquals(0, getRegistrySize(profileId));
		restart();
		assertEquals(0, getRegistrySize(profileId));
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

	private IInstallableUnit createTestIU() {
		InstallableUnitDescription description = new MetadataFactory.InstallableUnitDescription();
		description.setId("org.eclipse.test");
		description.setVersion(new Version("1.0.0"));
		description.setTouchpointType(MetadataFactory.createTouchpointType("null", new Version("1.0.0")));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(description);
		return createResolvedIU(unit);
	}
}
