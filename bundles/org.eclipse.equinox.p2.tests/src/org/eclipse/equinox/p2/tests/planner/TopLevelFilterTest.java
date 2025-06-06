/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
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

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TopLevelFilterTest extends AbstractProvisioningTest {

	public void testInstallIUWithFilter() {
		IInstallableUnit iu = createIU("ThingWithFilter", Version.create("1.0.0"), "(os=linux)", new IProvidedCapability[] {new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "A", Version.create("1.0.0"))});
		IProfile p = createProfile(getClass().getName());
		createTestMetdataRepository(new IInstallableUnit[] {iu});
		ProfileChangeRequest req = new ProfileChangeRequest(p);
		req.addInstallableUnits(iu);
		IProvisioningPlan plan = createPlanner().getProvisioningPlan(req, null, null);
		assertNotOK(plan.getStatus());
		assertTrue("Explanation does not mention filter!", plan.getStatus().getChildren()[0].getMessage().contains("filter"));
	}
}
