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

public class TestRequestFlexerRequestWithOptionalInstall extends AbstractProvisioningTest {
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
		profile = createProfile("TestProfile." + getName());
		IULoader.loadIUs(this);
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1});
		planner = createPlanner();
		engine = createEngine();
		context = new ProvisioningContext(getAgent());
		assertOK(installAsRoots(profile, new IInstallableUnit[] {sdk1}, true, planner, engine));
	}

	public void testWithOptionalInstall() {
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, egit1, egit2, egit3});

		IProfileChangeRequest originalRequest = planner.createChangeRequest(profile);
		originalRequest.add(egit2);
		originalRequest.setInstallableUnitInclusionRules(egit2, ProfileInclusionRules.createOptionalInclusionRule(egit2));

		{
			//The request is unsatisfiable is no flexibility is given
			RequestFlexer av = new RequestFlexer(planner);
			av.setProvisioningContext(context);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertNull(realRequest);
		}

		{
			//The base is allowed to change so egit2 will install fine
			RequestFlexer av = new RequestFlexer(planner);
			av.setAllowInstalledElementChange(true);
			av.setProvisioningContext(context);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertTrue(realRequest.getAdditions().contains(egit2));
			assertTrue(realRequest.getAdditions().contains(sdk2));
			assertEquals(2, realRequest.getAdditions().size());
			assertTrue(realRequest.getRemovals().contains(sdk1));
			assertResolve(realRequest, planner);
		}

		{
			//We allow for a different version to be installed, so egit1 will be installed
			RequestFlexer av = new RequestFlexer(planner);
			av.setAllowDifferentVersion(true);
			av.setProvisioningContext(context);
			IProfileChangeRequest realRequest = av.getChangeRequest(originalRequest, profile, new NullProgressMonitor());
			assertTrue(realRequest.getAdditions().contains(egit1));
			assertEquals(1, realRequest.getAdditions().size());
			assertTrue(isOptionallyBeingInstalled(egit1, realRequest));
			assertResolve(realRequest, planner);
		}
	}

	private boolean isOptionallyBeingInstalled(IInstallableUnit iu, IProfileChangeRequest originalRequest) {
		Map<String, String> match = ((ProfileChangeRequest) originalRequest).getInstallableUnitProfilePropertiesToAdd().get(iu);
		if (match == null)
			return false;
		return INCLUSION_OPTIONAL.equals(match.get(INCLUSION_RULES));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getProfileRegistry().removeProfile(profile.getProfileId());
	}

}
