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
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug203637 extends AbstractProvisioningTest {
	public void test() {
		IDirector d = createDirector();
		IProfile profile = createProfile("TestProfile." + getName());
		IInstallableUnit a1 = createIU("A", Version.createOSGi(1, 0, 0), true);
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.add(a1);
		request.removeInstallableUnits(new IInstallableUnit[0]);
		assertOK("1.0", d.provision(request, null, null));
	}
}
