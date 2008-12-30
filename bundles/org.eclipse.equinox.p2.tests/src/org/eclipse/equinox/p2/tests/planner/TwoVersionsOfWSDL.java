/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IEngine;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class TwoVersionsOfWSDL extends AbstractProvisioningTest {
	IInstallableUnit wsdl14;
	IInstallableUnit wsdl15;
	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		IMetadataRepositoryManager repoMan = (MetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		IMetadataRepository repo = repoMan.loadRepository(getTestData("repository for wsdl test", "testData/metadataRepo/wsdlTestRepo/").toURI(), new NullProgressMonitor());
		wsdl15 = (IInstallableUnit) repo.query(new InstallableUnitQuery("javax.wsdl", new VersionRange("[1.5, 1.6)")), new Collector(), null).iterator().next();
		wsdl14 = (IInstallableUnit) repo.query(new InstallableUnitQuery("javax.wsdl", new VersionRange("[1.4, 1.5)")), new Collector(), null).iterator().next();

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstallTwoVersionsOptionaly() {
		//Ensure that p1 causes a1 to resolve
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {wsdl15, wsdl14});
		req1.setInstallableUnitInclusionRules(wsdl15, PlannerHelper.createStrictInclusionRule(wsdl15));
		req1.setInstallableUnitInclusionRules(wsdl14, PlannerHelper.createStrictInclusionRule(wsdl14));
		ProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
		assertInstallOperand(plan1, wsdl15);
		assertInstallOperand(plan1, wsdl14);
	}
}
