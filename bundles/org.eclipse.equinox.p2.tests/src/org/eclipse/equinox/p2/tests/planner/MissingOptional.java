/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class MissingOptional extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit d;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		a1 = createIU("A", new Version("1.0.0"), true);
		b1 = createIU("B", new Version("1.0.0"), true);

		RequiredCapability[] req = new RequiredCapability[3];
		req[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, true);
		req[1] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, true, false, true);
		req[2] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "C", VersionRange.emptyRange, null, true, false, true);
		d = createIU("D", req);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, d});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	private void assertInstallOperand(ProvisioningPlan plan, IInstallableUnit iu) {
		Operand[] ops = plan.getOperands();
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitOperand) {
				InstallableUnitOperand iuOp = (InstallableUnitOperand) ops[i];
				if (iuOp.second().equals(iu))
					return;
			}
		}
		fail("Can't find " + iu + " in the plan");
	}

	private void assertNoOperand(ProvisioningPlan plan, IInstallableUnit iu) {
		Operand[] ops = plan.getOperands();
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitOperand) {
				InstallableUnitOperand iuOp = (InstallableUnitOperand) ops[i];
				if (iuOp.second() != null && iuOp.second().equals(iu))
					fail(iu + " should not be present in this plan.");
				if (iuOp.first() != null && iuOp.first().equals(iu))
					fail(iu + " should not be present in this plan.");
			}
		}
	}

	public void testInstallation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {d});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
		assertNoOperand(plan, b1);
		assertInstallOperand(plan, d);
	}
}
