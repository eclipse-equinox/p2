/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class InclusionRuleTest extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a2;
	IInstallableUnit b1;
	IProfile profile1;
	IProfile profile2;
	IProfile profile3;
	IProfile profile4;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), true);

		a2 = createIU("A", Version.create("2.0.0"), true);

		b1 = createIU("B", Version.create("1.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, a2});

		planner = createPlanner();
		engine = createEngine();
	}

	public void testMultipleInstallations() {
		profile1 = createProfile("TestProfile." + getName());
		ProfileChangeRequest req = new ProfileChangeRequest(profile1);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(plan, null);
		assertProfileContainsAll("A1 is missing", profile1, new IInstallableUnit[] {a1});
		assertEquals(queryResultSize(profile1.query(QueryUtil.createIUAnyQuery(), null)), 1);

		//Make a1 optional.
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createOptionalInclusionRule(a1));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		engine.perform(plan2, null);
		assertProfileContainsAll("A1 is missing", profile1, new IInstallableUnit[] {a1});
		assertEquals(queryResultSize(profile1.query(QueryUtil.createIUAnyQuery(), null)), 1);

		//Install b1 (this should not change anything for a1)
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {b1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.OK, plan3.getStatus().getSeverity());
		engine.perform(plan3, null);
		assertProfileContainsAll("A1 or B1 is missing", profile1, new IInstallableUnit[] {a1, b1});
		assertEquals(queryResultSize(profile1.query(QueryUtil.createIUAnyQuery(), null)), 2);

		//Add a2, this removes a1.
		ProfileChangeRequest req4 = new ProfileChangeRequest(profile1);
		req4.addInstallableUnits(new IInstallableUnit[] {a2});
		IProvisioningPlan plan4 = planner.getProvisioningPlan(req4, null, null);
		assertEquals(IStatus.OK, plan4.getStatus().getSeverity());
		engine.perform(plan4, null);
		assertProfileContainsAll("A2 is missing", profile1, new IInstallableUnit[] {a2});
		assertNotIUs(new IInstallableUnit[] {a1}, profile1.query(QueryUtil.createIUAnyQuery(), null).iterator());
		assertEquals(queryResultSize(profile1.query(QueryUtil.createIUAnyQuery(), null)), 2);

		//Try to add a1 again. This will fail because since a1 has been uninstalled in the previous step and we no longer know about its optional inclusion
		ProfileChangeRequest req5 = new ProfileChangeRequest(profile1);
		req5.addInstallableUnits(new IInstallableUnit[] {a1});
		IProvisioningPlan plan5 = planner.getProvisioningPlan(req5, null, null);
		assertEquals(IStatus.ERROR, plan5.getStatus().getSeverity());
	}

	public void testRemoveInclusionRule() {
		profile2 = createProfile("TestProfile2." + getName());
		//Install a1
		ProfileChangeRequest req = new ProfileChangeRequest(profile2);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(plan, null);
		assertProfileContainsAll("A1 is missing", profile2, new IInstallableUnit[] {a1});
		assertEquals(queryResultSize(profile2.query(QueryUtil.createIUAnyQuery(), null)), 1);

		//Make a1 optional.
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile2);
		req2.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createOptionalInclusionRule(a1));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		engine.perform(plan2, null);
		assertProfileContainsAll("A1 is missing", profile2, new IInstallableUnit[] {a1});
		assertEquals(queryResultSize(profile2.query(QueryUtil.createIUAnyQuery(), null)), 1);

		//Install b1 (this should not change anything for a1)
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile2);
		req3.addInstallableUnits(new IInstallableUnit[] {b1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.OK, plan3.getStatus().getSeverity());
		engine.perform(plan3, null);
		profile2 = getProfile(profile2.getProfileId());
		assertProfileContainsAll("A1 or B1 is missing", profile2, new IInstallableUnit[] {a1, b1});
		assertEquals(queryResultSize(profile2.query(QueryUtil.createIUAnyQuery(), null)), 2);

		//Remove the optional inclusion rule from a1. a1 and b1 are still here 
		ProfileChangeRequest req5 = new ProfileChangeRequest(profile2);
		req5.removeInstallableUnitInclusionRules(a1);
		IProvisioningPlan plan5 = planner.getProvisioningPlan(req5, null, null);
		assertEquals(IStatus.OK, plan5.getStatus().getSeverity());
		engine.perform(plan5, null);
		profile2 = getProfile(profile2.getProfileId());
		assertProfileContainsAll("A1 or B1 is missing", profile2, new IInstallableUnit[] {a1, b1});
		assertEquals(queryResultSize(profile2.query(QueryUtil.createIUAnyQuery(), null)), 2);
	}

	public void testRemoveIUandInclusionRule() {
		profile3 = createProfile("TestProfile3." + getName());
		ProfileChangeRequest req = new ProfileChangeRequest(profile3);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(plan, null);
		assertProfileContainsAll("A1 is missing", profile3, new IInstallableUnit[] {a1});
		assertEquals(queryResultSize(profile3.query(QueryUtil.createIUAnyQuery(), null)), 1);

		//Make a1 optional.
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile3);
		req2.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createOptionalInclusionRule(a1));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		engine.perform(plan2, null);
		assertProfileContainsAll("A1 is missing", profile3, new IInstallableUnit[] {a1});
		assertEquals(queryResultSize(profile3.query(QueryUtil.createIUAnyQuery(), null)), 1);

		//Install b1 (this should not change anything for a1)
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile3);
		req3.addInstallableUnits(new IInstallableUnit[] {b1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.OK, plan3.getStatus().getSeverity());
		engine.perform(plan3, null);
		assertProfileContainsAll("A1 or B1 is missing", profile3, new IInstallableUnit[] {a1, b1});
		assertEquals(queryResultSize(profile3.query(QueryUtil.createIUAnyQuery(), null)), 2);

		//Remove the a1 and its inclusion rule
		ProfileChangeRequest req5 = new ProfileChangeRequest(profile3);
		req5.removeInstallableUnits(new IInstallableUnit[] {a1});
		req5.removeInstallableUnitInclusionRules(a1);
		IProvisioningPlan plan5 = planner.getProvisioningPlan(req5, null, null);
		assertEquals(IStatus.OK, plan5.getStatus().getSeverity());
		engine.perform(plan5, null);
		assertProfileContainsAll("bB1 is missing", profile3, new IInstallableUnit[] {b1});
		assertEquals(queryResultSize(profile3.query(QueryUtil.createIUAnyQuery(), null)), 1);
	}

	public void testAdditionWithInclusionRule() {
		profile4 = createProfile("TestProfile4." + getName());
		//Try to Install a1 and a2
		ProfileChangeRequest req5 = new ProfileChangeRequest(profile4);
		req5.addInstallableUnits(new IInstallableUnit[] {a1, a2});
		IProvisioningPlan plan5 = planner.getProvisioningPlan(req5, null, null);
		assertEquals(IStatus.ERROR, plan5.getStatus().getSeverity());

		//Install a1 and a2 marking a1 optional 
		ProfileChangeRequest req = new ProfileChangeRequest(profile4);
		req.addInstallableUnits(new IInstallableUnit[] {a1, a2});
		req.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createOptionalInclusionRule(a1));
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(plan, null);
		assertProfileContainsAll("A2 is missing", profile4, new IInstallableUnit[] {a2});
		assertEquals(queryResultSize(profile4.query(QueryUtil.createIUAnyQuery(), null)), 1);

		//Make a1 optional, this is a no-op since a1 is not in the system
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile4);
		req2.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createOptionalInclusionRule(a1));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		engine.perform(plan2, null);
		assertProfileContainsAll("A2 is missing", profile4, new IInstallableUnit[] {a2});
		assertEquals(queryResultSize(profile4.query(QueryUtil.createIUAnyQuery(), null)), 1);

		//Install a1, this is expected to fail
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile4);
		req3.addInstallableUnits(new IInstallableUnit[] {a1});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.ERROR, plan3.getStatus().getSeverity());
	}
}
