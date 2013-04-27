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

public class TestRequestFlexerOneInstalledOneBeingInstalled extends AbstractProvisioningTest {
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

	@IUDescription(content = "package: svn1 \n" + "singleton: true\n" + "version: 1 \n" + "depends: platform = 1")
	public IInstallableUnit svn1;

	@IUDescription(content = "package: svn2 \n" + "singleton: true\n" + "version: 2 \n" + "depends: platform = 2")
	public IInstallableUnit svn2;

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
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2});
		planner = createPlanner();
		engine = createEngine();
		assertOK(install(profile, new IInstallableUnit[] {sdk1}, true, planner, engine));

		originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(egit2);
		originalRequest.setInstallableUnitInclusionRules(egit2, ProfileInclusionRules.createStrictInclusionRule(egit2));
		assertNotOK(planner.getProvisioningPlan(originalRequest, context, null).getStatus());

		{
			//Make sure that the goal we are after can actually be reached
			IProfileChangeRequest validateGoalRequest = planner.createChangeRequest(profile);
			validateGoalRequest.add(egit2);
			validateGoalRequest.setInstallableUnitInclusionRules(egit2, ProfileInclusionRules.createStrictInclusionRule(egit2));
			validateGoalRequest.add(sdk2);
			validateGoalRequest.setInstallableUnitInclusionRules(sdk2, ProfileInclusionRules.createStrictInclusionRule(sdk2));
			validateGoalRequest.remove(sdk1);
			assertOK(planner.getProvisioningPlan(validateGoalRequest, context, null).getStatus());
		}

		{
			//Make sure that the goal we are after can actually be reached
			IProfileChangeRequest validateGoalRequest = planner.createChangeRequest(profile);
			validateGoalRequest.add(egit1);
			validateGoalRequest.setInstallableUnitInclusionRules(egit1, ProfileInclusionRules.createStrictInclusionRule(egit1));
			assertOK(planner.getProvisioningPlan(validateGoalRequest, context, null).getStatus());
		}
		context = new ProvisioningContext(getAgent());

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getProfileRegistry().removeProfile(profile.getProfileId());
	}

	//Found a solution by installing a previous version
	public void testInstallAnotherVersion() {
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowDifferentVersion(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
		assertTrue(realRequest.getAdditions().contains(egit1));
		assertEquals(1, realRequest.getAdditions().size());
		assertTrue(realRequest.getRemovals().isEmpty());
		assertOK(install(realRequest, planner, engine));
	}

	public void testInstallPartial() {
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowPartialInstall(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
		assertNull(realRequest);
	}

	public void testInstallSomeElementsWithUpdates() {
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowPartialInstall(true);
		av.setAllowDifferentVersion(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
		assertTrue(realRequest.getAdditions().contains(egit1));
		assertEquals(1, realRequest.getAdditions().size());
		assertTrue(realRequest.getRemovals().isEmpty());
		assertOK(install(realRequest, planner, engine));
	}

	//Found a solution by changing the base
	public void testUpdateBase() {
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowInstalledElementChange(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
		assertTrue(realRequest.getAdditions().contains(egit2));
		assertTrue(realRequest.getAdditions().contains(sdk2));
		assertEquals(2, realRequest.getAdditions().size());
		assertTrue(realRequest.getRemovals().contains(sdk1));
		assertEquals(1, realRequest.getRemovals().size());
		assertOK(install(realRequest, planner, engine));
	}

	//Found a solution by removing the base - in this case only the new element is installed
	public void testRemoveBase() {
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowInstalledElementRemoval(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
		assertTrue(realRequest.getAdditions().contains(egit2));
		assertTrue(realRequest.getRemovals().contains(sdk1));
		assertOK(install(realRequest, planner, engine));
	}

	public void testAllowEverything() {
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowPartialInstall(true);
		av.setAllowDifferentVersion(true);
		av.setAllowInstalledElementRemoval(true);
		av.setAllowInstalledElementChange(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
		//We only sdk1 because our goal is maximize the number of installed element and minimize the number of changes
		assertTrue(realRequest.getAdditions().contains(egit2));
		assertTrue(realRequest.getAdditions().contains(sdk2));
		assertTrue(realRequest.getRemovals().contains(sdk1));
		assertOK(install(realRequest, planner, engine));
	}

	public void testDoNothing() {
		RequestFlexer av = new RequestFlexer(planner);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
		assertNull(realRequest);
	}
}
