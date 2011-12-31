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

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.*;

public class LuckyTest9 extends AbstractProvisioningTest {
	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 1 \n" + "depends: platform = 1")
	public IInstallableUnit sdk1;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit platform1;

	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 2 \n" + "depends: platform = 2")
	public IInstallableUnit sdk2;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit platform2;

	@IUDescription(content = "package: egit \n" + "singleton: true\n" + "version: 1 \n" + "depends: platform = 1")
	public IInstallableUnit egit1;

	@IUDescription(content = "package: egit \n" + "singleton: true\n" + "version: 2 \n" + "depends: platform = 2")
	public IInstallableUnit egit2;

	IProfile profile = createProfile("TestProfile." + getName());

	private IPlanner planner;

	private IEngine engine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IULoader.loadIUs(this);
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2});
		planner = createPlanner();
		engine = createEngine();
		assertOK(install(profile, new IInstallableUnit[] {sdk1}, true, planner, engine));
	}

	public void testRemovalIsTakenIntoAccount() {
		IProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.remove(egit1);

		ProfileChangeRequest res = new LuckyHelper().computeProfileChangeRequest(profile, planner, changeRequest, new ProvisioningContext(getAgent()), getMonitor());
		assertEquals(1, res.getAdditions().size());
		assertTrue(res.getAdditions().contains(sdk2));
		assertEquals(1, res.getRemovals().size());
		assertTrue(res.getRemovals().contains(sdk1));

		assertOK(install(res, planner, engine));
		assertProfileContains("validate new profile", profile, new IInstallableUnit[] {sdk2, platform2});
		assertTrue(profile.query(QueryUtil.createIUQuery(egit1), null).isEmpty());
		assertTrue(profile.query(QueryUtil.createIUQuery(egit2), null).isEmpty());
		assertEquals("STRICT", profile.getInstallableUnitProperty(sdk2, "org.eclipse.equinox.p2.internal.inclusion.rules"));
	}
}
