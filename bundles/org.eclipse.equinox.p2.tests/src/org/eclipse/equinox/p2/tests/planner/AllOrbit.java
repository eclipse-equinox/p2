/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class AllOrbit extends AbstractProvisioningTest {
	IProfile profile1;
	IPlanner planner;
	IEngine engine;
	IMetadataRepository repo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IMetadataRepositoryManager repoMan = getAgent().getService(IMetadataRepositoryManager.class);
		repo = repoMan.loadRepository(getTestData("repository for wsdl test", "testData/orbitRepo/").toURI(), new NullProgressMonitor());

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstallTwoVersionsOptionaly() {
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		IQueryResult<IInstallableUnit> allIUs = repo.query(QueryUtil.createIUAnyQuery(), null);
		req1.addInstallableUnits(allIUs.toArray(IInstallableUnit.class));
		for (Iterator<IInstallableUnit> iterator = allIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			if (!iu.getId().equals("javax.wsdl"))
				req1.setInstallableUnitInclusionRules(iu, ProfileInclusionRules.createOptionalInclusionRule(iu));
		}
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
	}

	public void test2() {
		//Install everything except com.ibm.icu
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		IQueryResult<IInstallableUnit> allIUs = repo.query(QueryUtil.createIUAnyQuery(), null);
		ArrayList<IInstallableUnit> toInstall = new ArrayList<>();
		for (Iterator<IInstallableUnit> iterator = allIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit toAdd = iterator.next();
			if (!toAdd.getId().equals("com.ibm.icu")) {
				toInstall.add(toAdd);
			}
		}
		req1.addInstallableUnits(toInstall.toArray(new IInstallableUnit[toInstall.size()]));

		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(178, countPlanElements(plan1));
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
	}

	public void test3() {
		//Install everything optionaly (except com.ibm.icu that we don't install at all)
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		IQueryResult<IInstallableUnit> allIUs = repo.query(QueryUtil.createIUAnyQuery(), null);
		ArrayList<IInstallableUnit> toInstall = new ArrayList<>();
		for (Iterator<IInstallableUnit> iterator = allIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit toAdd = iterator.next();
			if (!toAdd.getId().equals("com.ibm.icu")) {
				toInstall.add(toAdd);
				req1.setInstallableUnitInclusionRules(toAdd, ProfileInclusionRules.createOptionalInclusionRule(toAdd));
			}
		}
		req1.addInstallableUnits(toInstall.toArray(new IInstallableUnit[toInstall.size()]));

		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(178, countPlanElements(plan1));
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
	}
}
