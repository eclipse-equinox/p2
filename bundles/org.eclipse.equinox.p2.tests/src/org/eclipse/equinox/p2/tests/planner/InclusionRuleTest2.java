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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class InclusionRuleTest2 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a2;
	IProfile profile;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), true);

		createIU("A", Version.create("2.0.0"), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, ITouchpointType.NONE, NO_TP_DATA, true, MetadataFactory.createUpdateDescriptor("A", VersionRange.emptyRange, 0, "foo bar"), null);
		a2 = createIU("A", Version.create("2.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, a2});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testChangeIUProperty() {
		//Add into the profile the version a1;
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		req.setInstallableUnitProfileProperty(a1, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		engine.perform(plan, null);
		assertProfileContainsAll("A1 is missing", profile, new IInstallableUnit[] {a1});

		IProfileRegistry profileRegistry = getProfileRegistry();
		profile = profileRegistry.getProfile(profile.getProfileId());
		IQueryResult c = profile.query(new UserVisibleRootQuery(), null);
		assertEquals(queryResultSize(c), 1);

		System.gc();
		ProfileChangeRequest req2 = ProfileChangeRequest.createByProfileId(getAgent(), profile.getProfileId());
		req2.removeInstallableUnits(new IInstallableUnit[] {a1});
		req2.addInstallableUnits(new IInstallableUnit[] {a2});
		//		req2.setInstallableUnitProfileProperty(a2, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		assertInstallOperand(plan2, a2);
		engine.perform(plan2, null);
		profile = getProfile(profile.getProfileId());
		assertProfileContains("A2 is missing", profile, new IInstallableUnit[] {a2});
	}
}
