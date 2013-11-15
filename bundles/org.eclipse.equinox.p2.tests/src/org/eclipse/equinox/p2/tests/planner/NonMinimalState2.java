/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/*
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=276133 for details.
 */
public class NonMinimalState2 extends AbstractProvisioningTest {
	IProfile profile = null;
	IMetadataRepository repo = null;

	protected void setUp() throws Exception {
		getMetadataRepositoryManager().addRepository(getTestData("Test repo (copy of Galileo M7)", "testData/galileoM7/").toURI());
		profile = createProfile(NonMinimalState2.class.getName());
	}

	public void testInstallJetty() {
		IPlanner planner = createPlanner();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(getMetadataRepositoryManager().query(QueryUtil.createIUQuery("org.mortbay.jetty.server"), null).toArray(IInstallableUnit.class));
		IProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
		assertOK("Plan OK", plan.getStatus());

		assertTrue(plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.tptp.platform.agentcontroller"), null).isEmpty());
		assertTrue(plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.tptp.platform.iac.administrator"), null).isEmpty());
	}

	//	public void testp2Source() {
	//		IPlanner planner = createPlanner();
	//		IInstallableUnit iu = createIU("aRoot", new IRequiredCapability[] {MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.iu", "org.eclipse.equinox.p2.user.ui.source.feature.group", VersionRange.emptyRange, null, false, false)});
	//		ProfileChangeRequest request = new ProfileChangeRequest(profile);
	//		request.addInstallableUnits(new IInstallableUnit[] {iu});
	//		ProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
	//		assertOK("Plan OK", plan.getStatus());
	//	}
	//
	public void testWithTwoTPTP() {
		IRequirement cap1 = MetadataFactory.createRequirement("org.eclipse.equinox.p2.iu", "org.eclipse.tptp.platform.agentcontroller", VersionRange.emptyRange, null, false, false);
		IRequirement cap2 = MetadataFactory.createRequirement("org.eclipse.equinox.p2.iu", "org.eclipse.tptp.platform.iac.administrator", VersionRange.emptyRange, null, false, false);
		IInstallableUnit iu = createEclipseIU("org.eclipse.hyades.execution", Version.createOSGi(1, 0, 0), new IRequirement[] {cap1, cap2}, null);
		createTestMetdataRepository(new IInstallableUnit[] {iu});
		IPlanner planner = createPlanner();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(getMetadataRepositoryManager().query(QueryUtil.createIUQuery("org.mortbay.jetty.server"), null).toArray(IInstallableUnit.class));
		IProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
		assertOK("Plan OK", plan.getStatus());

		assertTrue(plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.tptp.platform.agentcontroller"), null).isEmpty());
		assertTrue(plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.tptp.platform.iac.administrator"), null).isEmpty());

	}
}
