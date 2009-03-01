/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ExplanationLargeConflict extends AbstractProvisioningTest {
	IMetadataRepository repo1;
	IMetadataRepository repo2;
	private IPlanner planner;
	private IProfile SDKprofile;

	protected void setUp() throws Exception {
		super.setUp();
		repo1 = loadMetadataRepository(getTestData("repo1", "testData/testLargeConflict/repo1").toURI());
		repo2 = loadMetadataRepository(getTestData("repo2", "testData/testLargeConflict/repo2").toURI());

		File reporegistry1 = getTestData("test data explanation large conflict", "testData/testLargeConflict/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		SimpleProfileRegistry registry = new SimpleProfileRegistry(tempFolder, null, false);
		SDKprofile = registry.getProfile("SDKProfile");
		assertNotNull(SDKprofile);

		planner = createPlanner();
	}

	public void testIndependentConflict() {
		long sTime = System.currentTimeMillis();
		//Here we verify that two version of JDT can't be installed together. The SDKProfile is not used
		IProfile profile = createProfile("TestProfile." + getName());
		Collector c = repo1.query(new InstallableUnitQuery("org.eclipse.jdt.feature.group"), new Collector(), null);
		assertEquals(1, c.size());
		IInstallableUnit jdt1 = (IInstallableUnit) c.iterator().next();

		Collector c2 = repo2.query(new InstallableUnitQuery("org.eclipse.jdt.feature.group"), new Collector(), null);
		assertEquals(1, c2.size());
		IInstallableUnit jdt2 = (IInstallableUnit) c2.iterator().next();

		assertNotSame(jdt1, jdt2);

		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.addInstallableUnits(new IInstallableUnit[] {jdt1, jdt2});
		ProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertNotOK(plan.getStatus());
		System.out.println(plan.getRequestStatus().getExplanations());
		long eTime = System.currentTimeMillis();
		System.out.println("Time to compute conflict: " + (eTime - sTime));
	}

	public void testMissingRequirement() {
		long sTime = System.currentTimeMillis();
		//Test the case of a missing requirement in the IU being installed
		IRequiredCapability[] cap = createRequiredCapabilities("missing", "missing", new VersionRange("[0.0.0, 1.0.0)"), null);
		IInstallableUnit otherIU2 = createIU("foo", new Version("0.9.0"), null, cap, BUNDLE_CAPABILITY, NO_PROPERTIES, TOUCHPOINT_OSGI, NO_TP_DATA, true);
		ProfileChangeRequest pcr3 = new ProfileChangeRequest(SDKprofile);
		pcr3.addInstallableUnits(new IInstallableUnit[] {otherIU2});
		ProvisioningPlan plan3 = planner.getProvisioningPlan(pcr3, null, null);
		assertNotOK(plan3.getStatus());
		System.out.println(plan3.getRequestStatus().getExplanations());
		long eTime = System.currentTimeMillis();
		System.out.println("Time to compute conflict: " + (eTime - sTime));
	}

	public void testSingletonConflict() {
		long sTime = System.currentTimeMillis();
		//The IU being installed conflict with something already installed because of a singleton
		IInstallableUnit otherIU = createIU("org.eclipse.equinox.p2.director", new Version("0.9.0"), null, NO_REQUIRES, BUNDLE_CAPABILITY, NO_PROPERTIES, TOUCHPOINT_OSGI, NO_TP_DATA, true);
		ProfileChangeRequest pcr2 = new ProfileChangeRequest(SDKprofile);
		pcr2.addInstallableUnits(new IInstallableUnit[] {otherIU});
		ProvisioningPlan plan2 = planner.getProvisioningPlan(pcr2, null, null);
		assertNotOK(plan2.getStatus());
		System.out.println(plan2.getRequestStatus().getExplanations());
		long eTime = System.currentTimeMillis();
		System.out.println("Time to compute conflict: " + (eTime - sTime));
	}

	public void testExplanationLargeConflictInSDK() {
		long sTime = System.currentTimeMillis();
		//Test large conflict. We are trying to install an inappropriate version of CVS over the already installed SDK
		Collector c = repo2.query(new InstallableUnitQuery("org.eclipse.cvs.feature.group"), new Collector(), null);
		assertEquals(1, c.size());
		IInstallableUnit cvs = (IInstallableUnit) c.iterator().next();

		ProfileChangeRequest pcr = new ProfileChangeRequest(SDKprofile);
		pcr.addInstallableUnits(new IInstallableUnit[] {cvs});
		ProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertNotOK(plan.getStatus());
		System.out.println(plan.getRequestStatus().getExplanations());
		long eTime = System.currentTimeMillis();
		System.out.println("Time to compute conflict: " + (eTime - sTime));
	}
}
