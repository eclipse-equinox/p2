/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class AddIUProperty extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a2;
	IProfile profile;
	IPlanner planner;
	IEngine engine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), true);

		a2 = createIU("A", Version.create("2.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, a2});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testWithoutIUProperty() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(a1);
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
	}

	public void testWithIUProperty() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(a1);
		req.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createOptionalInclusionRule(a1));
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
	}

	public void testChangeIUProperty() {
		//Add a1, strictly ;
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(a1);
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(plan, null);
		assertProfileContainsAll("A1 is missing", profile, new IInstallableUnit[] {a1});
		IQueryResult<IInstallableUnit> allProfileIUs = profile.query(QueryUtil.createIUAnyQuery(), null);
		assertEquals(queryResultSize(allProfileIUs), 1);

		//Add a2 with a1. This is an error
		ProfileChangeRequest req4 = ProfileChangeRequest.createByProfileId(getAgent(), profile.getProfileId());
		req4.addInstallableUnits(a2);
		IProvisioningPlan plan4 = planner.getProvisioningPlan(req4, null, null);
		assertEquals(IStatus.ERROR, plan4.getStatus().getSeverity());

		//Add a2, making a1 optional;
		ProfileChangeRequest req2 = ProfileChangeRequest.createByProfileId(getAgent(), profile.getProfileId());
		req2.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createOptionalInclusionRule(a1));
		req2.addInstallableUnits(a2);
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan2, a2);

		engine.perform(plan2, null);
		profile = getProfile(profile.getProfileId());
		assertProfileContainsAll("A2 is missing", profile, new IInstallableUnit[] {a2});
		allProfileIUs = profile.query(QueryUtil.createIUAnyQuery(), null);
		assertEquals(queryResultSize(allProfileIUs), 1);

		IQueryResult<IInstallableUnit> iuProfileProperties = profile.query(new IUProfilePropertyQuery(SimplePlanner.INCLUSION_RULES, IUProfilePropertyQuery.ANY), null);
		assertEquals(queryResultSize(iuProfileProperties), 1);

		//Remove a1 optionality - should be a no-op
		ProfileChangeRequest req3 = ProfileChangeRequest.createByProfileId(getAgent(), profile.getProfileId());
		req3.removeInstallableUnitInclusionRules(a1);
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(plan3, null);
		allProfileIUs = profile.query(QueryUtil.createIUAnyQuery(), null);
		assertProfileContainsAll("A2 is missing", profile, new IInstallableUnit[] {a2});
		assertEquals(queryResultSize(allProfileIUs), 1);
	}
}
