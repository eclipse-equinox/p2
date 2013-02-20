/*******************************************************************************
 *  Copyright (c) 2013 Rapicorp, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.tests.*;

public class FromStrictToOptional extends AbstractProvisioningTest {
	@IUDescription(content = "package: A \n" + "singleton: true\n" + "version: 1 \n" + "depends: B = 1")
	public IInstallableUnit a1;

	@IUDescription(content = "package: B \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit b1;

	@IUDescription(content = "package: C \n" + "singleton: true\n" + "version: 1 \n" + "depends: B = 2")
	public IInstallableUnit c1;

	@IUDescription(content = "package: B \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit b2;

	IProfile profile = createProfile("TestProfile." + getName());

	private IPlanner planner;

	private IEngine engine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IULoader.loadIUs(this);
		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, c1});
		planner = createPlanner();
		engine = createEngine();
		assertOK(install(profile, new IInstallableUnit[] {a1}, true, planner, engine));
	}

	public void testChangeFromStrictToOptional() {
		assertNotOK(install(profile, new IInstallableUnit[] {c1}, true, planner, engine));
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.add(a1);
		req.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createStrictInclusionRule(a1));
		req.add(c1);
		req.setInstallableUnitInclusionRules(c1, ProfileInclusionRules.createOptionalInclusionRule(c1));
		assertOK(install(req, planner, engine));
	}
}
