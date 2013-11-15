/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
import java.util.ArrayList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug252682 extends AbstractProvisioningTest {
	IProfile profile = null;
	ArrayList newIUs = new ArrayList();

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 252682", "testData/bug252682");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), tempFolder, null, false);
		profile = registry.getProfile("Bug252682");
		assertNotNull(profile);
		newIUs.add(createEclipseIU("org.eclipse.equinox.p2.core", Version.createOSGi(1, 0, 100, "v20081024")));

		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		IProvisioningPlan plan = createPlanner().getProvisioningPlan(req, null, null);
		assertOK("validate profile", plan.getStatus());
	}

	public void testInstallFeaturePatch() {
		IInstallableUnit p2Feature = profile.query(QueryUtil.createIUQuery("org.eclipse.equinox.p2.user.ui.feature.group"), new NullProgressMonitor()).iterator().next();
		System.out.println(p2Feature);
		IQueryResult c = profile.query(QueryUtil.createIUQuery("org.eclipse.equinox.p2.core.patch"), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		ctx.setExtraInstallableUnits(newIUs);
		IInstallableUnit patch = (IInstallableUnit) c.iterator().next();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(new IInstallableUnit[] {patch});
		IPlanner planner = createPlanner();
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("Installation plan", plan.getStatus());
	}
}
