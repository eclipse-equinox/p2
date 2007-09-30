/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.tests.director;

import java.util.Collection;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.prov.director.IDirector;
import org.eclipse.equinox.prov.director.Oracle;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.*;
import org.eclipse.equinox.prov.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class OracleTest2 extends AbstractProvisioningTest {
	private InstallableUnit a1;
	private InstallableUnit a2;
	private InstallableUnit b1;
	private InstallableUnit c1;
	private InstallableUnit c2;

	IDirector director;
	Profile profile;

	protected void setUp() throws Exception {
		a1 = new InstallableUnit();
		a1.setId("A");
		a1.setVersion(new Version(1, 0, 0));
		a1.setSingleton(true);
		a1.setRequiredCapabilities(createRequiredCapabilities(IInstallableUnit.IU_NAMESPACE, "C", new VersionRange("[1.0.0, 2.0.0)"), null));

		c1 = new InstallableUnit();
		c1.setId("C");
		c1.setVersion(new Version(1, 0, 0));
		c1.setSingleton(true);

		a2 = new InstallableUnit();
		a2.setId("A");
		a2.setVersion(new Version(2, 0, 0));
		a2.setSingleton(true);
		a2.setProperty(IInstallableUnitConstants.UPDATE_FROM, "A");
		a2.setProperty(IInstallableUnitConstants.UPDATE_RANGE, "[1.0.0, 2.3.0)");
		a2.setRequiredCapabilities(createRequiredCapabilities(IInstallableUnit.IU_NAMESPACE, "C", new VersionRange("[2.0.0, 3.0.0)"), null));

		b1 = new InstallableUnit();
		b1.setId("B");
		b1.setVersion(new Version(1, 0, 0));
		b1.setSingleton(true);
		b1.setRequiredCapabilities(createRequiredCapabilities(IInstallableUnit.IU_NAMESPACE, "C", new VersionRange("[2.0.0, 3.0.0)"), null));

		c2 = new InstallableUnit();
		c2.setId("C");
		c2.setVersion(new Version(2, 0, 0));
		c2.setSingleton(true);
		c2.setProperty(IInstallableUnitConstants.UPDATE_FROM, "C");
		c2.setProperty(IInstallableUnitConstants.UPDATE_RANGE, "[1.0.0, 2.3.0)");

		createTestMetdataRepository(new IInstallableUnit[] {a1, c1});

		profile = new Profile("TestProfile." + getName());
		director = createDirector();

	}

	public void testInstallA1() {
		assertEquals(director.install(new IInstallableUnit[] {a1}, profile, null, null).getSeverity(), IStatus.OK);

		createTestMetdataRepository(new IInstallableUnit[] {a2, c2, b1});
		Collection brokenEntryPoint = (Collection) new Oracle().canInstall(new IInstallableUnit[] {b1}, profile, null);
		//		assertNotNull(brokenEntryPoint.getProperty("entryPoint"));

		new Oracle().hasUpdate(a1);
		System.out.println(new Oracle().canInstall(new IInstallableUnit[] {b1}, (IInstallableUnit[]) brokenEntryPoint.toArray(new IInstallableUnit[brokenEntryPoint.size()]), profile, null));
	}

	public void testInstallA1bis() {
		profile = new Profile("testInstallA1bis." + getName());
		director = createDirector();
		createTestMetdataRepository(new IInstallableUnit[] {a1, a2, c1, c2, b1});

		assertEquals(director.install(new IInstallableUnit[] {a1}, profile, null, null).getSeverity(), IStatus.OK);
	}
}
