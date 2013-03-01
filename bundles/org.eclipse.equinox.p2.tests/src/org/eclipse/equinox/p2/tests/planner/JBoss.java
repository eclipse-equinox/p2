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
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.tests.*;

public class JBoss extends AbstractProvisioningTest {
	@IUDescription(content = "package: jboss \n" + "singleton: true\n" + "version: 6 \n" + "depends: m2e = 12")
	public IInstallableUnit jboss;

	@IUDescription(content = "package: m2e \n" + "singleton: true\n" + "version: 11 \n")
	public IInstallableUnit m2e11;

	@IUDescription(content = "package: m2e \n" + "singleton: true\n" + "version: 12 \n")
	public IInstallableUnit m2e12;

	IProfile profile = createProfile("TestProfile." + getClass().getSimpleName());

	private IPlanner planner;

	private IEngine engine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IULoader.loadIUs(this);
		createTestMetdataRepository(new IInstallableUnit[] {jboss, m2e11, m2e12});
		planner = createPlanner();
		engine = createEngine();
		assertOK(install(profile, new IInstallableUnit[] {m2e11}, true, planner, engine));
	}

	public void testInstallJBoss() {
		ProfileChangeRequest installJBoss = new ProfileChangeRequest(profile);
		installJBoss.add(jboss);
		assertNotOK(install(installJBoss, planner, engine));

		IProfileChangeRequest res = new LuckyHelper().computeProfileChangeRequest(profile, planner, installJBoss, new ProvisioningContext(getAgent()), getMonitor());
		assertTrue(res.getAdditions().contains(m2e12));
		assertTrue(res.getRemovals().contains(m2e11));

	}

}
