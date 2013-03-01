/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

public class TestRequestFlexerOneInstalledReplacingIt extends AbstractProvisioningTest {
	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 1 \n" + "depends: platform = 1")
	public IInstallableUnit sdk1;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit platform1;

	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 2 \n" + "depends: platform = 2")
	public IInstallableUnit sdk2;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit platform2;
	IProfile profile;

	private IPlanner planner;

	private IEngine engine;

	private IProfileChangeRequest originalRequest;

	private ProvisioningContext context;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		profile = createProfile("TestProfile." + getName());
		IULoader.loadIUs(this);
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2});
		planner = createPlanner();
		engine = createEngine();
		assertOK(install(profile, new IInstallableUnit[] {sdk1}, true, planner, engine));

		originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(sdk2);
		originalRequest.setInstallableUnitInclusionRules(sdk2, ProfileInclusionRules.createStrictInclusionRule(sdk2));
		assertNotOK(planner.getProvisioningPlan(originalRequest, context, null).getStatus());

		context = new ProvisioningContext(getAgent());

	}

	public void testRemoveInstalledElement() {
		IProfileChangeRequest requestToTest = planner.createChangeRequest(profile);
		requestToTest.add(sdk2);
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowInstalledElementRemoval(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(requestToTest, profile, new NullProgressMonitor());
		assertTrue(realRequest.getRemovals().contains(sdk1));
		assertTrue(realRequest.getAdditions().contains(sdk2));
		assertEquals(1, realRequest.getRemovals().size());
		assertOK(install(realRequest, planner, engine));
	}

	public void testUpdateInstalledElement() {
		IProfileChangeRequest requestToTest = planner.createChangeRequest(profile);
		requestToTest.add(sdk2);
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowInstalledElementChange(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(requestToTest, profile, new NullProgressMonitor());
		assertTrue(realRequest.getRemovals().contains(sdk1));
		assertTrue(realRequest.getAdditions().contains(sdk2));
		assertEquals(1, realRequest.getRemovals().size());
		assertEquals(1, realRequest.getAdditions().size());
		assertOK(install(realRequest, planner, engine));
	}

	public void testUDifferentVersionAndUpdateInstalledElement() {
		IProfileChangeRequest requestToTest = planner.createChangeRequest(profile);
		requestToTest.add(sdk2);
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowDifferentVersion(true);
		av.setAllowInstalledElementChange(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(requestToTest, profile, new NullProgressMonitor());
		assertTrue(realRequest.getRemovals().contains(sdk1));
		assertTrue(realRequest.getAdditions().contains(sdk2));
		assertEquals(1, realRequest.getRemovals().size());
		assertEquals(1, realRequest.getAdditions().size());
		assertOK(install(realRequest, planner, engine));
	}
}
