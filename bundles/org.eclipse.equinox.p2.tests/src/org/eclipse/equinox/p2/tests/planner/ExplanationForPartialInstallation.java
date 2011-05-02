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
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class ExplanationForPartialInstallation extends AbstractProvisioningTest {
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

	public void testPartialProblemSingleton() {
		//CDT will have a singleton conflict with SDK
		//EMF will be good
		IInstallableUnit cdt = createIU("CDT", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "SDKPart", new VersionRange("[2.0.0, 2.0.0]")));

		IInstallableUnit emf = createIU("EMF", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), true);

		createTestMetdataRepository(new IInstallableUnit[] {cdt, emf});
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cdt, emf});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(pcr, null, null);
		// System.out.println(plan.getRequestStatus().getExplanations());
		RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(cdt));
		assertFalse(requestStatus.getConflictsWithInstalledRoots().contains(emf));
		assertFalse(requestStatus.getConflictsWithInstalledRoots().contains(sdk));

		//		assertTrue(plan.getRequestStatus(cdt).getSeverity() == IStatus.ERROR);
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithAnyRoots().contains(sdk));
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithInstalledRoots().contains(sdk));
		//
		//		assertTrue(plan.getRequestStatus(emf).getSeverity() != IStatus.ERROR);
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithAnyRoots().size());
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithInstalledRoots().size());
		//
		//		assertNull(plan.getRequestStatus(sdk));
	}

	public void testPartialProblemRequirement() {
		//CDT will be missing a requirement
		//EMF will be good
		IInstallableUnit cdt = createIU("CDT", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "MissingPart", new VersionRange("[2.0.0, 2.0.0]")));

		IInstallableUnit emf = createIU("EMF", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), true);

		createTestMetdataRepository(new IInstallableUnit[] {cdt, emf});
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cdt, emf});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(pcr, null, null);
		// System.out.println(plan.getRequestStatus().getExplanations());
		RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(cdt));
		assertFalse(requestStatus.getConflictsWithInstalledRoots().contains(emf));
		assertFalse(requestStatus.getConflictsWithInstalledRoots().contains(sdk));

		//		assertTrue(plan.getRequestStatus(cdt).getSeverity() == IStatus.ERROR);
		//		assertEquals(0, plan.getRequestStatus(cdt).getConflictsWithAnyRoots().size());
		//		assertEquals(0, plan.getRequestStatus(cdt).getConflictsWithInstalledRoots().size());
		//
		//		assertTrue(plan.getRequestStatus(emf).getSeverity() != IStatus.ERROR);
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithAnyRoots().size());
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithInstalledRoots().size());
	}
}
