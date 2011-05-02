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

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchTestUpdate3 extends AbstractProvisioningTest {
	private static final String PP2 = "PatchForIUP2";
	private static final String PP1 = "PatchForIUP1";
	private static final String P2 = "P2";
	private static final String P1 = "P1";
	private static final String P2_FEATURE = "p2.feature";
	private IInstallableUnit p2Feature;
	private IInstallableUnit p2Feature20;
	private IInstallableUnit p1;
	private IInstallableUnit p2;
	private IInstallableUnitPatch pp1;
	private IInstallableUnitPatch pp2;
	private IInstallableUnit p2b;
	private IInstallableUnit p1b;
	private IPlanner planner;
	private IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		p2Feature = createIU(P2_FEATURE, Version.createOSGi(1, 0, 0), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P1, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)});
		p1 = createIU(P1, Version.createOSGi(1, 0, 0), true);
		p2 = createIU(P2, Version.createOSGi(1, 0, 0), true);
		p1b = createIU(P1, Version.createOSGi(1, 1, 1), true);
		p2b = createIU(P2, Version.createOSGi(1, 1, 1), true);

		IRequirementChange changepp1 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P1, VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P1, new VersionRange("[1.1.1, 1.1.1]"), null, false, false, true));
		IRequirement lifeCyclepp1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0, 3.0.0]"), null, false, false, true);
		IRequirement[][] scopepp1 = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0, 3.0.0]"), null, false, false)}};
		pp1 = createIUPatch(PP1, Version.create("3.0.0"), true, new IRequirementChange[] {changepp1}, scopepp1, lifeCyclepp1);

		IRequirementChange changepp2 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2, VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2, new VersionRange("[1.1.1, 1.1.1]"), null, false, false, true));
		IRequirement lifeCyclepp2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true);
		IRequirement[][] scopepp2 = new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2_FEATURE, new VersionRange("[1.0.0, 1.0.0]"), null, false, false)}};
		pp2 = createIUPatch(PP2, Version.create("5.0.0"), true, new IRequirementChange[] {changepp2}, scopepp2, lifeCyclepp2);

		p2Feature20 = createIU(P2_FEATURE, Version.createOSGi(2, 0, 0), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P1, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, P2, new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)});
		createTestMetdataRepository(new IInstallableUnit[] {p2Feature, p1, p2, p1b, p2b, pp1, pp2, p2Feature20});

		createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();

		if (!install(getProfile("TestProfile." + getName()), new IInstallableUnit[] {p2Feature}, true, planner, engine).isOK())
			fail("Setup failed");

		if (!install(getProfile("TestProfile." + getName()), new IInstallableUnit[] {pp1}, false, planner, engine).isOK())
			fail("Setup failed");

		if (!install(getProfile("TestProfile." + getName()), new IInstallableUnit[] {pp2}, false, planner, engine).isOK())
			fail("Setup failed");

		assertProfileContainsAll("Profile setup incorrectly", getProfile("TestProfile." + getName()), new IInstallableUnit[] {p2Feature, pp1, p1b, pp2, p2b});
	}

	public void testUpdate() {
		ProfileChangeRequest req2 = new ProfileChangeRequest(getProfile("TestProfile." + getName()));
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		ctx.setMetadataRepositories(new URI[0]);
		assertOK("Validating the profile", planner.getProvisioningPlan(req2, ctx, null).getStatus());

		//The update of the feature will cause the patch pp2 to be uninstalled because its lifecycle is no longer matched. pp1 stays because its lifecycle is still applicable
		ProfileChangeRequest req1 = new ProfileChangeRequest(getProfile("TestProfile." + getName()));
		req1.addInstallableUnits(new IInstallableUnit[] {p2Feature20});
		req1.setInstallableUnitInclusionRules(p2Feature20, ProfileInclusionRules.createStrictInclusionRule(p2Feature20));
		req1.removeInstallableUnits(new IInstallableUnit[] {p2Feature});
		IProvisioningPlan plan = planner.getProvisioningPlan(req1, null, null);
		assertEquals(true, IStatus.ERROR != plan.getStatus().getSeverity());

		engine.perform(plan, null);
		assertProfileContains("The profile contains unexpected IUs", getProfile("TestProfile." + getName()), new IInstallableUnit[] {p2Feature20, p1b, pp1, p2});
	}
}
