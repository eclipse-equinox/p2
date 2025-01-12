/*******************************************************************************
 * Copyright (c) 2007, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests
 * {@link IPlanner#getProvisioningPlan(org.eclipse.equinox.p2.planner.IProfileChangeRequest, ProvisioningContext, org.eclipse.core.runtime.IProgressMonitor)}
 * involving replacing an IU with a different version.
 */
public class ReplacePlanTest extends AbstractProvisioningTest {
	IInstallableUnit f1;
	IInstallableUnit f1_1;
	IInstallableUnit f1_4;
	IInstallableUnit frag1;
	IInstallableUnit frag1_1;
	IInstallableUnit frag1_4;
	IInstallableUnit fa;
	IInstallableUnit fap;
	IDirector director;
	IPlanner planner;
	IProfile profile;

	@Override
	protected void setUp() throws Exception {
		//base IU that others require
		f1 = createIU("f1", DEFAULT_VERSION, true);
		f1_1 = createIU("f1", Version.createOSGi(1, 1, 0), true);
		f1_4 = createIU("f1", Version.createOSGi(1, 4, 0), true);

		//fragments of base IU
		frag1 = createIUFragment(f1, "frag1", f1.getVersion());
		frag1_1 = createIUFragment(f1, "frag1", f1_1.getVersion());
		frag1_4 = createIUFragment(f1, "frag1", f1_4.getVersion());

		//IUs that require base IU
		IRequirement[] requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "f1", new VersionRange("[1.0.0, 1.3.0)"));
		fa = createIU("fa", requires, false);
		requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "f1", new VersionRange("[1.0.0, 1.4.0)"));
		fap = createIU("fa", Version.createOSGi(1, 1, 0), requires, NO_PROPERTIES, false);

		createTestMetdataRepository(new IInstallableUnit[] {f1, fa, frag1});

		profile = createProfile("TestProfile." + getName());
		director = createDirector();
		planner = createPlanner();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {fa, frag1});
		director.provision(request, null, null);

		createTestMetdataRepository(new IInstallableUnit[] {f1_1, f1_4, frag1_1, frag1_4});
	}

	public void testSimpleReplace() {
		IInstallableUnit[] oldUnits = new IInstallableUnit[] {fa};
		IInstallableUnit[] newUnits = new IInstallableUnit[] {fap};
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(oldUnits);
		request.addInstallableUnits(newUnits);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, new ProvisioningContext(getAgent()), null);
		assertTrue("1.0", plan.getStatus().isOK());
		assertProfileContainsAll("1.1", profile, oldUnits);
		IStatus result = createEngine().perform(plan, null);
		assertTrue("1.2", result.isOK());
		assertProfileContainsAll("1.3", profile, newUnits);
	}

	public void testReplaceFragment() {
		//TODO it is strange that this succeeds, since frag1_4 and fa cannot co-exist
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(new IInstallableUnit[] {frag1});
		request.addInstallableUnits(new IInstallableUnit[] {frag1_4});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, new ProvisioningContext(getAgent()), null);
		assertTrue("1.0", plan.getStatus().isOK());
	}

}
