/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TopLevelFilterTest extends AbstractProvisioningTest {

	public void testInstallIUWithFilter() {
		InstallableUnitDescription desc = new InstallableUnitDescription();
		desc.setId("ThingWithFilter");
		desc.setFilter("(os=linux)");
		desc.setVersion(Version.createOSGi(1, 0, 0));

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(desc);
		IProfile p = createProfile(getClass().getName());
		createTestMetdataRepository(new IInstallableUnit[] {iu});
		ProfileChangeRequest req = new ProfileChangeRequest(p);
		req.addInstallableUnits(new IInstallableUnit[] {iu});
		IProvisioningPlan plan = createPlanner().getProvisioningPlan(req, null, null);
		assertNotOK(plan.getStatus());
		assertTrue(plan.getStatus().getChildren()[0].getMessage().contains("filter"));
	}
}
