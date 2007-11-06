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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
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
		f1 = createIU("f1");
		f1.setSingleton(true);

		f1_1 = createIU("f1", new Version(1, 1, 0));
		f1_1.setSingleton(true);

		f2 = createIU("f2");
		f2.setSingleton(true);

		f2_1 = createIU("f2", new Version(1, 0, 1));

		junit38 = createIU("junit", new Version(3, 8, 1));

		junit40 = createIU("junit", new Version(4, 0, 1));

		createTestMetdataRepository(new IInstallableUnit[] {f1, f1_1, junit38, junit40, f2, f2_1});

		profile = new Profile("TestProfile." + getName());
		director = createDirector();
	}

	public void testMultipleVersionNonSingleton() {
		// The installation of junit38 and junit 40 together should succeed
		assertEquals(IStatus.OK, director.install(new IInstallableUnit[] {junit38, junit40}, profile, new NullProgressMonitor()).getSeverity());
	}

	public void testMultipleVersionSingleton() {
		// The installation of junit38 and junit 40 together should succeed
		assertEquals(IStatus.ERROR, director.install(new IInstallableUnit[] {f1, f1_1}, profile, new NullProgressMonitor()).getSeverity());
	}

	public void testMultipleVersionSingleton2() {
		// The installation of junit38 and junit 40 together should succeed
		assertEquals(IStatus.ERROR, director.install(new IInstallableUnit[] {f2, f2_1}, profile, new NullProgressMonitor()).getSeverity());
	}
}
