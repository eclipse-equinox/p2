package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
public class AbsolutePlanTest extends AbstractProvisioningTest {
	public void testAddAndRemoveIU() {
		IProfile profile = createProfile(getName());
		IEngine engine = createEngine();
		IProvisioningPlan plan = engine.createPlan(profile, new ProvisioningContext(getAgent()));
		IInstallableUnit iuA = createEclipseIU("A");
		plan.addInstallableUnit(iuA);
		plan.addInstallableUnit(createEclipseIU("B"));
		plan.addInstallableUnit(createEclipseIU("C"));
		assertEquals(3, countPlanElements(plan));
		engine.perform(plan, null);

		IProvisioningPlan plan2 = engine.createPlan(profile, new ProvisioningContext(getAgent()));
		plan2.removeInstallableUnit(iuA);
		assertEquals(1, countPlanElements(plan2));
	}

	public void testAddAndRemoveProperty() {
		IInstallableUnit iuA = createEclipseIU("A");
		IProfile profile = createProfile(getName());

		IEngine engine = createEngine();
		IProvisioningPlan plan = engine.createPlan(profile, new ProvisioningContext(getAgent()));
		plan.addInstallableUnit(iuA);
		plan.setInstallableUnitProfileProperty(iuA, "key", "value");

		assertEquals(1, countPlanElements(plan));
		engine.perform(plan, null);

		Operand[] ops = ((ProvisioningPlan) plan).getOperands();
		boolean found = false;
		for (Operand op : ops) {
			if (op instanceof InstallableUnitPropertyOperand) {
				found = true;
				break;
			}
		}
		assertTrue(found);

		IProvisioningPlan plan2 = engine.createPlan(profile, new ProvisioningContext(getAgent()));
		plan2.removeInstallableUnit(iuA);
		plan2.setInstallableUnitProfileProperty(iuA, "key", null);

		assertEquals(1, countPlanElements(plan2));
	}

	public void testAddProperty() {
		IProfile profile = createProfile(getName());
		IEngine engine = createEngine();

		IProvisioningPlan plan = engine.createPlan(profile, new ProvisioningContext(getAgent()));
		plan.setProfileProperty("foo", "bar");
		engine.perform(plan, null);

		assertEquals("bar", getProfileRegistry().getProfile(getName()).getProperty("foo"));
	}
}
