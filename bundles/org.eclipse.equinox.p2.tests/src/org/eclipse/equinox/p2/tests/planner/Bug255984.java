/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug255984 extends AbstractProvisioningTest {
	IInstallableUnit a;
	IInstallableUnit b;
	IInstallableUnit c;
	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a = createIU("A", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)}, NO_PROPERTIES, true);

		b = createIU("B", Version.create("1.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a, b});

		planner = createPlanner();
		engine = createEngine();
	}

	public void testProperties() {
		//Install B
		profile1 = createProfile("TestProfile." + getName());
		ProfileChangeRequest req = new ProfileChangeRequest(profile1);
		req.addInstallableUnits(new IInstallableUnit[] {b});
		req.setInstallableUnitInclusionRules(b, ProfileInclusionRules.createStrictInclusionRule(b));
		req.setInstallableUnitProfileProperty(b, "foo", "bar");
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(plan, null);
		assertProfileContainsAll("B is missing", profile1, new IInstallableUnit[] {b});
		assertEquals(1, queryResultSize(profile1.query(QueryUtil.createIUAnyQuery(), null)));

		//Install A
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {a});
		req2.setInstallableUnitInclusionRules(a, ProfileInclusionRules.createStrictInclusionRule(a));
		req2.setInstallableUnitProfileProperty(a, "foo", "bar");
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		engine.perform(plan2, null);
		assertProfileContainsAll("A is missing", profile1, new IInstallableUnit[] {a, b});
		assertEquals(2, queryResultSize(profile1.query(QueryUtil.createIUAnyQuery(), null)));

		//Uninstall B
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.removeInstallableUnits(new IInstallableUnit[] {b});
		req3.removeInstallableUnitProfileProperty(b, "foo");
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.OK, plan3.getStatus().getSeverity());
		engine.perform(plan3, null);
		assertProfileContainsAll("A is missing", profile1, new IInstallableUnit[] {a, b});
		assertEquals(1, queryResultSize(profile1.query(new IUProfilePropertyQuery("foo", "bar"), new NullProgressMonitor())));
		assertEquals(2, queryResultSize(profile1.query(QueryUtil.createIUAnyQuery(), null)));
	}
}
