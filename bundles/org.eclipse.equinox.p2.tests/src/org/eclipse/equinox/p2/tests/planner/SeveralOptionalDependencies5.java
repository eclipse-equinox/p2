/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.p2.metadata.MetadataFactory;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SeveralOptionalDependencies5 extends AbstractProvisioningTest {
	private IInstallableUnit x1;
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit c1;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		IRequiredCapability reqA = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, true, false, true); //optional dependency, will be satisfied
		IRequiredCapability reqC = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[1.0.0, 2.0.0)"), null, true, false, true); //optional dependency, will be satisfied because it is the highest version
		IRequiredCapability reqE = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "E", new VersionRange("[1.0.0, 2.0.0)"), null, true, false, true); //optional dependency, will be satisfied because it is the highest version
		x1 = createIU("X", Version.create("1.0.0"), new IRequiredCapability[] {reqA, reqC, reqE});

		IRequiredCapability reqB = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true); //optional dependency, will be satisfied
		a1 = createIU("A", Version.create("1.0.0"), new IRequiredCapability[] {reqB});

		IRequiredCapability reqD = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, false, false, true); //optional dependency, will be satisfied
		c1 = createIU("C", Version.create("1.0.0"), new IRequiredCapability[] {reqD});

		b1 = createIU("B", Version.create("1.0.0"), true);
		createTestMetdataRepository(new IInstallableUnit[] {x1, a1, b1, c1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {x1});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, x1);
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, b1);
		assertNoOperand(plan, c1);
	}
}