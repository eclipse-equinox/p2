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
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
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
		ProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
		assertOK("Plan OK", plan.getStatus());
		assertEquals(0, plan.getAdditions().query(new InstallableUnitQuery("org.eclipse.tptp.platform.agentcontroller"), new Collector(), null).size());
		assertEquals(0, plan.getAdditions().query(new InstallableUnitQuery("org.eclipse.tptp.platform.iac.administrator"), new Collector(), null).size());
	}

}
