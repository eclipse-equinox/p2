/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
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

import java.io.File;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug254481dataSet2 extends AbstractProvisioningTest {
	IProfile profile = null;
	IMetadataRepository repo = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 254481", "testData/bug254481/dataSet2/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), tempFolder, null, false);
		profile = registry.getProfile("bootProfile");
		assertNotNull(profile);
		repo = getMetadataRepositoryManager().loadRepository(getTestData("test data bug 254481", "testData/bug254481/dataSet2/repo").toURI(), null);
		assertNotNull(repo);
	}

	@Override
	protected void tearDown() throws Exception {
		getMetadataRepositoryManager().removeRepository(getTestData("test data bug 254481", "testData/bug254481/dataSet2/repo").toURI());
		super.tearDown();
	}

	public void testInstallFeaturePatch() {
		IQueryResult<IInstallableUnit> c = repo.query(QueryUtil.createIUQuery("org.eclipse.jdt.feature.patch.feature.group"), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		IInstallableUnit patch = c.iterator().next();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {patch});
		request.setInstallableUnitInclusionRules(patch, ProfileInclusionRules.createOptionalInclusionRule(patch));
		IPlanner planner = createPlanner();
		IProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
		assertInstallOperand(plan, patch);
		assertEquals(1, queryResultSize(plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.jdt.core"), null)));
		assertEquals(1, queryResultSize(plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.jdt.core.manipulation"), null)));
	}
}
