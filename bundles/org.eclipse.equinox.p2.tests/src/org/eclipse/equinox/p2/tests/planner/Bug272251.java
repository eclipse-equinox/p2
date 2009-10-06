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

import org.eclipse.equinox.internal.provisional.p2.core.Version;

import java.io.File;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug272251 extends AbstractProvisioningTest {
	IProfile profile = null;
	IMetadataRepository repo = null;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 272251", "testData/bug272251/profileRegistry/");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		SimpleProfileRegistry registry = new SimpleProfileRegistry(tempFolder, null, false);
		profile = registry.getProfile("PlatformProfile");
		assertNotNull(profile);
		repo = getMetadataRepositoryManager().loadRepository(getTestData("test data bug 272251", "testData/bug272251/repo").toURI(), null);
		assertNotNull(repo);
	}

	protected void tearDown() throws Exception {
		getMetadataRepositoryManager().removeRepository(getTestData("test data bug 272251", "testData/bug272251/repo").toURI());
		super.tearDown();
	}

	public void testInstallFeaturePatch() {
		Collector c = repo.query(new InstallableUnitQuery("org.eclipse.wst.jsdt.feature.patch.feature.group", new Version("3.0.4.v200904020304-1-8d7w311_15131415")), new Collector(), new NullProgressMonitor());
		assertEquals(1, c.size());
		Collector expectedIU = repo.query(new InstallableUnitQuery("org.eclipse.wst.jsdt.web.ui", new Version("1.0.105.v200904020304")), new Collector(), new NullProgressMonitor());
		assertEquals(1, expectedIU.size());
		IInstallableUnit patch = (IInstallableUnit) c.iterator().next();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {patch});
		request.setInstallableUnitInclusionRules(patch, PlannerHelper.createStrictInclusionRule(patch));
		IPlanner planner = createPlanner();
		ProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
		assertOK("Plan OK", plan.getStatus());
		assertNoOperand(plan, patch);
		assertNoOperand(plan, (IInstallableUnit) expectedIU.iterator().next());
	}
}
