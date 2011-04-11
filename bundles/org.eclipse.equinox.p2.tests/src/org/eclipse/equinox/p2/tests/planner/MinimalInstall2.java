/*******************************************************************************
 *  Copyright (c) 2011 Sonatype, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MinimalInstall2 extends AbstractProvisioningTest {
	IInstallableUnit kernelRoot;
	IInstallableUnit kernelBundle;

	IInstallableUnit userRegionRoot;
	IInstallableUnit userRegionBundle;

	IInstallableUnit userRegionRoot2;
	IInstallableUnit kernelProxy;

	IInstallableUnit commonDep;

	IProfile profile;
	IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();

		kernelRoot = createIU("KernelRoot", Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "KernelBundle", new VersionRange("[1.0.0, 2.0.0)")));
		kernelBundle = createIU("KernelBundle", Version.create("1.0.0"), new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "KernelBundle", Version.create("1.0.0")), MetadataFactory.createProvidedCapability("java.package", "kernel.package", Version.create("1.0.0"))});

		userRegionRoot = createIU("UserRegionRoot", Version.create("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "UserRegionBundle", new VersionRange("[1.0.0, 2.0.0)")));
		userRegionBundle = createIU("UserRegionBundle", Version.create("1.0.0"), createRequiredCapabilities("java.package", "kernel.package"));

		IRequirement[] reqs = new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "UserRegionBundle", new VersionRange("[1.0.0, 2.0.0)"), null, false, false, true), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "KernelProxy", new VersionRange("[1.0.0, 2.0.0)"), null, false, false, true)};
		userRegionRoot2 = createIU("UserRegionRoot2", reqs);
		kernelProxy = createIU("KernelProxy", Version.create("1.0.0"), new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "KernelProxy", Version.create("1.0.0")), MetadataFactory.createProvidedCapability("java.package", "kernel.package", Version.create("1.0.0"))});

		createTestMetdataRepository(new IInstallableUnit[] {kernelRoot, kernelBundle, userRegionRoot, userRegionBundle, userRegionRoot2, kernelProxy});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallUserRegion() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {userRegionRoot});
		IProvisioningPlan pplan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, pplan.getStatus().getSeverity());
		assertEquals(3, pplan.getAdditions().query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toSet().size());
	}

	public void testInstallUserRegion2() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {userRegionRoot2});
		IProvisioningPlan pplan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, pplan.getStatus().getSeverity());
		assertEquals(3, pplan.getAdditions().query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).toSet().size());
		assertFalse(pplan.getAdditions().query(QueryUtil.createIUQuery(userRegionRoot2.getId()), new NullProgressMonitor()).isEmpty());
		assertFalse(pplan.getAdditions().query(QueryUtil.createIUQuery(kernelProxy.getId()), new NullProgressMonitor()).isEmpty());
		assertFalse(pplan.getAdditions().query(QueryUtil.createIUQuery(userRegionBundle.getId()), new NullProgressMonitor()).isEmpty());
	}
}
