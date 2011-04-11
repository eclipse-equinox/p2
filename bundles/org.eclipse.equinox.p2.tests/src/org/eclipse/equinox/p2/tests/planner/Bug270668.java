/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug270668 extends AbstractProvisioningTest {

	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IProfile profile;
	private IPlanner planner;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), "(os=win32)", new IProvidedCapability[] {new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", Version.create("1.0.0"))});
		b1 = createIU("B", Version.create("1.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallStrict() {
		IProfileChangeRequest pcr = planner.createChangeRequest(profile);
		pcr.add(a1);
		pcr.add(b1);
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertNotOK(plan.getStatus());
		assertTrue("Explanation does not mention filter!", plan.getStatus().getChildren()[0].getMessage().contains("filter"));
	}

	public void testInstallOptional() {
		IProfileChangeRequest pcr = planner.createChangeRequest(profile);
		pcr.add(a1);
		pcr.add(b1);
		pcr.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createOptionalInclusionRule(a1));
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertOK("Optional install", plan.getStatus());
	}
}
