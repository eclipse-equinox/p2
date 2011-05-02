/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class OracleTest2 extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit a2;
	private IInstallableUnit b1;
	private IInstallableUnit c1;
	private IInstallableUnit c2;

	IDirector director;
	IProfile profile;

	protected void setUp() throws Exception {
		IRequirement[] requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[1.0.0, 2.0.0)"));
		a1 = createIU("A", requires, true);

		c1 = createIU("C", DEFAULT_VERSION, true);

		requires = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[2.0.0, 3.0.0)"));
		InstallableUnitDescription desc = new MetadataFactory.InstallableUnitDescription();
		desc.setRequirements(requires);
		desc.setId("A");
		desc.setVersion(Version.createOSGi(2, 0, 0));
		desc.setSingleton(true);
		desc.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor("A", new VersionRange("[1.0.0, 2.3.0)"), IUpdateDescriptor.NORMAL, null));

		a2 = MetadataFactory.createInstallableUnit(desc);

		b1 = createIU("B", DEFAULT_VERSION, requires, NO_PROPERTIES, true);

		InstallableUnitDescription desc2 = new MetadataFactory.InstallableUnitDescription();
		desc2.setId("C");
		desc2.setVersion(Version.createOSGi(2, 0, 0));
		desc2.setSingleton(true);
		desc2.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor("C", new VersionRange("[1.0.0, 2.3.0)"), IUpdateDescriptor.NORMAL, null));
		c2 = MetadataFactory.createInstallableUnit(desc2);

		createTestMetdataRepository(new IInstallableUnit[] {a1, c1});

		profile = createProfile("TestProfile." + getName());
		director = createDirector();

	}

	/* I'm not sure what this test should look like now
	 *
	 
	public void testInstallA1() {
		assertEquals(director.install(new IInstallableUnit[] {a1}, profile, null).getSeverity(), IStatus.OK);

		createTestMetdataRepository(new IInstallableUnit[] {a2, c2, b1});
		Collection brokenEntryPoint = (Collection) new Oracle().canInstall(new IInstallableUnit[] {b1}, profile, null);
		//		assertNotNull(brokenEntryPoint.getProperty("entryPoint"));

		new Oracle().hasUpdate(a1);
		System.out.println(new Oracle().canInstall(new IInstallableUnit[] {b1}, (IInstallableUnit[]) brokenEntryPoint.toArray(new IInstallableUnit[brokenEntryPoint.size()]), profile, null));
	}
	 */

	public void testInstallA1bis() {
		profile = createProfile("testInstallA1bis." + getName());
		director = createDirector();
		createTestMetdataRepository(new IInstallableUnit[] {a1, a2, c1, c2, b1});
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a1});
		assertEquals(IStatus.OK, director.provision(request, null, null).getSeverity());
	}
}
