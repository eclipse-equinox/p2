/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.p2.engine.IEngine;

import org.eclipse.equinox.p2.engine.IProvisioningPlan;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class AllOrbit extends AbstractProvisioningTest {
	IProfile profile1;
	IPlanner planner;
	IEngine engine;
	IMetadataRepository repo;

	protected void setUp() throws Exception {
		super.setUp();
		IMetadataRepositoryManager repoMan = (MetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		repo = repoMan.loadRepository(getTestData("repository for wsdl test", "testData/orbitRepo/").toURI(), new NullProgressMonitor());

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstallTwoVersionsOptionaly() {
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		Collector allIUs = repo.query(InstallableUnitQuery.ANY, new Collector(), null);
		req1.addInstallableUnits((IInstallableUnit[]) allIUs.toArray(IInstallableUnit.class));
		for (Iterator iterator = allIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			if (!iu.getId().equals("javax.wsdl"))
				req1.setInstallableUnitInclusionRules(iu, PlannerHelper.createOptionalInclusionRule(iu));
		}
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		Operand[] ops = plan1.getOperands();
		int count = 0;
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitOperand) {
				count++;
			}
		}
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
	}

	public void test2() {
		//Install everything except com.ibm.icu
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		Collector allIUs = repo.query(InstallableUnitQuery.ANY, new Collector(), null);
		ArrayList toInstall = new ArrayList(allIUs.size());
		int removed = 0;
		for (Iterator iterator = allIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit toAdd = (IInstallableUnit) iterator.next();
			if (!toAdd.getId().equals("com.ibm.icu")) {
				toInstall.add(toAdd);
			} else
				removed++;
		}
		req1.addInstallableUnits((IInstallableUnit[]) toInstall.toArray(new IInstallableUnit[toInstall.size()]));

		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		Operand[] ops = plan1.getOperands();
		int count = 0;
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitOperand) {
				count++;
			}
		}
		assertEquals(178, count);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
	}

	public void test3() {
		//Install everything optionaly (except com.ibm.icu that we don't install at all)
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		Collector allIUs = repo.query(InstallableUnitQuery.ANY, new Collector(), null);
		ArrayList toInstall = new ArrayList(allIUs.size());
		int removed = 0;
		for (Iterator iterator = allIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit toAdd = (IInstallableUnit) iterator.next();
			if (!toAdd.getId().equals("com.ibm.icu")) {
				toInstall.add(toAdd);
				req1.setInstallableUnitInclusionRules(toAdd, PlannerHelper.createOptionalInclusionRule(toAdd));
			} else
				removed++;
		}
		req1.addInstallableUnits((IInstallableUnit[]) toInstall.toArray(new IInstallableUnit[toInstall.size()]));

		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		Operand[] ops = plan1.getOperands();
		int count = 0;
		for (int i = 0; i < ops.length; i++) {
			if (ops[i] instanceof InstallableUnitOperand) {
				count++;
			}
		}
		assertEquals(178, count);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
	}
}
