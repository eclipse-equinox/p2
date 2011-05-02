/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchFailingToInstall extends AbstractProvisioningTest {
	private IInstallableUnit p2Feature;
	private IInstallableUnit p1;
	private IInstallableUnit p2;
	private IInstallableUnitPatch pp1;
	private IInstallableUnitPatch pp2;
	private IInstallableUnit p2b;
	private IInstallableUnit p1b;
	private IProfile profile1;
	private IPlanner planner;
	private IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		p2Feature = createIU("p2.feature", Version.createOSGi(1, 0, 0), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "P1", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "P2", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)});
		p1 = createIU("P1", Version.createOSGi(1, 0, 0), true);
		p2 = createIU("P2", Version.createOSGi(1, 0, 0), true);
		p1b = createIU("P1", Version.createOSGi(1, 1, 1), true);
		p2b = createIU("P2", Version.createOSGi(1, 1, 1), true);

		IRequirementChange changepp1 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirement lifeCyclepp1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "p2.feature", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true);
		IRequirement[][] scopepp1 = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "P1", new VersionRange("[1.1.1,1.1.1]"), null, false, false)}};
		pp1 = createIUPatch("PP1", Version.create("3.0.0"), true, new IRequirementChange[] {changepp1}, scopepp1, lifeCyclepp1);

		IRequirementChange changepp2 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.3.0)"), null, false, false, true));
		IRequirement lifeCyclepp2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "p2.feature", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true);
		IRequirement[][] scopepp2 = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "P2", new VersionRange("[1.1.1,1.1.1]"), null, false, false)}};
		pp2 = createIUPatch("PP2", Version.create("5.0.0"), true, new IRequirementChange[] {changepp2}, scopepp2, lifeCyclepp2);

		createTestMetdataRepository(new IInstallableUnit[] {p2Feature, p1, p2, p1b, p2b, pp1, pp2});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();

		if (!install(profile1, new IInstallableUnit[] {p2Feature}, true, planner, engine).isOK())
			fail("Setup failed");

		if (install(profile1, new IInstallableUnit[] {pp1}, false, planner, engine).getSeverity() == IStatus.ERROR)
			fail("Setup failed while installing patch");

		if (install(profile1, new IInstallableUnit[] {pp2}, true, planner, engine).getSeverity() == IStatus.ERROR)
			fail("Setup failed while installing patch");

		fail("this should likely fail since the scope does not match anything");
		assertProfileContainsAll("Profile setup incorrectly", profile1, new IInstallableUnit[] {p2Feature, p1, p2, pp1, pp2});
	}

	public void testUninstall() {
		IStatus result = uninstall(profile1, new IInstallableUnit[] {pp1}, planner, engine);
		assertTrue("1.0", result.isOK());
	}
}
