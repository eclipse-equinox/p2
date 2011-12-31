/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * Sonatype, Inc. - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.tests.*;

public class LuckyTest4 extends AbstractProvisioningTest {
	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 1 \n" + "depends: platform = 1\n")
	public IInstallableUnit sdk1;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit platform1;

	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 2 \n" + "depends: platform = 2\n")
	public IInstallableUnit sdk2;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit platform2;

	@IUDescription(content = "package: egit \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit egit1;

	@IUDescription(content = "package: egit \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit egit2;

	IProfile profile;

	private IPlanner planner;

	private IEngine engine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IULoader.loadIUs(this);
		profile = createProfile("TestProfile." + getName());
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2});
		planner = createPlanner();
		engine = createEngine();

		//Setup the initial state of the profile
		assertOK(install(profile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		assertOK(install(profile, new IInstallableUnit[] {egit1}, false, planner, engine));
		assertEquals("STRICT", profile.getInstallableUnitProperty(sdk1, "org.eclipse.equinox.p2.internal.inclusion.rules"));
		assertEquals("OPTIONAL", profile.getInstallableUnitProperty(egit1, "org.eclipse.equinox.p2.internal.inclusion.rules"));
	}

	//Verify that both strict and optional dependencies are updated and the properties appropriately conserved
	public void testInstallSDK2() {
		assertNotOK(install(profile, new IInstallableUnit[] {platform2, egit2}, true, planner, engine));

		IProfileChangeRequest res = new LuckyHelper().computeProfileChangeRequest(profile, planner, null, new ProvisioningContext(getAgent()), getMonitor());
		assertEquals(2, res.getAdditions().size());
		assertTrue(res.getAdditions().contains(sdk2));
		assertTrue(res.getAdditions().contains(egit2));
		assertEquals(2, res.getRemovals().size());
		assertTrue(res.getRemovals().contains(sdk1));
		assertTrue(res.getRemovals().contains(egit1));

		assertOK(install(res, planner, engine));
		assertProfileContains("validate new profile", profile, new IInstallableUnit[] {sdk2, platform2, egit2});
		assertEquals("STRICT", profile.getInstallableUnitProperty(sdk2, "org.eclipse.equinox.p2.internal.inclusion.rules"));
		assertEquals(null, profile.getInstallableUnitProperty(platform2, "org.eclipse.equinox.p2.internal.inclusion.rules"));
		assertEquals("OPTIONAL", profile.getInstallableUnitProperty(egit2, "org.eclipse.equinox.p2.internal.inclusion.rules"));
	}

	//Check that 
	public void updateWithInterference() {
		assertNotOK(install(profile, new IInstallableUnit[] {platform2, egit2}, true, planner, engine));

		IProfileChangeRequest res = new LuckyHelper().computeProfileChangeRequest(profile, planner, null, new ProvisioningContext(getAgent()), getMonitor());
		assertEquals(2, res.getAdditions().size());
		assertTrue(res.getAdditions().contains(sdk2));
		assertTrue(res.getAdditions().contains(egit2));
		assertEquals(2, res.getRemovals().size());
		assertTrue(res.getRemovals().contains(sdk1));
		assertTrue(res.getRemovals().contains(egit1));

		assertOK(install(res, planner, engine));
		assertProfileContains("validate new profile", profile, new IInstallableUnit[] {sdk2, platform2, egit2});
		assertEquals("STRICT", profile.getInstallableUnitProperty(sdk2, "org.eclipse.equinox.p2.internal.inclusion.rules"));
		assertEquals(null, profile.getInstallableUnitProperty(platform2, "org.eclipse.equinox.p2.internal.inclusion.rules"));
		assertEquals("OPTIONAL", profile.getInstallableUnitProperty(egit2, "org.eclipse.equinox.p2.internal.inclusion.rules"));

	}
}
