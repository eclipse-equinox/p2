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
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class OracleTest extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a2;
	IInstallableUnit b1;
	IInstallableUnit c1;
	IInstallableUnit d1;
	IInstallableUnit d2;

	IDirector director;
	IProfile profile;

	protected void setUp() throws Exception {
		IRequirement[] requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[1.0.0, 2.0.0)"));
		a1 = createIU("A", requires, true);

		requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "D", new VersionRange("[1.0.0, 3.0.0)"));
		c1 = createIU("C", requires, true);

		d1 = createIU("D", DEFAULT_VERSION, true);

		requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "D", new VersionRange("[2.0.0, 3.0.0)"));
		b1 = createIU("B", requires, true);

		d2 = createIU("D", Version.createOSGi(2, 0, 0), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, c1, d1, b1});

		profile = createProfile("TestProfile." + getName());
		director = createDirector();

	}

	public void testInstallA1() {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a1});
		assertEquals(IStatus.OK, director.provision(request, null, null).getSeverity());

		createTestMetdataRepository(new IInstallableUnit[] {d2});
		//		assertEquals(new Oracle().canInstall(new IInstallableUnit[] {b1}, profile, null), true);
		request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {b1});
		assertEquals(IStatus.OK, director.provision(request, null, null).getSeverity());
	}
}
