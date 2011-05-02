/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
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

public class DropinsScenario extends AbstractProvisioningTest {
	IInstallableUnit a0;
	IInstallableUnit b0;
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit as;
	IInstallableUnit bs;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), true);

		b1 = createIU("B", Version.create("1.0.0"), true);

		a0 = createIU("A", Version.create("0.0.0"), true);
		b0 = createIU("B", Version.create("0.0.0"), true);

		IRequirement[] reqAs = new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[0.0.0, 1.0.0]"), null, false, false, true)};
		as = createIU("AS", Version.create("0.0.0"), reqAs);

		IRequirement[] reqBs = new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[0.0.0, 1.0.0]"), null, false, false, true)};
		bs = createIU("BS", Version.create("0.0.0"), reqBs);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, a0, b0, as, bs});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {as, bs});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());

	}
}
