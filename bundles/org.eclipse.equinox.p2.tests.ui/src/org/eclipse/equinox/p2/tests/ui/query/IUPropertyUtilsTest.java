/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import java.io.File;
import java.net.URI;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.ui.IUPropertyUtils;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Tests for {@link IUPropertyUtils}.
 */
public class IUPropertyUtilsTest extends AbstractQueryTest {
	public void testFeatureProperties() {
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
		File site = getTestData("0.1", "/testData/metadataRepo/externalized");
		URI location = site.toURI();
		IMetadataRepository repository;
		try {
			repository = repoMan.loadRepository(location, getMonitor());
		} catch (ProvisionException e) {
			fail("1.99", e);
			return;
		}
		Collector result = repository.query(new InstallableUnitQuery("test.feature.feature.group"), new Collector(), getMonitor());
		assertTrue("1.0", !result.isEmpty());
		IInstallableUnit unit = (IInstallableUnit) result.iterator().next();

		Copyright copyright = org.eclipse.equinox.internal.provisional.p2.ui.IUPropertyUtils.getCopyright(unit);
		assertEquals("1.1", "Test Copyright", copyright.getBody());
		License license = IUPropertyUtils.getLicense(unit);
		assertEquals("1.2", "Test License", license.getBody());
		//		assertEquals("1.3", "license.html", license.getURL().toExternalForm());
		String name = IUPropertyUtils.getIUProperty(unit, IInstallableUnit.PROP_NAME);
		assertEquals("1.4", "Test Feature Name", name);
		String description = IUPropertyUtils.getIUProperty(unit, IInstallableUnit.PROP_DESCRIPTION);
		assertEquals("1.5", "Test Description", description);
		String provider = IUPropertyUtils.getIUProperty(unit, IInstallableUnit.PROP_PROVIDER);
		assertEquals("1.6", "Test Provider Name", provider);
	}

	public void testBasicIU() {
		IInstallableUnit unit = createIU("f1");

		assertNull("1.1", IUPropertyUtils.getCopyright(unit));
		assertNull("1.2", IUPropertyUtils.getLicense(unit));
		assertNull("1.3", IUPropertyUtils.getIUProperty(unit, IInstallableUnit.PROP_NAME));
		assertNull("1.4", IUPropertyUtils.getIUProperty(unit, IInstallableUnit.PROP_DESCRIPTION));
		assertNull("1.5", IUPropertyUtils.getIUProperty(unit, IInstallableUnit.PROP_PROVIDER));
	}
}
