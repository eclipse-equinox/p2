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

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ActualChangeRequestTest extends AbstractProvisioningTest {
	IInstallableUnit a;
	IInstallableUnit b;
	IInstallableUnit c;
	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a = createIU("A", Version.create("1.0.0"), new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)}, NO_PROPERTIES, true);

		b = createIU("B", Version.create("1.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a, b});

		planner = createPlanner();
		engine = createEngine();
	}

	public void testRequestStatus() {
		//Install B
		profile1 = createProfile("TestProfile." + getName());
		ProfileChangeRequest req = new ProfileChangeRequest(profile1);
		req.addInstallableUnits(new IInstallableUnit[] {b});
		req.setInstallableUnitInclusionRules(b, PlannerHelper.createStrictInclusionRule(b));
		req.setInstallableUnitProfileProperty(b, "foo", "bar");
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getRequestStatus(b).getSeverity());
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(profile1, new DefaultPhaseSet(), plan.getOperands(), null, null);
		assertProfileContainsAll("B is missing", profile1, new IInstallableUnit[] {b});
		assertEquals(1, profile1.query(InstallableUnitQuery.ANY, new Collector(), null).size());

		//Install A
		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {a});
		req2.setInstallableUnitInclusionRules(a, PlannerHelper.createStrictInclusionRule(a));
		req2.setInstallableUnitProfileProperty(a, "foo", "bar");
		ProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		assertNull(plan2.getRequestStatus(b));
		Map m = plan2.getSideEffectChanges();
		m.toString();
		assertEquals(IStatus.OK, plan2.getRequestStatus(a).getSeverity());
		engine.perform(profile1, new DefaultPhaseSet(), plan2.getOperands(), null, null);
		assertProfileContainsAll("A is missing", profile1, new IInstallableUnit[] {a, b});
		assertEquals(2, profile1.query(InstallableUnitQuery.ANY, new Collector(), null).size());

		//Uninstall B
		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.removeInstallableUnits(new IInstallableUnit[] {b});
		req3.removeInstallableUnitProfileProperty(b, "foo");
		ProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, null);
		assertEquals(IStatus.OK, plan3.getStatus().getSeverity());
		assertEquals(IStatus.ERROR, plan3.getRequestStatus(b).getSeverity());
	}
}
