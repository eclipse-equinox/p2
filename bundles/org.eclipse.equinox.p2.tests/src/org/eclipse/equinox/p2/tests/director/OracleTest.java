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
import org.eclipse.equinox.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class OracleTest extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit a2;
	IInstallableUnit b1;
	IInstallableUnit c1;
	IInstallableUnit d1;
	IInstallableUnit d2;

	IDirector director;
	Profile profile;

	protected void setUp() throws Exception {
		RequiredCapability[] requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU, "C", new VersionRange("[1.0.0, 2.0.0)"), null);
		a1 = createIU("A", requires, true);

		requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU, "D", new VersionRange("[1.0.0, 3.0.0)"), null);
		c1 = createIU("C", requires, true);

		d1 = createIU("D", DEFAULT_VERSION, true);

		requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU, "D", new VersionRange("[2.0.0, 3.0.0)"), null);
		b1 = createIU("B", requires, true);

		d2 = createIU("D", new Version(2, 0, 0), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, c1, d1, b1});

		profile = createProfile("TestProfile." + getName());
		director = createDirector();

	}

	public void testInstallA1() {
		assertEquals(IStatus.OK, director.install(new IInstallableUnit[] {a1}, profile, null, null).getSeverity());

		createTestMetdataRepository(new IInstallableUnit[] {d2});
		//		assertEquals(new Oracle().canInstall(new IInstallableUnit[] {b1}, profile, null), true);
		assertEquals(IStatus.OK, director.install(new IInstallableUnit[] {b1}, profile, null, null).getSeverity());
	}
}
