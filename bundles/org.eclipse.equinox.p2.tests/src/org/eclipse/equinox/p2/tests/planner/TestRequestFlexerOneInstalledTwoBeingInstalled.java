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

public class TestRequestFlexerOneInstalledTwoBeingInstalled extends AbstractProvisioningTest {
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

	@IUDescription(content = "package: svn \n" + "singleton: true\n" + "version: 1 \n" + "depends: platform = 1")
	public IInstallableUnit svn1;

	@IUDescription(content = "package: svn \n" + "singleton: true\n" + "version: 2 \n" + "depends: platform = 2")
	public IInstallableUnit svn2;

	@IUDescription(content = "package: svn \n" + "singleton: true\n" + "version: 3 \n" + "depends: platform = 3")
	public IInstallableUnit svn3;

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

	public void testAssumptions() {
		IProfileChangeRequest originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(egit2);
		originalRequest.setInstallableUnitInclusionRules(egit2, ProfileInclusionRules.createStrictInclusionRule(egit2));
		originalRequest.add(svn1);
		originalRequest.setInstallableUnitInclusionRules(svn1, ProfileInclusionRules.createStrictInclusionRule(svn1));
		assertNotOK(planner.getProvisioningPlan(originalRequest, context, null).getStatus());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getProfileRegistry().removeProfile(profile.getProfileId());
	}

	public void testConflictWithTheBase() {
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2, egit3, svn1, svn2, svn3});

		IProfileChangeRequest originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(egit2);
		originalRequest.setInstallableUnitInclusionRules(egit2, ProfileInclusionRules.createStrictInclusionRule(egit2));
		originalRequest.add(svn1);
		originalRequest.setInstallableUnitInclusionRules(svn1, ProfileInclusionRules.createStrictInclusionRule(svn1));

		{
			RequestFlexer av = new RequestFlexer(planner);
			av.setAllowDifferentVersion(true);
			av.setProvisioningContext(context);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertTrue(realRequest.getAdditions().contains(egit1));
			assertTrue(realRequest.getAdditions().contains(svn1));
			assertEquals(2, realRequest.getAdditions().size());
			assertTrue(realRequest.getRemovals().isEmpty());
			assertResolve(realRequest, planner);
		}

		{
			RequestFlexer av = new RequestFlexer(planner);
			av.setAllowPartialInstall(true);
			av.setProvisioningContext(context);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertTrue(realRequest.getAdditions().contains(svn1));
			assertEquals(1, realRequest.getAdditions().size());
			assertTrue(realRequest.getRemovals().isEmpty());
		}

		{
			RequestFlexer av = new RequestFlexer(planner);
			av.setAllowInstalledElementChange(true);
			av.setAllowDifferentVersion(true);
			av.setProvisioningContext(context);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertTrue(realRequest.getAdditions().contains(svn2));
			assertTrue(realRequest.getAdditions().contains(egit2));
			assertTrue(realRequest.getAdditions().contains(svn2));
			assertEquals(3, realRequest.getAdditions().size());
			assertTrue(realRequest.getRemovals().contains(sdk1));
		}
	}

	//	public void testOneElementWithMissingRequirement() {
	//		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2, egit3, svn1, svn2});
	//
	//		IProfileChangeRequest originalRequest = planner.createChangeRequest(profile);
	//		originalRequest.add(egit3); //This is the element for which we have a missing requirement
	//		originalRequest.setInstallableUnitInclusionRules(egit3, ProfileInclusionRules.createStrictInclusionRule(egit3));
	//		originalRequest.add(svn1);
	//		originalRequest.setInstallableUnitInclusionRules(svn1, ProfileInclusionRules.createStrictInclusionRule(svn1));
	//
	//		{
	//			AnyVersion2 av = new AnyVersion2(planner);
	//			av.setAllowPartialInstall(true);
	//			av.setProvisioningContext(context);
	//			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
	//			assertTrue(realRequest.getAdditions().contains(svn1));
	//			assertEquals(1, realRequest.getAdditions().size());
	//			assertTrue(realRequest.getRemovals().isEmpty());
	//		}
	//
	//		{
	//			AnyVersion2 av = new AnyVersion2(planner);
	//			av.setAllowDifferentVersion(true);
	//			av.setProvisioningContext(context);
	//			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
	//			assertTrue(realRequest.getAdditions().contains(svn2));
	//			assertTrue(realRequest.getAdditions().contains(egit2));
	//			assertTrue(realRequest.getRemovals().isEmpty());
	//		}
	//
	//		{
	//			AnyVersion2 av = new AnyVersion2(planner);
	//			av.setAllowInstalledElementChange(true);
	//			av.setAllowDifferentVersion(true);
	//			av.setProvisioningContext(context);
	//			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
	//			assertTrue(realRequest.getAdditions().contains(svn2));
	//			assertTrue(realRequest.getAdditions().contains(egit2));
	//			assertTrue(realRequest.getRemovals().isEmpty());
	//		}
	//	}

}
