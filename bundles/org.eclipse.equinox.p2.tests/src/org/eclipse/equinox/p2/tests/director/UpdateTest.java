/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class UpdateTest extends AbstractProvisioningTest {
	IInstallableUnit f1;
	IInstallableUnit f1_1;
	IInstallableUnit f1_4;

	IInstallableUnit fa;
	IInstallableUnit fap;
	IDirector director;
	IPlanner planner;
	Profile profile;

	protected void setUp() throws Exception {
		String f1Id = getName() + "f1";
		f1 = createIU(f1Id, DEFAULT_VERSION, true);
		f1_1 = createIU(f1Id, new Version(1, 1, 0), true);
		f1_4 = createIU(f1Id, new Version(1, 4, 0), true);

		RequiredCapability[] requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU, f1Id, new VersionRange("[1.0.0, 1.3.0)"), null);
		String faId = getName() + ".fa";
		fa = createIU(faId, requires, false);

		requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU, f1Id, new VersionRange("[1.0.0, 1.4.0)"), null);
		fap = createIU(faId, new Version(1, 1, 0), requires, NO_PROPERTIES, false);

		createTestMetdataRepository(new IInstallableUnit[] {f1, fa});

		profile = createProfile("TestProfile." + getName());
		director = createDirector();
		planner = createPlanner();
		assertOK(director.install(new IInstallableUnit[] {fa}, profile, null, null));
		assertProfileContains("Profile setup", profile, new IInstallableUnit[] {f1, fa});
		createTestMetdataRepository(new IInstallableUnit[] {f1_1, f1_4});
	}

	public void testInstall() {
		ProvisioningPlan plan = planner.getInstallPlan(new IInstallableUnit[] {f1_1}, profile, null, new NullProgressMonitor());
		assertOK(plan.getStatus());
		assertOK(director.install(new IInstallableUnit[] {f1_1}, profile, null, new NullProgressMonitor()));
		for (Iterator iterator = getInstallableUnits(profile); iterator.hasNext();) {
			System.out.println(iterator.next());
		}
		assertEquals(IStatus.ERROR, director.install(new IInstallableUnit[] {f1_4}, profile, null, new NullProgressMonitor()).getSeverity());

		//		director.replace(new IInstallableUnit[] {fap}, profile, new NullProgressMonitor());
	}
}
