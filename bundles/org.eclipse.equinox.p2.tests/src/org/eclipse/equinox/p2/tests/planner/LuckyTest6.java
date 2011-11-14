/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * Sonatype, Inc. - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.*;

public class LuckyTest6 extends AbstractProvisioningTest {
	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 1 \n" + "depends: platform = 1")
	public IInstallableUnit sdk1;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit platform1;

	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 2 \n" + "depends: platform = 2")
	public IInstallableUnit sdk2;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit platform2;

	@IUDescription(content = "package: sdk \n" + "singleton: true\n" + "version: 3 \n" + "depends: platform = 2")
	public IInstallableUnit sdk3;

	IProfile profile = createProfile("TestProfile." + getName());

	private IPlanner planner;

	private IEngine engine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IULoader.loadIUs(this);
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, sdk3, platform2});
		planner = createPlanner();
		engine = createEngine();
		assertOK(install(profile, new IInstallableUnit[] {sdk1}, true, planner, engine));
	}

	//Test that the highest version is installed
	public void testInstallSDK3() {
		assertNotOK(install(profile, new IInstallableUnit[] {platform2}, true, planner, engine));

		ProfileChangeRequest res = new LuckyHelper().computeProfileChangeRequest(profile, planner, null, new ProvisioningContext(getAgent()), getMonitor());
		assertTrue(res.getAdditions().contains(sdk3));
		assertTrue(res.getRemovals().contains(sdk1));
	}

}
