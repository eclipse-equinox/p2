/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others. All rights reserved. This
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

public class SeveralOptionalDependencies3 extends AbstractProvisioningTest {
	private IInstallableUnit x1;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		IRequirement reqA = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, true, false, true); //optional dependency, will not be satisfied
		IRequirement reqB = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, true, false, true); //optional dependency, will not be satisfied
		IRequirement reqC = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[0.0.0, 1.0.0)"), null, true, false, true); //will not match
		IRequirement reqD = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, true, false, true); //will not match
		x1 = createIU("X", Version.create("1.0.0"), new IRequirement[] {reqA, reqB, reqC, reqD});

		createTestMetdataRepository(new IInstallableUnit[] {x1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		//X will install because all the requirements are optional
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {x1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, x1);
	}
}