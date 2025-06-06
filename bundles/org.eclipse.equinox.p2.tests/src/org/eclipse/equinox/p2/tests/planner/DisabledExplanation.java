/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class DisabledExplanation extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	private IProfile profile;
	private IPlanner planner;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), true);

		//Missing non optional dependency
		IRequirement[] req = new IRequirement[1];
		req[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, false, false, true);
		b1 = createIU("B", Version.create("1.0.0"), req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testExplanation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(a1, b1);
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		ctx.setProperty("org.eclipse.equinox.p2.director.explain", "false");
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(req, ctx, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		assertFalse((plan.getStatus() instanceof PlannerStatus));
	}
}
