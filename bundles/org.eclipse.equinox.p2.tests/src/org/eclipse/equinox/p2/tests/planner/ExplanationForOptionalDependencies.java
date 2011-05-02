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

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class ExplanationForOptionalDependencies extends AbstractProvisioningTest {
	private IProfile profile;
	private IPlanner planner;
	private IEngine engine;
	private IInstallableUnit sdk;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		sdk = createIU("SDK", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "SDKPart", new VersionRange("[1.0.0, 1.0.0]")));
		IInstallableUnit sdkPart = createIU("SDKPart", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), true);
		IInstallableUnit sdkPart2 = createIU("SDKPart", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("2.0.0")), true);

		createTestMetdataRepository(new IInstallableUnit[] {sdk, sdkPart, sdkPart2});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {sdk});
		engine.perform(planner.getProvisioningPlan(pcr, null, null), null);

	}

	public void testNoProblemWithMissingOptionalDependency() {
		//CDT will be missing a requirement but it is optional so everything should be good
		//EMF will be not be good because it is missing a requirement
		IRequirement missingOptionalDependency = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "MissingSomething", new VersionRange("[1.0.0, 1.0.0]"), null, true, false);
		IInstallableUnit cdt = createIU("CDT", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), new IRequirement[] {missingOptionalDependency});

		IRequirement emfMissing = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "EMFPart", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IInstallableUnit emf = createIU("EMF", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), new IRequirement[] {emfMissing}, NO_PROPERTIES, true);

		createTestMetdataRepository(new IInstallableUnit[] {cdt, emf});
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cdt, emf});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(pcr, null, null);
		RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(emf));
		assertFalse(requestStatus.getConflictsWithInstalledRoots().contains(cdt));
		assertFalse(requestStatus.getConflictsWithInstalledRoots().contains(sdk));

		//		assertTrue(plan.getRequestStatus(cdt).getSeverity() != IStatus.ERROR);
		//
		//		assertTrue(plan.getRequestStatus(emf).getSeverity() == IStatus.ERROR);
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithInstalledRoots());
	}
}
