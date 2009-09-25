/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleSingleton extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a2;
	IInstallableUnit a3;
	IInstallableUnit a4;
	IInstallableUnit u, v, w, x, y, z;

	IPlanner planner;
	IProfile profile;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", new Version("1.0.0"), true);

		a2 = createIU("A", new Version("2.0.0"), true);

		IRequiredCapability c1 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IRequiredCapability c2 = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 2.0.0]"), null, false, false);
		y = createIU("Y", new Version(2, 0, 0), new IRequiredCapability[] {c1, c2});

		createTestMetdataRepository(new IInstallableUnit[] {a1, a2, y});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();

	}

	public void test1() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {y});
		ProvisioningPlan provisioningPlan = planner.getProvisioningPlan(req, null, null);
		assertEquals(1, provisioningPlan.getCompleteState().query(new InstallableUnitQuery("A"), new Collector(), null).size());
		assertEquals(0, provisioningPlan.getInstallerPlan().getCompleteState().query(new InstallableUnitQuery("A"), new Collector(), null).size());
		assertEquals(IStatus.ERROR, provisioningPlan.getStatus().getSeverity());
	}

	public void testExplanation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {y});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		assertEquals(Explanation.VIOLATED_SINGLETON_CONSTRAINT, plan.getRequestStatus().getShortExplanation());
		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(y));
	}
}
