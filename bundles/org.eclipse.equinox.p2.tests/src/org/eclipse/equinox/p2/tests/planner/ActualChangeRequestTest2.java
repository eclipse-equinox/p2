/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ActualChangeRequestTest2 extends AbstractProvisioningTest {
	IInstallableUnit a;
	IInstallableUnit b;
	IInstallableUnit b2;
	IInstallableUnit c;
	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a = createIU("A", new Version("1.0.0"), new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)}, NO_PROPERTIES, true);

		b = createIU("B", new Version("1.0.0"), true);
		b2 = createIU("B", new Version("2.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a, b2, b});

		planner = createPlanner();
		engine = createEngine();
	}

	public void testRequestStatus() {
		//Install B
		profile1 = createProfile("TestProfile." + getName());
		ProfileChangeRequest req = new ProfileChangeRequest(profile1);
		req.addInstallableUnits(new IInstallableUnit[] {b});
		req.setInstallableUnitInclusionRules(b, PlannerHelper.createStrictInclusionRule(b));
		req.addInstallableUnits(new IInstallableUnit[] {a});
		req.setInstallableUnitInclusionRules(a, PlannerHelper.createOptionalInclusionRule(a));

		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getRequestStatus(b).getSeverity());
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(profile1, new DefaultPhaseSet(), plan.getOperands(), null, null);
		assertProfileContainsAll("B is missing", profile1, new IInstallableUnit[] {a, b});
		assertEquals(2, profile1.query(InstallableUnitQuery.ANY, new Collector(), null).size());

		//Install B2
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {b2});
		req2.setInstallableUnitInclusionRules(b2, PlannerHelper.createStrictInclusionRule(b2));
		req2.removeInstallableUnits(new IInstallableUnit[] {b});
		ProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		assertNotNull(plan2.getRequestStatus(b));
		assertNotNull(plan2.getRequestStatus(b2));
		Map m = plan2.getSideEffectChanges();
		assertEquals(1, m.size());
		assertNotNull(m.get(a));
		assertEquals(IStatus.INFO, ((RequestStatus) m.get(a)).getSeverity());
		assertEquals(RequestStatus.REMOVED, ((RequestStatus) m.get(a)).getInitialRequestType());
		engine.perform(profile1, new DefaultPhaseSet(), plan2.getOperands(), null, null);
		assertProfileContainsAll("A is missing", profile1, new IInstallableUnit[] {b2});
		assertEquals(1, profile1.query(InstallableUnitQuery.ANY, new Collector(), null).size());

		//Try to Install A
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {a});
		req3.setInstallableUnitInclusionRules(a, PlannerHelper.createOptionalInclusionRule(a));
		ProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertNotNull(plan3.getRequestStatus(a));
		assertEquals(IStatus.ERROR, plan3.getRequestStatus(a).getSeverity());
		assertEquals(RequestStatus.ADDED, plan3.getRequestStatus(a).getInitialRequestType());

		//Try to Install A
		ProfileChangeRequest req4 = new ProfileChangeRequest(profile1);
		req4.addInstallableUnits(new IInstallableUnit[] {a});
		req4.setInstallableUnitInclusionRules(a, PlannerHelper.createStrictInclusionRule(a));
		ProvisioningPlan plan4 = planner.getProvisioningPlan(req4, null, null);
		assertNotNull(plan4.getRequestStatus(a));
		assertEquals(IStatus.ERROR, plan4.getRequestStatus(a).getSeverity());
		assertEquals(RequestStatus.ADDED, plan4.getRequestStatus(a).getInitialRequestType());
	}
}
