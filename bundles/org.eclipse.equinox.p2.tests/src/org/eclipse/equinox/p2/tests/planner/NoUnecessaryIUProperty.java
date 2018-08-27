/*******************************************************************************
 * Copyright (c) 2010, 2017 Sonatype, Inc. Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class NoUnecessaryIUProperty extends AbstractProvisioningTest {

	IProfile profile1;
	IPlanner planner;
	IEngine engine;
	IMetadataRepository repo;
	IInstallableUnit iuA, iuB;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
		iuA = createIU("A");
		iuB = createIU("B");
	}

	//Confirm that the planner will not generate an IUProperty operand if the IU is not in the final profile.
	public void testIUExtraneousPlanEntry() {
		IProfileChangeRequest pcr = planner.createChangeRequest(profile1);
		pcr.add(iuA);
		pcr.setInstallableUnitProfileProperty(iuB, "theKey", "theValue"); // Try to set a property on an IU that does not end up in plan
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, new NullProgressMonitor());
		assertEquals(2, ((ProvisioningPlan) plan).getOperands().length);

	}

	public void testZeroOperands() {
		IProfileChangeRequest pcr = planner.createChangeRequest(profile1);
		pcr.setInstallableUnitProfileProperty(iuB, "theKey", "theValue"); // Try to set a property on an IU that does not end up in plan
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, new NullProgressMonitor());
		assertEquals(0, ((ProvisioningPlan) plan).getOperands().length);

	}
}
