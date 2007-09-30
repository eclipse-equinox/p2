/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.tests.director;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.prov.director.IDirector;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.InstallableUnit;
import org.eclipse.equinox.prov.tests.AbstractProvisioningTest;
import org.osgi.framework.Version;

public class SingletonTest extends AbstractProvisioningTest {
	InstallableUnit f1;
	InstallableUnit f1_1;

	InstallableUnit f2;
	InstallableUnit f2_1;

	InstallableUnit junit38;
	InstallableUnit junit40;

	IDirector director;
	Profile profile;

	protected void setUp() throws Exception {
		f1 = new InstallableUnit();
		f1.setId("f1");
		f1.setVersion(new Version(1, 0, 0));
		f1.setSingleton(true);

		f1_1 = new InstallableUnit();
		f1_1.setId("f1");
		f1_1.setVersion(new Version(1, 1, 0));
		f1_1.setSingleton(true);

		f2 = new InstallableUnit();
		f2.setId("f2");
		f2.setVersion(new Version(1, 0, 0));
		f2.setSingleton(true);

		f2_1 = new InstallableUnit();
		f2_1.setId("f2");
		f2_1.setVersion(new Version(1, 0, 1));

		junit38 = new InstallableUnit();
		junit38.setId("junit");
		junit38.setVersion(new Version(3, 8, 1));

		junit40 = new InstallableUnit();
		junit40.setId("junit");
		junit40.setVersion(new Version(4, 0, 1));

		createTestMetdataRepository(new IInstallableUnit[] {f1, f1_1, junit38, junit40, f2, f2_1});

		profile = new Profile("TestProfile." + getName());
		director = createDirector();
	}

	public void testMultipleVersionNonSingleton() {
		//The installation of junit38 and junit 40 together should succeed
		assertEquals(director.install(new IInstallableUnit[] {junit38, junit40}, profile, null, new NullProgressMonitor()).getSeverity(), IStatus.OK);
	}

	public void testMultipleVersionSingleton() {
		//The installation of junit38 and junit 40 together should succeed
		assertEquals(director.install(new IInstallableUnit[] {f1, f1_1}, profile, null, new NullProgressMonitor()).getSeverity(), IStatus.ERROR);
	}

	public void testMultipleVersionSingleton2() {
		//The installation of junit38 and junit 40 together should succeed
		assertEquals(director.install(new IInstallableUnit[] {f2, f2_1}, profile, null, new NullProgressMonitor()).getSeverity(), IStatus.ERROR);
	}
}
