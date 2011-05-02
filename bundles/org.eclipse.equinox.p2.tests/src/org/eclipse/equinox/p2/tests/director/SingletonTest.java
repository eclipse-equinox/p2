/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SingletonTest extends AbstractProvisioningTest {
	IInstallableUnit f1;
	IInstallableUnit f1_1;

	IInstallableUnit f2;
	IInstallableUnit f2_1;

	IInstallableUnit junit38;
	IInstallableUnit junit40;

	IDirector director;
	IProfile profile;

	protected void setUp() throws Exception {
		f1 = createIU("f1", Version.createOSGi(1, 0, 0), true);

		f1_1 = createIU("f1", Version.createOSGi(1, 1, 0), true);

		f2 = createIU("f2", Version.createOSGi(1, 0, 0), true);

		f2_1 = createIU("f2", Version.createOSGi(1, 0, 1));

		junit38 = createIU("junit", Version.createOSGi(3, 8, 1));

		junit40 = createIU("junit", Version.createOSGi(4, 0, 1));

		createTestMetdataRepository(new IInstallableUnit[] {f1, f1_1, junit38, junit40, f2, f2_1});

		profile = createProfile("TestProfile." + getName());
		director = createDirector();
	}

	public void testMultipleVersionNonSingleton() {
		// The installation of junit38 and junit 40 together should succeed
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {junit38, junit40});
		assertEquals(IStatus.OK, director.provision(request, null, new NullProgressMonitor()).getSeverity());
	}

	public void testMultipleVersionSingleton() {
		// The installation of junit38 and junit 40 together should not succeed
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {f1, f1_1});
		assertEquals(IStatus.ERROR, director.provision(request, null, new NullProgressMonitor()).getSeverity());
	}

	public void testMultipleVersionSingleton2() {
		// The installation of junit38 and junit 40 together should not succeed
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {f2, f2_1});
		assertEquals(IStatus.ERROR, director.provision(request, null, new NullProgressMonitor()).getSeverity());
	}
}
