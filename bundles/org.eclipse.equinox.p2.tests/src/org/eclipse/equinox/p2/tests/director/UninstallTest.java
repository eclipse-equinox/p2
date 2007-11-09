/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class UninstallTest extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private Profile profile;
	private IDirector director;

	protected void setUp() throws Exception {
		a1 = createIU("A", DEFAULT_VERSION, true);

		profile = new Profile("TestProfile." + getName());
		director = createDirector();
	}

	public void testUninstall() {
		System.out.println(director.install(new IInstallableUnit[] {a1}, profile, null));
		director.uninstall(new IInstallableUnit[] {a1}, profile, null);
		assertEmptyProfile(profile);
	}
}
