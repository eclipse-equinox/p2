/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.Set;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MultipleSingleton extends AbstractProvisioningTest {
	IInstallableUnit a1, a2, a3, a4;
	IInstallableUnit u, v, w, x, y, z;
	IInstallableUnit sdk1, sdk2, platform1, platform2, third1, third2, s1, s2, t1, t2, random;

	IPlanner planner;
	IProfile profile;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), true);
		a2 = createIU("A", Version.create("2.0.0"), true);
		a3 = createIU("A", Version.create("3.0.0"), false);
		a4 = createIU("A", Version.create("4.0.0"), false);

		x = createIU("X", Version.createOSGi(2, 0, 0), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 4.1.0)")));

		IRequirement c1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IRequirement c2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 2.0.0]"), null, false, false);
		y = createIU("Y", Version.createOSGi(2, 0, 0), new IRequirement[] {c1, c2});

		IRequirement c3 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[3.0.0, 3.0.0]"), null, false, false);
		IRequirement c4 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[4.0.0, 4.0.0]"), null, false, false);
		z = createIU("Z", Version.createOSGi(2, 0, 0), new IRequirement[] {c3, c4});

		IRequirement c5 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IRequirement c6 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[3.0.0, 3.0.0]"), null, false, false);
		w = createIU("W", Version.createOSGi(2, 0, 0), new IRequirement[] {c5, c6});

		IRequirement c7 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 2.0.0]"), null, false, false);
		IRequirement c8 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[3.0.0, 4.0.0]"), null, false, false);
		v = createIU("V", Version.createOSGi(2, 0, 0), new IRequirement[] {c7});
		u = createIU("U", Version.createOSGi(2, 0, 0), new IRequirement[] {c8});

		// create IUs to mimic the platform, SDK, and a third-party feature
		platform1 = createIU("platform", Version.create("1.0.0"), true);
		platform2 = createIU("platform", Version.create("2.0.0"), true);
		sdk1 = createIU("sdk", Version.create("1.0.0"), true);
		sdk2 = createIU("sdk", Version.create("2.0.0"), true);
		third1 = createIU("third", Version.create("1.0.0"), true);
		third2 = createIU("third", Version.create("2.0.0"), true);
		random = createIU("random");

		IRequirement pr1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "platform", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IRequirement pr2 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "platform", new VersionRange("[2.0.0, 2.0.0]"), null, false, false);

		// the sdk has a dependency on the platform
		s1 = createIU("s1", Version.createOSGi(1, 0, 0), new IRequirement[] {pr1});
		s2 = createIU("s2", Version.createOSGi(2, 0, 0), new IRequirement[] {pr2});

		// the third-party feature has a dependency on the platform
		t1 = createIU("t1", Version.createOSGi(1, 0, 0), new IRequirement[] {pr1});
		t2 = createIU("t2", Version.createOSGi(2, 0, 0), new IRequirement[] {pr2});

		// create a repo with the data in it
		createTestMetdataRepository(new IInstallableUnit[] {a1, a2, a3, a4, w, x, y, z, platform1, platform2, sdk1, sdk2, third1, third2, s1, s2, t1, t2, random});

		profile = createProfile("TestProfile.MultipleSingleton" + getName());
		planner = createPlanner();
	}

	/*
	 * Install both the SDK and Third as roots. 
	 * Both have a dependency on the Platform.
	 * Try to update the SDK.
	 * Operation blocked because Third also depends on Platform.
	 */
	public void testSDKandThirdPartyConflict() {
		assertOK(install(profile, new IInstallableUnit[] {s1, t1}, true, planner, createEngine()));
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(s2);
		pcr.remove(s1);
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertNotOK(plan.getStatus());
	}

	/*
	 * Install Platform as root.
	 * Add SDK.
	 * Try to update SDK.
	 * Operation blocked because Platform is root and not automatically updated.
	 */
	public void testSDKandPlatformBothRootsConflict() {
		assertOK(install(profile, new IInstallableUnit[] {platform1, s1}, true, planner, createEngine()));
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(s2);
		pcr.remove(s1);
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertNotOK(plan.getStatus());
	}

	/*
	 * Install Platform and SDK both as roots.
	 * Try and update Platform.
	 * Blocked because SDK needs Platform.
	 */
	public void testSDKandPlatformBothRootsConflict2() {
		assertOK(install(profile, new IInstallableUnit[] {platform1, s1}, true, planner, createEngine()));
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(platform2);
		pcr.remove(platform1);
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertNotOK(plan.getStatus());
	}

	public void test1() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {x});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertEquals(1, queryResultSize(((PlannerStatus) plan.getStatus()).getPlannedState().query(QueryUtil.createIUQuery("X"), null)));
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());

	}

	public void test2() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {y});
		assertEquals(IStatus.ERROR, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void testExplanation2() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {y});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		final RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		Set explanation = requestStatus.getExplanations();
		// System.out.println(explanation);
		assertFalse(explanation.isEmpty());
		assertEquals(Explanation.VIOLATED_SINGLETON_CONSTRAINT, requestStatus.getShortExplanation());
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(y));
		assertEquals(1, requestStatus.getConflictsWithInstalledRoots().size());

	}

	public void test3() {
		//Test that we can install A3 and A4 together
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {z});
		assertEquals(IStatus.OK, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void test4() {
		//Ensure that A1 and A3 can't be installed together since one is singleton and the other not
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {w});
		assertEquals(IStatus.ERROR, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void testExplanation4() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {w});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		final RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		Set explanation = requestStatus.getExplanations();
		// System.out.println(explanation);
		assertFalse(explanation.isEmpty());
		assertEquals(Explanation.VIOLATED_SINGLETON_CONSTRAINT, requestStatus.getShortExplanation());
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(w));

	}

	public void test5b() {
		//Validate the setup
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {u});
		assertEquals(IStatus.OK, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void test5c() {
		//Validate the setup
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {v});
		assertEquals(IStatus.OK, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void test5() {
		//Ensure
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {u, v});
		assertEquals(IStatus.ERROR, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}

	public void testExplanation5() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {u, v});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.ERROR, plan.getStatus().getSeverity());
		final RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		Set explanation = requestStatus.getExplanations();
		assertFalse(explanation.isEmpty());
		assertEquals(Explanation.VIOLATED_SINGLETON_CONSTRAINT, requestStatus.getShortExplanation());
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(u));
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(v));
		// System.out.println(explanation);

	}
}
