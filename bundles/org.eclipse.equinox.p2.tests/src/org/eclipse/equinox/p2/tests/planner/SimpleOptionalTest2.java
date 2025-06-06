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
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleOptionalTest2 extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit b2;
	private IInstallableUnit b3;
	private IInstallableUnit c1;
	private IInstallableUnit c2;
	private IInstallableUnit d1;
	private IInstallableUnit d2;
	private IInstallableUnit x1;
	private IInstallableUnit y1;
	private IInstallableUnit z1;

	private IProfile profile;
	private IPlanner planner;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		b1 = createIU("B", Version.create("1.0.0"), true);
		b2 = createIU("B", Version.create("2.0.0"), true);
		b3 = createIU("B", Version.create("3.0.0"), true);

		c1 = createIU("C", Version.create("1.0.0"), true);
		c2 = createIU("C", Version.create("2.0.0"), true);

		d1 = createIU("D", Version.create("1.0.0"), true);
		d2 = createIU("D", Version.create("2.0.0"), true);

		y1 = createIU("Y", Version.create("1.0.0"), true);

		z1 = createIU("Z", Version.create("1.0.0"), true);

		//B's dependency is missing
		IRequirement[] reqA = new IRequirement[3];
		reqA[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true);
		reqA[1] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, true, false, true);
		reqA[2] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, true, false, true);
		a1 = createIU("A", Version.create("1.0.0"), reqA);

		IRequirement[] req = new IRequirement[3];
		req[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true);
		req[1] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "Y", VersionRange.emptyRange, null, false, false, true);
		req[2] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "Z", VersionRange.emptyRange, null, true, false, true);
		x1 = createIU("X", req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, b3, c1, c2, d1, d2, x1, z1, y1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		//Ensure that D's installation does not fail because of C's absence
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(a1, x1);
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, c2);
		assertInstallOperand(plan, d1);
		assertInstallOperand(plan, y1);
		assertInstallOperand(plan, z1);
		assertInstallOperand(plan, x1);
	}
}
