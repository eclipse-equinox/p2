/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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

public class SeveralOptionalDependencies4 extends AbstractProvisioningTest {
	private IInstallableUnit x1;
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit a2;
	private IProfile profile;
	private IPlanner planner;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IRequirement reqA = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, true, false, true); //optional dependency, will be satisfied
		IRequirement reqB = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true); //normal dependency
		x1 = createIU("X", Version.create("1.0.0"), new IRequirement[] {reqA, reqB,});
		a1 = createIU("A", Version.create("1.0.0"), true);
		a2 = createIU("A", Version.create("2.0.0"), true);
		b1 = createIU("B", Version.create("1.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {x1, a1, b1, a2});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(x1);
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, x1);
		assertInstallOperand(plan, b1);
		assertInstallOperand(plan, a2);
		assertNoOperand(plan, a1);
	}
}