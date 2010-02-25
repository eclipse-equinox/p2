package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
public class AbsolutePlanTest extends AbstractProvisioningTest {
	public void testAddAndRemoveIU() {
		IProfile profile = createProfile(getName());
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.setAbsoluteMode(true);
		IInstallableUnit iuA = createEclipseIU("A");
		pcr.addInstallableUnits(new IInstallableUnit[] {iuA, createEclipseIU("B"), createEclipseIU("C")});
		IPlanner planner = createPlanner();
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, new ProvisioningContext(), null);
		assertEquals(3, countPlanElements(plan));
		createEngine().perform(plan, null);

		ProfileChangeRequest removeRequest = new ProfileChangeRequest(profile);
		removeRequest.setAbsoluteMode(true);
		removeRequest.removeInstallableUnits(new IInstallableUnit[] {iuA});
		assertEquals(1, countPlanElements(planner.getProvisioningPlan(removeRequest, new ProvisioningContext(), null)));
	}

	public void testAddAndRemoveProperty() {
		IInstallableUnit iuA = createEclipseIU("A");
		IProfile profile = createProfile(getName());

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.setAbsoluteMode(true);
		pcr.addInstallableUnits(new IInstallableUnit[] {iuA});
		pcr.setInstallableUnitProfileProperty(iuA, "key", "value");

		IPlanner planner = createPlanner();
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(pcr, new ProvisioningContext(), null);
		assertEquals(1, countPlanElements(plan));
		createEngine().perform(plan, null);

		Operand[] ops = plan.getOperands();
		boolean found = false;
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitPropertyOperand)
				found = true;
		}
		assertTrue(found);

		ProfileChangeRequest removeRequest = new ProfileChangeRequest(profile);
		removeRequest.setAbsoluteMode(true);
		removeRequest.removeInstallableUnits(new IInstallableUnit[] {iuA});
		removeRequest.removeInstallableUnitProfileProperty(iuA, "key");

		assertEquals(1, countPlanElements(planner.getProvisioningPlan(removeRequest, new ProvisioningContext(), null)));
	}

	public void testAddProperty() {
		IProfile profile = createProfile(getName());

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.setAbsoluteMode(true);
		pcr.setProfileProperty("foo", "bar");

		IPlanner planner = createPlanner();
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, new ProvisioningContext(), null);
		createEngine().perform(plan, null);

		assertEquals("bar", getProfileRegistry().getProfile(getName()).getProperty("foo"));
	}
}
