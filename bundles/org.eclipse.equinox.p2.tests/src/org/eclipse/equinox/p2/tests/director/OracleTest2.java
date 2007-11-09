/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class OracleTest2 extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit a2;
	private IInstallableUnit b1;
	private IInstallableUnit c1;
	private IInstallableUnit c2;

	IDirector director;
	Profile profile;

	protected void setUp() throws Exception {
		RequiredCapability[] requires = createRequiredCapabilities(IInstallableUnit.IU_NAMESPACE, "C", new VersionRange("[1.0.0, 2.0.0)"), null);
		a1 = createIU("A", requires, true);

		c1 = createIU("C", DEFAULT_VERSION, true);

		requires = createRequiredCapabilities(IInstallableUnit.IU_NAMESPACE, "C", new VersionRange("[2.0.0, 3.0.0)"), null);
		Map properties = new HashMap();
		properties.put(IInstallableUnitConstants.UPDATE_FROM, "A");
		properties.put(IInstallableUnitConstants.UPDATE_RANGE, "[1.0.0, 2.3.0)");
		a2 = createIU("A", new Version(2, 0, 0), requires, properties, true);

		b1 = createIU("B", DEFAULT_VERSION, requires, NO_PROPERTIES, true);

		properties.clear();
		properties.put(IInstallableUnitConstants.UPDATE_FROM, "C");
		properties.put(IInstallableUnitConstants.UPDATE_RANGE, "[1.0.0, 2.3.0)");
		c2 = createIU("C", new Version(2, 0, 0), NO_REQUIRES, properties, true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, c1});

		profile = new Profile("TestProfile." + getName());
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
		profile = new Profile("testInstallA1bis." + getName());
		director = createDirector();
		createTestMetdataRepository(new IInstallableUnit[] {a1, a2, c1, c2, b1});

		assertEquals(director.install(new IInstallableUnit[] {a1}, profile, null).getSeverity(), IStatus.OK);
	}
}
