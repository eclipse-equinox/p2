/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug259537 extends AbstractProvisioningTest {
	IInstallableUnit a1, a2, a3, a4, b1, b2, b3;
	IInstallableUnit f1, f2, f3;
	IInstallableUnit x1, x2, y1;

	IPlanner planner;
	IProfile profile;
	private IEngine engine;
	private String profileId;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), true);

		b1 = createIU("B", Version.create("2.0.0"), true);

		IRequirement c1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 2.0.0)"), null, false, false);
		IRequirement c2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 3.0.0)"), null, false, false);
		f1 = createIU("F", Version.createOSGi(2, 0, 0), new IRequirement[] {c1, c2});

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, f1});

		profileId = "TestProfile." + getName();
		profile = createProfile(profileId);
		planner = createPlanner();
		engine = createEngine();

	}

	public void test1() {
		//Part 1, we install F1 and the repo only contains the lowest version of the required bundles
		ProfileChangeRequest req = new ProfileChangeRequest(getProfile(profileId));
		req.addInstallableUnits(new IInstallableUnit[] {f1});
		ProvisioningPlan provisioningPlan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertOK("Plan not OK", provisioningPlan.getStatus());
		assertFalse(provisioningPlan.getAdditions().query(QueryUtil.createIUQuery(f1), null).isEmpty());
		assertFalse(provisioningPlan.getAdditions().query(QueryUtil.createIUQuery(a1), null).isEmpty());
		assertFalse(provisioningPlan.getAdditions().query(QueryUtil.createIUQuery(b1), null).isEmpty());

		assertOK("Engine failed", engine.perform(provisioningPlan, null));

		//Part 2, we install F2 and the repo only contains newer version of the required bundles
		a2 = createIU("A", Version.create("1.1.0"), true);
		b2 = createIU("B", Version.create("2.1.0"), true);
		IRequirement c1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 2.0.0)"), null, false, false);
		IRequirement c2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 3.0.0)"), null, false, false);
		f2 = createIU("F", Version.createOSGi(2, 1, 0), new IRequirement[] {c1, c2});
		createTestMetdataRepository(new IInstallableUnit[] {a2, b2, f2});
		ProfileChangeRequest req2 = new ProfileChangeRequest(getProfile("TestProfile." + getName()));
		req2.addInstallableUnits(f2);
		req2.remove(f1);
		ProvisioningPlan provisioningPlan2 = (ProvisioningPlan) planner.getProvisioningPlan(req2, null, null);
		assertFalse(provisioningPlan2.getAdditions().query(QueryUtil.createIUQuery(f2), null).isEmpty());
		assertFalse(provisioningPlan2.getAdditions().query(QueryUtil.createIUQuery(a2), null).isEmpty());
		assertFalse(provisioningPlan2.getAdditions().query(QueryUtil.createIUQuery(b2), null).isEmpty());
		assertOK("Plan not OK", provisioningPlan2.getStatus());
		assertOK("Engine failed", engine.perform(provisioningPlan2, null));

		//Part 3, we add more content to the repo
		a3 = createIU("A", Version.create("1.2.0"), true);
		b3 = createIU("B", Version.create("2.2.0"), true);
		y1 = createIU("Y", Version.create("1.1.1"), true);

		IRequirement d4 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 2.0.0)"), null, false, false);
		x1 = createIU("X", Version.create("3.3.3"), new IRequirement[] {d4});
		IRequirement d3 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 2.0.0)"), null, false, false);
		x2 = createIU("X", Version.createOSGi(3, 4, 4), new IRequirement[] {d3});
		IRequirement d1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 2.0.0)"), null, false, false);
		IRequirement d2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 3.0.0)"), null, false, false);
		f3 = createIU("F", Version.createOSGi(2, 2, 0), new IRequirement[] {d1, d2});
		createTestMetdataRepository(new IInstallableUnit[] {a3, b3, f3, x1, x2});

		IProfileChangeRequest req4 = new ProfileChangeRequest(getProfile("TestProfile." + getName()));
		req4.add(y1);
		ProvisioningPlan provisioningPlan4 = (ProvisioningPlan) planner.getProvisioningPlan(req4, null, null);
		assertOK("Plan not OK", provisioningPlan4.getStatus());
		assertEquals(1, provisioningPlan4.getAdditions().query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size());
		assertOK("Engine failed", engine.perform(provisioningPlan4, null));

		IProfileChangeRequest req5 = new ProfileChangeRequest(getProfile("TestProfile." + getName()));
		req5.add(x1);
		ProvisioningPlan provisioningPlan5 = (ProvisioningPlan) planner.getProvisioningPlan(req5, null, null);
		assertOK("Plan not OK", provisioningPlan5.getStatus());
		assertEquals(2, provisioningPlan5.getAdditions().query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size());
		assertFalse(provisioningPlan5.getAdditions().query(QueryUtil.createIUQuery(a3), null).isEmpty());
		assertOK("Engine failed", engine.perform(provisioningPlan5, null));

		a4 = createIU("A", Version.create("1.3.0"), true);
		createTestMetdataRepository(new IInstallableUnit[] {a4});

		//Part 4, we remove F2 and the repo only contains newer version of the required bundles
		ProfileChangeRequest req3 = new ProfileChangeRequest(getProfile("TestProfile." + getName()));
		req3.remove(f2);
		ProvisioningPlan provisioningPlan3 = (ProvisioningPlan) planner.getProvisioningPlan(req3, null, null);
		assertOK("Plan not OK", provisioningPlan3.getStatus());
		assertTrue(provisioningPlan3.getAdditions().query(QueryUtil.ALL_UNITS, null).isEmpty());
		assertFalse(provisioningPlan3.getRemovals().query(QueryUtil.createIUQuery(f2), null).isEmpty());
		assertFalse(provisioningPlan3.getRemovals().query(QueryUtil.createIUQuery(b2), null).isEmpty());
		assertOK("Engine failed", engine.perform(provisioningPlan3, null));
	}
}
