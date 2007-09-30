/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class UpdateTestWithoutEntryPoint extends AbstractProvisioningTest {
	InstallableUnit f1;
	InstallableUnit f1_1;
	InstallableUnit f1_4;

	InstallableUnit fa;
	InstallableUnit fap;
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

		f1_4 = new InstallableUnit();
		f1_4.setId("f1");
		f1_4.setVersion(new Version(1, 4, 0));
		f1_4.setSingleton(true);

		fa = new InstallableUnit();
		fa.setId("fa");
		fa.setVersion(new Version(1, 0, 0));
		fa.setRequiredCapabilities(createRequiredCapabilities(IInstallableUnit.IU_NAMESPACE, "f1", new VersionRange("[1.0.0, 1.3.0)"), null));

		fap = new InstallableUnit();
		fap.setId("fa");
		fap.setVersion(new Version(1, 1, 0));
		fap.setRequiredCapabilities(createRequiredCapabilities(IInstallableUnit.IU_NAMESPACE, "f1", new VersionRange("[1.0.0, 1.4.0)"), null));

		createTestMetdataRepository(new IInstallableUnit[] {f1, fa});

		profile = new Profile("TestProfile." + getName());
		director = createDirector();
		director.install(new IInstallableUnit[] {fa}, profile, null, null);

		createTestMetdataRepository(new IInstallableUnit[] {f1_1, f1_4});
	}

	public void testInstall() {
		assertEquals(director.install(new IInstallableUnit[] {f1_1}, profile, null, new NullProgressMonitor()).getSeverity(), IStatus.OK);
		for (Iterator iterator = profile.getInstallableUnits(); iterator.hasNext();) {
			System.out.println(iterator.next());

		}
		assertEquals(director.install(new IInstallableUnit[] {f1_4}, profile, null, new NullProgressMonitor()).getSeverity(), IStatus.ERROR);

		//		director.replace(new IInstallableUnit[] {fap}, profile, new NullProgressMonitor());
	}
}
