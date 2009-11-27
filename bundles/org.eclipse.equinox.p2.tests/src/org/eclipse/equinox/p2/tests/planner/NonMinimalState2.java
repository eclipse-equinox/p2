/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

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
		request.addInstallableUnits((IInstallableUnit[]) getMetadataRepositoryManager().query(new InstallableUnitQuery("org.mortbay.jetty.server"), new Collector(), null).toArray(IInstallableUnit.class));
		IProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
		assertOK("Plan OK", plan.getStatus());

		assertEquals(0, plan.getAdditions().query(new InstallableUnitQuery("org.eclipse.tptp.platform.agentcontroller"), new Collector(), null).size());
		assertEquals(0, plan.getAdditions().query(new InstallableUnitQuery("org.eclipse.tptp.platform.iac.administrator"), new Collector(), null).size());
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
		IRequiredCapability cap1 = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.iu", "org.eclipse.tptp.platform.agentcontroller", VersionRange.emptyRange, null, false, false);
		IRequiredCapability cap2 = MetadataFactory.createRequiredCapability("org.eclipse.equinox.p2.iu", "org.eclipse.tptp.platform.iac.administrator", VersionRange.emptyRange, null, false, false);
		IInstallableUnit iu = createEclipseIU("org.eclipse.hyades.execution", Version.createOSGi(1, 0, 0), new IRequiredCapability[] {cap1, cap2}, null);
		createTestMetdataRepository(new IInstallableUnit[] {iu});
		IPlanner planner = createPlanner();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits((IInstallableUnit[]) getMetadataRepositoryManager().query(new InstallableUnitQuery("org.mortbay.jetty.server"), new Collector(), null).toArray(IInstallableUnit.class));
		IProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
		assertOK("Plan OK", plan.getStatus());

		assertEquals(0, plan.getAdditions().query(new InstallableUnitQuery("org.eclipse.tptp.platform.agentcontroller"), new Collector(), null).size());
		assertEquals(0, plan.getAdditions().query(new InstallableUnitQuery("org.eclipse.tptp.platform.iac.administrator"), new Collector(), null).size());

	}
}
