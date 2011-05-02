/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TwoVersionsOfWSDL extends AbstractProvisioningTest {
	IInstallableUnit wsdl14;
	IInstallableUnit wsdl15;
	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		IMetadataRepository repo = repoMan.loadRepository(getTestData("repository for wsdl test", "testData/metadataRepo/wsdlTestRepo/").toURI(), new NullProgressMonitor());
		wsdl15 = repo.query(QueryUtil.createIUQuery("javax.wsdl", new VersionRange("[1.5, 1.6)")), null).iterator().next();
		wsdl14 = repo.query(QueryUtil.createIUQuery("javax.wsdl", new VersionRange("[1.4, 1.5)")), null).iterator().next();

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstallTwoVersionsOptionaly() {
		//Ensure that p1 causes a1 to resolve
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {wsdl15, wsdl14});
		req1.setInstallableUnitInclusionRules(wsdl15, ProfileInclusionRules.createStrictInclusionRule(wsdl15));
		req1.setInstallableUnitInclusionRules(wsdl14, ProfileInclusionRules.createStrictInclusionRule(wsdl14));
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
		assertInstallOperand(plan1, wsdl15);
		assertInstallOperand(plan1, wsdl14);
	}
}
