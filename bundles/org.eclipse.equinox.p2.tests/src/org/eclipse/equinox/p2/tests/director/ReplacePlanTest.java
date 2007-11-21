/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.engine.DefaultPhaseSet;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Tests {@link IPlanner#getReplacePlan(IInstallableUnit[], IInstallableUnit[], Profile, org.eclipse.core.runtime.IProgressMonitor)}.
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
	Profile profile;

	protected void setUp() throws Exception {
		//base IU that others require
		f1 = createIU("f1", DEFAULT_VERSION, true);
		f1_1 = createIU("f1", new Version(1, 1, 0), true);
		f1_4 = createIU("f1", new Version(1, 4, 0), true);

		//fragments of base IU
		frag1 = createIUFragment(f1, "frag1", f1.getVersion());
		frag1_1 = createIUFragment(f1, "frag1", f1_1.getVersion());
		frag1_4 = createIUFragment(f1, "frag1", f1_4.getVersion());

		//IUs that require base IU
		RequiredCapability[] requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU, "f1", new VersionRange("[1.0.0, 1.3.0)"), null);
		fa = createIU("fa", requires, false);
		requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU, "f1", new VersionRange("[1.0.0, 1.4.0)"), null);
		fap = createIU("fa", new Version(1, 1, 0), requires, NO_PROPERTIES, false);

		createTestMetdataRepository(new IInstallableUnit[] {f1, fa, frag1});

		profile = createProfile("TestProfile." + getName());
		director = createDirector();
		planner = createPlanner();
		director.install(new IInstallableUnit[] {fa, frag1}, profile, null);

		createTestMetdataRepository(new IInstallableUnit[] {f1_1, f1_4, frag1_1, frag1_4});
	}

	public void testSimpleReplace() {
		IInstallableUnit[] oldUnits = new IInstallableUnit[] {fa};
		IInstallableUnit[] newUnits = new IInstallableUnit[] {fap};
		ProvisioningPlan plan = planner.getReplacePlan(oldUnits, newUnits, profile, null);
		assertTrue("1.0", plan.getStatus().isOK());
		assertProfileContainsAll("1.1", profile, oldUnits);
		IStatus result = createEngine().perform(profile, new DefaultPhaseSet(), plan.getOperands(), null);
		assertTrue("1.2", result.isOK());
		assertProfileContainsAll("1.3", profile, newUnits);
	}

	public void testReplaceFragment() {
		//TODO it is strange that this succeeds, since frag1_4 and fa cannot co-exist
		IInstallableUnit[] oldUnits = new IInstallableUnit[] {frag1};
		IInstallableUnit[] newUnits = new IInstallableUnit[] {frag1_4};
		ProvisioningPlan plan = planner.getReplacePlan(oldUnits, newUnits, profile, null);
		assertTrue("1.0", plan.getStatus().isOK());
	}

}
