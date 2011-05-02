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

public class AllOptional extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit c1;
	private IInstallableUnit d1;
	private IInstallableUnit e1;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		IRequirement[] reqA = new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true)};
		a1 = createIU("A", Version.create("1.0.0"), reqA);
		b1 = createIU("B", Version.create("1.0.0"), true);

		IRequirement[] reqC = new IRequirement[2];
		reqC[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, true, false, true);
		reqC[1] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "E", VersionRange.emptyRange, null, true, false, true);
		c1 = createIU("C", Version.create("1.0.0"), reqC);
		d1 = createIU("D", Version.create("1.0.0"), true);
		e1 = createIU("E", Version.create("1.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, c1, d1, e1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1, c1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, b1);
		assertInstallOperand(plan, c1);
		assertInstallOperand(plan, d1);
		assertInstallOperand(plan, e1);
	}
}
