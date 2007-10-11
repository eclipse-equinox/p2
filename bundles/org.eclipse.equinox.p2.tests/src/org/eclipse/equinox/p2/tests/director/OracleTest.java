/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class OracleTest extends AbstractProvisioningTest {
	InstallableUnit a1;
	InstallableUnit a2;
	InstallableUnit b1;
	InstallableUnit c1;
	InstallableUnit d1;
	InstallableUnit d2;

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
		c1.setRequiredCapabilities(createRequiredCapabilities(IInstallableUnit.IU_NAMESPACE, "D", new VersionRange("[1.0.0, 3.0.0)"), null));

		d1 = new InstallableUnit();
		d1.setId("D");
		d1.setVersion(new Version(1, 0, 0));
		d1.setSingleton(true);

		b1 = new InstallableUnit();
		b1.setId("B");
		b1.setVersion(new Version(1, 0, 0));
		b1.setSingleton(true);
		b1.setRequiredCapabilities(createRequiredCapabilities(IInstallableUnit.IU_NAMESPACE, "D", new VersionRange("[2.0.0, 3.0.0)"), null));

		d2 = new InstallableUnit();
		d2.setId("D");
		d2.setVersion(new Version(2, 0, 0));
		d2.setSingleton(true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, c1, d1, b1});

		profile = new Profile("TestProfile." + getName());
		director = createDirector();

	}

	public void testInstallA1() {
		assertEquals(director.install(new IInstallableUnit[] {a1}, profile, null).getSeverity(), IStatus.OK);

		createTestMetdataRepository(new IInstallableUnit[] {d2});
		//		assertEquals(new Oracle().canInstall(new IInstallableUnit[] {b1}, profile, null), true);
		assertEquals(director.install(new IInstallableUnit[] {b1}, profile, null).getSeverity(), IStatus.OK);
	}
}
