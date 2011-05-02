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

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchTestMultiplePatch3 extends AbstractProvisioningTest {
	private static final String PP2 = "PatchForIUP2";
	private static final String PP1 = "PatchForIUP1";
	private static final String P3 = "P3";
	private static final String P2 = "P2";
	private static final String P1 = "P1";
	private static final String P2_FEATURE = "p2.feature";
	private IInstallableUnit p2Feature;
	private IInstallableUnit p1;
	private IInstallableUnit p2;
	private IInstallableUnit p3;
	private IInstallableUnitPatch pp1;
	private IInstallableUnitPatch pp2;
	private IInstallableUnit p1c;
	private IInstallableUnit p1b;
	private IProfile profile1;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		p2Feature = createIU(P2_FEATURE, Version.createOSGi(1, 0, 0), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P3, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P1, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)});
		p1 = createIU(P1, Version.createOSGi(1, 0, 0), true);
		p2 = createIU(P2, Version.createOSGi(1, 0, 0), true);
		p1b = createIU(P1, Version.createOSGi(1, 1, 1), true);
		p1c = createIU(P1, Version.createOSGi(1, 1, 2), true);
		p3 = createIU(P3, Version.createOSGi(1, 0, 0), true);

		IRequirementChange changepp1 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P1, VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P1, new VersionRange("[1.1.1, 1.1.1]"), null, false, false, true));
		IRequirement lifeCyclepp1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true);
		IRequirement[][] scopepp1 = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0,1.0.0]"), null, false, false)}};
		pp1 = createIUPatch(PP1, Version.create("3.0.0"), true, new IRequirementChange[] {changepp1}, scopepp1, lifeCyclepp1);

		IRequirementChange changepp2 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P1, VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P1, new VersionRange("[1.1.2, 1.1.2]"), null, false, false, true));
		IRequirement lifeCyclepp2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true);
		IRequirement[][] scopepp2 = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0, 1.0.0]"), null, false, false)}};
		pp2 = createIUPatch(PP2, Version.create("5.0.0"), true, new IRequirementChange[] {changepp2}, scopepp2, lifeCyclepp2);
		pp2 = createIUPatch(PP2, Version.create("5.0.0"), null, new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "missingThing", new VersionRange("[1.0.0, 1.0.0]"), null, false, false)}, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, true, null, new IRequirementChange[] {changepp2}, scopepp2, lifeCyclepp2, NO_REQUIRES);
		createTestMetdataRepository(new IInstallableUnit[] {p2Feature, p1, p2, p3, p1b, p1c, pp1, pp2});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	//Test that two patches applying to the same requirement of an IU being patched does not cause any problem when one of the two patch is not applied.
	public void testFailingInstall() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile1);
		req.addInstallableUnits(new IInstallableUnit[] {p2Feature});
		req.addInstallableUnits(new IInstallableUnit[] {pp1});
		req.addInstallableUnits(new IInstallableUnit[] {pp2});
		req.setInstallableUnitInclusionRules(pp2, ProfileInclusionRules.createOptionalInclusionRule(pp2));

		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertOK("Planning should be ok", plan.getStatus());
	}
}
