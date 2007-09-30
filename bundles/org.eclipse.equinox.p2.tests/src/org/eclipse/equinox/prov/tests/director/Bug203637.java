/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.prov.tests.director;

import org.eclipse.equinox.prov.director.IDirector;
import org.eclipse.equinox.prov.engine.Profile;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.metadata.InstallableUnit;
import org.eclipse.equinox.prov.tests.AbstractProvisioningTest;
import org.osgi.framework.Version;

public class Bug203637 extends AbstractProvisioningTest {
	public void test() {
		IDirector d = createDirector();
		Profile profile = new Profile("TestProfile." + getName());
		InstallableUnit a1 = new InstallableUnit();
		a1.setId("A");
		a1.setVersion(new Version(1, 0, 0));
		a1.setSingleton(true);

		assertOK(d.replace(new IInstallableUnit[0], new IInstallableUnit[] {a1}, profile, null));

	}
}
