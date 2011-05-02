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

public class MissingDependency3 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	private IProfile profile;
	private IPlanner planner;

	//This tests that A can still be resolved and installed
	protected void setUp() throws Exception {
		super.setUp();
		IRequirement[] reqA = new IRequirement[1];
		reqA[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, true, false, true);
		a1 = createIU("A", Version.create("1.0.0"), reqA);

		//Missing dependency
		IRequirement[] req = new IRequirement[1];
		req[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, false, false, true);
		b1 = createIU("B", Version.create("1.0.0"), req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testContradiction() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
	}
}
