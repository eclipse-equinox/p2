/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class DependencyOnSelf extends AbstractProvisioningTest {
	IInstallableUnit a1;

	IPlanner planner;
	IProfile profile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[1.0.0, 1.0.0]")));
		createTestMetdataRepository(new IInstallableUnit[] {a1});
		profile = createProfile(DependencyOnSelf.class.getName());
		planner = createPlanner();
	}

	public void testInstall() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(a1);
		assertEquals(IStatus.OK, planner.getProvisioningPlan(req, null, null).getStatus().getSeverity());
	}
}
