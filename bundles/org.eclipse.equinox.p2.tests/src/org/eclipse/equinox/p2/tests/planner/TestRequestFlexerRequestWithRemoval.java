/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.operations.RequestFlexer;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.*;
import org.eclipse.equinox.p2.tests.*;

public class TestRequestFlexerRequestWithRemoval extends AbstractProvisioningTest {
	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 1 \n" + "depends: platform = 1")
	public IInstallableUnit sdk1;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit platform1;

	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 2 \n" + "depends: platform = 2")
	public IInstallableUnit sdk2;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit platform2;

	@IUDescription(content = "package: egit \n" + "singleton: true\n" + "version: 1 \n" + "depends: platform = 1")
	public IInstallableUnit egit1;

	@IUDescription(content = "package: egit \n" + "singleton: true\n" + "version: 2 \n" + "depends: platform = 2")
	public IInstallableUnit egit2;

	@IUDescription(content = "package: egit \n" + "singleton: true\n" + "version: 3 \n" + "depends: platform = 3")
	public IInstallableUnit egit3;

	IProfile profile;

	private IPlanner planner;

	private IEngine engine;

	private ProvisioningContext context;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		profile = createProfile("TestProfile." + getName());
		IULoader.loadIUs(this);
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1});
		planner = createPlanner();
		engine = createEngine();
		context = new ProvisioningContext(getAgent());
		assertOK(install(profile, new IInstallableUnit[] {sdk1}, true, planner, engine));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getProfileRegistry().removeProfile(profile.getProfileId());
	}

	public void testWithRemovalInChangeRequest() {
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2, egit3});

		IProfileChangeRequest originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(egit2);
		originalRequest.setInstallableUnitInclusionRules(egit2, ProfileInclusionRules.createStrictInclusionRule(egit2));
		originalRequest.remove(sdk1);

		{
			RequestFlexer av = new RequestFlexer(planner);
			av.setProvisioningContext(context);
			av.setAllowDifferentVersion(true);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertTrue(realRequest.getAdditions().contains(egit1));
			assertTrue(realRequest.getRemovals().contains(sdk1));
			assertEquals(1, realRequest.getAdditions().size());
			assertResolve(realRequest, planner);
		}
	}
}
