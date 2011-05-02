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

import java.util.Set;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class ExplanationSeveralConflictingRoots extends AbstractProvisioningTest {
	private IProfile profile;
	private IPlanner planner;
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
		IEngine engine = createEngine();

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {sdk});
		engine.perform(planner.getProvisioningPlan(pcr, null, null), null);

	}

	public void testConflictingSingletonAndMissingDependency() {
		//CDT will have a singleton conflict with SDK
		//EMF will be missing a dependency
		IInstallableUnit cdt = createIU("CDT", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "SDKPart", new VersionRange("[2.0.0, 2.0.0]")));

		IInstallableUnit emf = createIU("EMF", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "EMFPart", new VersionRange("[1.0.0, 1.0.0]")));

		createTestMetdataRepository(new IInstallableUnit[] {cdt, emf});
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cdt, emf});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(pcr, null, null);
		// System.out.println(plan.getRequestStatus().getExplanations());
		Set conflictRoots = ((PlannerStatus) plan.getStatus()).getRequestStatus().getConflictsWithInstalledRoots();
		assertTrue(conflictRoots.contains(cdt) || conflictRoots.contains(emf));
		//		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(emf));

		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithInstalledRoots().contains(sdk));
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithAnyRoots().contains(sdk));
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithAnyRoots().size());
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithInstalledRoots().size());
	}

	public void testConflictingSingletonAndMissingDependency2() {
		//CDT will have a singleton conflict EMF
		//EMF will be missing a dependency and will be in conflict with CDT
		IInstallableUnit cdt = createIU("CDT", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "ASingleton", new VersionRange("[2.0.0, 2.0.0]")));
		IInstallableUnit aSingleton1 = createIU("ASingleton", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), true);
		IInstallableUnit aSingleton2 = createIU("ASingleton", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("2.0.0")), true);

		IRequirement emfOnSingleton = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "ASingleton", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IRequirement emfMissing = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "EMFPart", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IInstallableUnit emf = createIU("EMF", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), new IRequirement[] {emfOnSingleton, emfMissing});

		createTestMetdataRepository(new IInstallableUnit[] {aSingleton1, aSingleton2, cdt, emf});
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cdt, emf});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(pcr, null, null);
		// System.out.println(plan.getRequestStatus().getExplanations());
		Set conflictRoots = ((PlannerStatus) plan.getStatus()).getRequestStatus().getConflictsWithInstalledRoots();
		assertTrue(conflictRoots.contains(cdt) || conflictRoots.contains(emf));

		//		assertEquals(0, plan.getRequestStatus(cdt).getConflictsWithInstalledRoots().size());
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithAnyRoots().contains(emf));
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithInstalledRoots().size());
		//		assertTrue(plan.getRequestStatus(emf).getConflictsWithAnyRoots().contains(cdt));
	}

	public void testConflictingSingletonAndMissingDependency3() {
		//CDT will have a singleton conflict EMF and with the SDK
		//EMF will be conflicting with CDT
		IInstallableUnit cdt = createIU("CDT", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "SDKPart", new VersionRange("[2.0.0, 2.0.0]")));
		IInstallableUnit sdkPart3 = createIU("SDKPart", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("3.0.0")), true);

		IRequirement emfOnSingleton = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "SDKPart", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IRequirement emfMissing = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "EMFPart", new VersionRange("[1.0.0, 1.0.0]"), null, false, false);
		IInstallableUnit emf = createIU("EMF", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), new IRequirement[] {emfOnSingleton, emfMissing});

		createTestMetdataRepository(new IInstallableUnit[] {sdkPart3, cdt, emf});
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cdt, emf});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(pcr, null, null);
		// System.out.println(plan.getRequestStatus().getExplanations());
		Set conflictRoots = ((PlannerStatus) plan.getStatus()).getRequestStatus().getConflictsWithInstalledRoots();
		assertTrue(conflictRoots.contains(cdt) || conflictRoots.contains(emf));
		//		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(cdt));
		//		assertTrue(plan.getRequestStatus().getConflictsWithInstalledRoots().contains(emf));

		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithInstalledRoots().contains(sdk));
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithAnyRoots().contains(sdk));
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithAnyRoots().contains(emf));
		//		assertEquals(0, plan.getRequestStatus(emf).getConflictsWithInstalledRoots().size());
		//		assertTrue(plan.getRequestStatus(emf).getConflictsWithAnyRoots().contains(cdt));
	}
}
