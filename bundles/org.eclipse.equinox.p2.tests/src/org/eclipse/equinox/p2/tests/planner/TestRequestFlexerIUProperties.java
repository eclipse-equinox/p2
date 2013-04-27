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

import java.util.Map;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.operations.RequestFlexer;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.*;
import org.eclipse.equinox.p2.tests.*;

public class TestRequestFlexerIUProperties extends AbstractProvisioningTest {
	final String INCLUSION_RULES = "org.eclipse.equinox.p2.internal.inclusion.rules"; //$NON-NLS-1$
	final String INCLUSION_OPTIONAL = "OPTIONAL"; //$NON-NLS-1$
	final String INCLUSION_STRICT = "STRICT"; //$NON-NLS-1$

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
		IULoader.loadIUs(this);
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1});
		planner = createPlanner();
		engine = createEngine();
		context = new ProvisioningContext(getAgent());
	}

	public void testWithChanginRootFromOptionalToStrict() {
		profile = createProfile("TestProfile." + getName());
		assertOK(installAsRoots(profile, new IInstallableUnit[] {sdk1}, false, planner, engine));
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2});

		IProfileChangeRequest originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(egit2);
		originalRequest.setInstallableUnitInclusionRules(egit2, ProfileInclusionRules.createStrictInclusionRule(egit2));
		originalRequest.setInstallableUnitInclusionRules(sdk1, ProfileInclusionRules.createStrictInclusionRule(sdk1));
		assertNotOK(planner.getProvisioningPlan(originalRequest, null, null).getStatus());

		{
			//Verify that it is possible to install egit2 because the sdk1 is optional
			RequestFlexer av = new RequestFlexer(planner);
			av.setAllowInstalledElementChange(true);
			av.setProvisioningContext(context);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertTrue(realRequest.getAdditions().contains(egit2));
			assertTrue(realRequest.getAdditions().contains(sdk2));
			assertTrue(realRequest.getRemovals().contains(sdk1));
			assertEquals(2, realRequest.getAdditions().size());
			assertEquals(1, realRequest.getRemovals().size());
		}
	}

	public void testWithChanginRootFromOptionalToStrictByRemovingIUProperty() {
		profile = createProfile("TestProfile." + getName());
		assertOK(installAsRoots(profile, new IInstallableUnit[] {sdk1}, false, planner, engine));
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2});

		IProfileChangeRequest originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(egit2);
		originalRequest.setInstallableUnitInclusionRules(egit2, ProfileInclusionRules.createStrictInclusionRule(egit2));
		originalRequest.removeInstallableUnitInclusionRules(sdk1);
		assertNotOK(planner.getProvisioningPlan(originalRequest, null, null).getStatus());

		{
			//Verify that it is possible to install egit2 because the sdk1 is optional
			RequestFlexer av = new RequestFlexer(planner);
			av.setAllowInstalledElementChange(true);
			av.setProvisioningContext(context);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertTrue(realRequest.getAdditions().contains(egit2));
			assertTrue(realRequest.getAdditions().contains(sdk2));
			assertTrue(realRequest.getRemovals().contains(sdk1));
			assertEquals(2, realRequest.getAdditions().size());
			assertEquals(1, realRequest.getRemovals().size());
		}
	}

	public void testWithChanginRootFromStrictToOptional() {
		profile = createProfile("TestProfile." + getName());
		assertOK(installAsRoots(profile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2});

		IProfileChangeRequest originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(egit2);
		originalRequest.setInstallableUnitInclusionRules(egit2, ProfileInclusionRules.createStrictInclusionRule(egit2));
		originalRequest.setInstallableUnitInclusionRules(sdk1, ProfileInclusionRules.createOptionalInclusionRule(sdk1));
		assertOK(planner.getProvisioningPlan(originalRequest, null, null).getStatus());

		{
			//Verify that it is possible to install egit2 because the sdk1 inclusion is change to optional
			assertResolve(originalRequest, planner);
		}

		{
			//Verify that it is possible to install egit2 because the sdk1 inclusion is change to optional
			RequestFlexer av = new RequestFlexer(planner);
			av.setAllowInstalledElementChange(true);
			av.setProvisioningContext(context);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertTrue(realRequest.getAdditions().contains(egit2));
			assertTrue(realRequest.getAdditions().contains(sdk2));
			assertTrue(realRequest.getRemovals().contains(sdk1));
			assertEquals(2, realRequest.getAdditions().size());
			assertEquals(1, realRequest.getRemovals().size());
			assertResolve(realRequest, planner);
			assertTrue(isOptionallyBeingInstalled(sdk2, realRequest));
		}

	}

	public void testRandomIUProperty() {
		profile = createProfile("TestProfile." + getName());
		assertOK(installAsRoots(profile, new IInstallableUnit[] {sdk1}, true, planner, engine));
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2});

		IProfileChangeRequest originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(egit3);
		originalRequest.setInstallableUnitInclusionRules(egit3, ProfileInclusionRules.createStrictInclusionRule(egit3));
		originalRequest.setInstallableUnitProfileProperty(egit3, "MYKEY", "MYVALUE");
		assertNotOK(planner.getProvisioningPlan(originalRequest, null, null).getStatus());

		//Verify that it is possible to install egit2 because the sdk1 inclusion is change to optional
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowDifferentVersion(true);
		av.setProvisioningContext(context);
		IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
		assertTrue(realRequest.getAdditions().contains(egit1));
		assertEquals(1, realRequest.getAdditions().size());
		assertResolve(realRequest, planner);
		assertKeyValue(egit1, "MYKEY", "MYVALUE", realRequest);
	}

	private void assertKeyValue(IInstallableUnit iu, String key, String value, IProfileChangeRequest request) {
		Map<String, String> match = ((ProfileChangeRequest) request).getInstallableUnitProfilePropertiesToAdd().get(iu);
		assertNotNull(match);
		assertEquals(value, match.get(key));
	}

	private boolean isOptionallyBeingInstalled(IInstallableUnit iu, IProfileChangeRequest request) {
		Map<String, String> match = ((ProfileChangeRequest) request).getInstallableUnitProfilePropertiesToAdd().get(iu);
		if (match == null)
			return false;
		return INCLUSION_OPTIONAL.equals(match.get(INCLUSION_RULES));
	}
}
