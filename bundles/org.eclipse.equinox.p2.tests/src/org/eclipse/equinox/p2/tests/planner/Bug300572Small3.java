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

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

//This test verify that one patch can replace another one.
//In this test we use the metadata from the original bug 300572 and simulate the optional addition of the patches
public class Bug300572Small3 extends AbstractProvisioningTest {
	IInstallableUnit featureBeingPatched;
	IInstallableUnitPatch p1, p2;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;
	private IInstallableUnit p1b;
	private IInstallableUnit p2b;

	//	private IInstallableUnit p2c;

	protected void setUp() throws Exception {
		super.setUp();
		IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(getTestData("bug300572 data", "testData/bug300572Small/repo/").toURI(), new NullProgressMonitor());
		featureBeingPatched = repo.query(QueryUtil.createIUQuery("hellofeature.feature.group"), null).iterator().next();

		//All the IUs for the patch
		p1 = (IInstallableUnitPatch) repo.query(QueryUtil.createIUQuery("hellopatch.feature.group", Version.create("1.0.0")), null).iterator().next();
		p1b = repo.query(QueryUtil.createIUQuery("hello", Version.create("1.0.1.200911201237")), null).iterator().next();
		//		p1c = repo.query(QueryUtil.createIUQuery("hellopatch.feature.jar", Version.create("1.0.0")), null).iterator().next();

		p2 = (IInstallableUnitPatch) repo.query(QueryUtil.createIUQuery("hellopatch.feature.group", Version.create("1.0.2.201001211536")), null).iterator().next();
		p2b = repo.query(QueryUtil.createIUQuery("hello", Version.create("1.0.2.201001211536")), null).iterator().next();
		//		p2c = repo.query(QueryUtil.createIUQuery("hellopatch.feature.jar", Version.create("1.0.2.201001211536")), null).iterator().next();

		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstallAandP1ThenP2() {
		profile1 = createProfile("TestProfile." + getName());
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {featureBeingPatched, p1, p1b});
		req1.setInstallableUnitInclusionRules(p1, ProfileInclusionRules.createOptionalInclusionRule(p1));
		req1.setInstallableUnitInclusionRules(p1b, ProfileInclusionRules.createOptionalInclusionRule(p1b));

		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
		assertContains(plan1.getAdditions().query(QueryUtil.ALL_UNITS, null), p1);
		assertOK("plan execution", engine.perform(plan1, null));
		assertProfileContainsAll("Patch 1 not installed", profile1, new IInstallableUnit[] {p1, p1b});

		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {p2, p2b});
		req2.setInstallableUnitInclusionRules(p2, ProfileInclusionRules.createOptionalInclusionRule(p2));
		req2.setInstallableUnitInclusionRules(p2, ProfileInclusionRules.createOptionalInclusionRule(p2b));
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, null);
		assertOK("Planning for installing P2", plan2.getStatus());
		assertContains(plan2.getAdditions().query(QueryUtil.ALL_UNITS, null), p2);
		assertFalse(plan2.getAdditions().query(QueryUtil.createIUQuery(p2b), null).isEmpty());
		assertFalse(plan2.getAdditions().query(QueryUtil.createIUQuery(p2), null).isEmpty());
		assertFalse(plan2.getRemovals().query(QueryUtil.createIUQuery(p1), null).isEmpty());
		assertFalse(plan2.getRemovals().query(QueryUtil.createIUQuery(p1b), null).isEmpty());
	}

}
