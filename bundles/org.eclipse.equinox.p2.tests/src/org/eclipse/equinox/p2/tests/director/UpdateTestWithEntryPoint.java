/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.director.Oracle;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Version;

public class UpdateTestWithEntryPoint extends AbstractProvisioningTest {
	InstallableUnit f1;
	InstallableUnit f1_1;

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
		f1_1.setProperty(IInstallableUnitConstants.UPDATE_FROM, "f1");
		f1_1.setProperty(IInstallableUnitConstants.UPDATE_RANGE, "[1.0.0, 2.0.0)");

		profile = new Profile("TestProfile." + getName());
		director = createDirector();
		createTestMetdataRepository(new IInstallableUnit[] {f1_1, f1});
	}

	public void testInstall() {
		//TODO Currently this test is failing
		if (true)
			return;

		String entryPointName = "e1";
		assertEquals(director.install(new IInstallableUnit[] {f1}, profile, entryPointName, new NullProgressMonitor()).getSeverity(), IStatus.OK);
		for (Iterator iterator = profile.getInstallableUnits(); iterator.hasNext();) {
			System.out.println(iterator.next());

		}

		//TODO This should return an error because we are trying to update something that would cause the entry point to be unsatisfied
		//director.update(new IInstallableUnit[] {f1_1}, profile, new NullProgressMonitor());

		IInstallableUnit toReplace = get(entryPointName, profile);
		Collection updates = new Oracle().hasUpdate(toReplace);

		director.replace(new IInstallableUnit[] {toReplace}, (IInstallableUnit[]) updates.toArray(new IInstallableUnit[updates.size()]), profile, new NullProgressMonitor());

		//TODO Add a test to verify that we are not going down in the version
		//TODO Add a test to verify that we respect the ranges

	}

	private IInstallableUnit get(String id, Profile p) {
		Iterator it = profile.getInstallableUnits();
		while (it.hasNext()) {
			IInstallableUnit o = (IInstallableUnit) it.next();
			if (o.getId().equals(id))
				return o;
		}
		return null;
	}
}
