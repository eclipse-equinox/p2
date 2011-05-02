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

public class ExplanationDeepConflict extends AbstractProvisioningTest {
	private IProfile profile;
	private IPlanner planner;
	private IEngine engine;
	private IInstallableUnit sdk;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		sdk = createIU("SDK", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "SDKPart", new VersionRange("[1.0.0, 1.0.0]")));
		IInstallableUnit sdkPart = createIU("SDKPart", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "InnerSDKPart", new VersionRange("[1.0.0, 1.0.0]")));
		IInstallableUnit innerSdkPart = createIU("InnerSDKPart", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "InnerInnerSDKPart", new VersionRange("[1.0.0, 1.0.0]")));
		IInstallableUnit innerInnerSDKPart = createIU("InnerInnerSDKPart", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), true);

		createTestMetdataRepository(new IInstallableUnit[] {sdk, sdkPart, innerSdkPart, innerInnerSDKPart});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {sdk});
		engine.perform(planner.getProvisioningPlan(pcr, null, null), null);
		assertProfileContains("1.0", profile, new IInstallableUnit[] {sdk, sdkPart, innerSdkPart, innerInnerSDKPart});
	}

	public void testDeepSingletonConflict() {
		//CDT will have a singleton conflict with SDK
		IInstallableUnit cdt = createIU("CDT", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "CDTPart", new VersionRange("[1.0.0, 1.0.0]")));
		IInstallableUnit cdtPart = createIU("CDTPart", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("1.0.0")), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "InnerInnerSDKPart", new VersionRange("[2.0.0, 2.0.0]")));
		IInstallableUnit innerInnerSDKPart2 = createIU("InnerInnerSDKPart", PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version("2.0.0")), true);

		createTestMetdataRepository(new IInstallableUnit[] {cdt, cdtPart, innerInnerSDKPart2});
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cdt});
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(pcr, null, null);
		// System.out.println(plan.getRequestStatus().getExplanations());
		RequestStatus requestStatus = ((PlannerStatus) plan.getStatus()).getRequestStatus();
		assertTrue(requestStatus.getConflictsWithInstalledRoots().contains(cdt));
		//Here we verify that we only return the roots we asked the installation of. The SDK is installable since it is already installed
		assertFalse(requestStatus.getConflictsWithInstalledRoots().contains(sdk));
		assertTrue(requestStatus.getConflictsWithAnyRoots().contains(sdk));
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithAnyRoots().contains(sdk));
		//		assertTrue(plan.getRequestStatus(cdt).getConflictsWithInstalledRoots().contains(sdk));
	}
}
