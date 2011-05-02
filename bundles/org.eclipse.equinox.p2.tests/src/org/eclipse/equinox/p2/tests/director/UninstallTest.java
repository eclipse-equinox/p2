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

import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class UninstallTest extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IProfile profile;
	private IDirector director;

	protected void setUp() throws Exception {
		a1 = createIU("A", DEFAULT_VERSION, true);

		profile = createProfile("TestProfile." + getName());
		director = createDirector();
	}

	public void testUninstall() {
		IInstallableUnit[] units = new IInstallableUnit[] {a1};
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(units);
		assertTrue("1.0", director.provision(request, null, null).isOK());
		assertProfileContains("1.1", profile, units);
		request = new ProfileChangeRequest(profile);
		request.removeInstallableUnits(units);
		assertTrue("1.2", director.provision(request, null, null).isOK());
		assertEmptyProfile(profile);
		// uninstalling on empty profile should be a no-op
		assertTrue("1.3", director.provision(request, null, null).isOK());
		assertEmptyProfile(profile);
	}
}
