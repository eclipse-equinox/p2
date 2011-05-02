/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MultipleProvider extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit b11;
	IInstallableUnit b12;
	IInstallableUnit b13;
	IInstallableUnit b14;

	IPlanner planner;
	IProfile profile;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B1", new VersionRange("[1.0.0, 2.0.0)")));

		b1 = createIU("B1", Version.create("1.0.0"), true);

		b11 = createIU("B1", Version.create("1.1.0"), true);

		b12 = createIU("B1", Version.create("1.2.0"), true);

		b13 = createIU("B1", Version.create("1.3.0"), true);

		b14 = createIU("B1", Version.create("1.4.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b11, b12, b13, b14});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();

	}

	public void testInstall() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		assertEquals(IStatus.OK, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}
}
