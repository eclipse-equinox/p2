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
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleOptionalTest3 extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit b2;
	private IInstallableUnit b3;
	private IInstallableUnit c1;
	private IInstallableUnit c2;
	private IInstallableUnit d1;
	private IInstallableUnit d2;
	private IInstallableUnit x1;

	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		b1 = createIU("B", Version.create("1.0.0"), true);
		b2 = createIU("B", Version.create("2.0.0"), true);
		b3 = createIU("B", Version.create("3.0.0"), true);

		c1 = createIU("C", Version.create("1.0.0"), true);
		c2 = createIU("C", Version.create("2.0.0"), true);

		d1 = createIU("D", Version.create("1.0.0"), true);
		d2 = createIU("D", Version.create("2.0.0"), true);

		IRequirement[] reqA = new IRequirement[3];
		reqA[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, true, false, true);
		reqA[1] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, false, false, true);
		reqA[2] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, true, false, true);
		a1 = createIU("A", Version.create("1.0.0"), reqA);

		IRequirement[] req = new IRequirement[1];
		req[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 2.0.0]"), null, false, false, true);
		x1 = createIU("X", req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, b3, c1, c2, d1, d2, x1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		//Ensure that A and X1's installation does not fail because of C's absence
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1, x1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, c2);
		assertInstallOperand(plan, d2);
		assertInstallOperand(plan, x1);
	}
}
