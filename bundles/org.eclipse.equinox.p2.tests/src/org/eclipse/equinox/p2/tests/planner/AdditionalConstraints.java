/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.Arrays;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class AdditionalConstraints extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit b2;
	IInstallableUnit b3;
	IInstallableUnit x1;

	IProfile profile;
	IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 4.0.0)")));

		b1 = createIU("B", Version.create("1.0.0"), true);

		b2 = createIU("B", Version.create("2.0.0"), true);

		b3 = createIU("B", Version.create("3.0.0"), true);

		x1 = createIU("X", Version.createOSGi(2, 0, 0), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 2.0.0]")));

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, b3, x1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();

	}

	public void testInstallA1() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		req.addExtraRequirements(Arrays.<IRequirement> asList(createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 2.0.0]"))[0]));
		IProvisioningPlan plan = planner.getProvisioningPlan(req, ctx, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, b2);
		assertNoOperand(plan, x1);
	}

	public void testExtraRequirement() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		req.addExtraRequirements(Arrays.<IRequirement> asList(createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 4.0.0]"))[0]));
		IProvisioningPlan plan = planner.getProvisioningPlan(req, ctx, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, b3);
		assertNoOperand(plan, x1);
	}
}
