/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class UpdateTest extends AbstractProvisioningTest {
	IInstallableUnit f1;
	IInstallableUnit f1_1;
	IInstallableUnit f1_4;

	IInstallableUnit fa;
	IInstallableUnit fap;
	IDirector director;
	IPlanner planner;
	IProfile profile;

	protected void setUp() throws Exception {
		String f1Id = getName() + "f1";
		f1 = createIU(f1Id, DEFAULT_VERSION, true);
		f1_1 = createIU(f1Id, Version.createOSGi(1, 1, 0), true);
		f1_4 = createIU(f1Id, Version.createOSGi(1, 4, 0), true);

		IRequirement[] requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, f1Id, new VersionRange("[1.0.0, 1.3.0)"));
		String faId = getName() + ".fa";
		fa = createIU(faId, requires, false);

		requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, f1Id, new VersionRange("[1.0.0, 1.4.0)"));
		fap = createIU(faId, Version.createOSGi(1, 1, 0), requires, NO_PROPERTIES, false);

		createTestMetdataRepository(new IInstallableUnit[] {f1, fa});

		profile = createProfile("UpdateTest." + getName());
		director = createDirector();
		planner = createPlanner();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {fa});
		assertOK("1.0", director.provision(request, null, null));
		assertProfileContains("Profile setup", profile, new IInstallableUnit[] {f1, fa});
		createTestMetdataRepository(new IInstallableUnit[] {f1_1, f1_4});
	}

	public void testInstall() {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {f1_1});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, new ProvisioningContext(getAgent()), new NullProgressMonitor());
		assertOK("1.0", plan.getStatus());
		assertOK("1.1", director.provision(request, null, null));
		request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {f1_4});
		assertEquals(IStatus.ERROR, director.provision(request, null, new NullProgressMonitor()).getSeverity());
	}
}
