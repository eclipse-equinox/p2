/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug259537 extends AbstractProvisioningTest {
	IInstallableUnit a1, a2, b1, b2;
	IInstallableUnit f1, f2;

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
		ProfileChangeRequest req = new ProfileChangeRequest(getProfile(profileId));
		req.addInstallableUnits(new IInstallableUnit[] {f1});
		ProvisioningPlan provisioningPlan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertOK("Plan not OK", provisioningPlan.getStatus());
		assertFalse(provisioningPlan.getAdditions().query(QueryUtil.createIUQuery(f1), null).isEmpty());
		assertFalse(provisioningPlan.getAdditions().query(QueryUtil.createIUQuery(a1), null).isEmpty());
		assertFalse(provisioningPlan.getAdditions().query(QueryUtil.createIUQuery(b1), null).isEmpty());

		assertOK("Engine failed", engine.perform(provisioningPlan, null));

		a2 = createIU("A", Version.create("1.1.0"), true);
		b2 = createIU("B", Version.create("2.2.0"), true);
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
	}
}
