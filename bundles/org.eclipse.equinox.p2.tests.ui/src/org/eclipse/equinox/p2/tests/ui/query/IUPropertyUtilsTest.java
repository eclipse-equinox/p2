/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - bug fixing
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
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

		ICopyright copyright = org.eclipse.equinox.internal.provisional.p2.ui.IUPropertyUtils.getCopyright(unit);
		assertEquals("1.1", "Test Copyright", copyright.getBody());
		ILicense license = IUPropertyUtils.getLicense(unit);
		assertEquals("1.2", "Test License", license.getBody());
		//		assertEquals("1.3", "license.html", license.getURL().toExternalForm());
		String name = IUPropertyUtils.getIUProperty(unit, IInstallableUnit.PROP_NAME);
		assertEquals("1.4", "Test Feature Name", name);
		String description = IUPropertyUtils.getIUProperty(unit, IInstallableUnit.PROP_DESCRIPTION);
		assertEquals("1.5", "Test Description", description);
		String provider = IUPropertyUtils.getIUProperty(unit, IInstallableUnit.PROP_PROVIDER);
		assertEquals("1.6", "Test Provider Name", provider);
	}

	public void testLocalizedLicense() throws URISyntaxException {
		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		Profile profile = (Profile) profileRegistry.getProfile(IProfileRegistry.SELF);
		profileRegistry.lockProfile(profile);
		String germanLicense = "German License";
		String canadianFRLicense = "Canadian French License";

		// Create a IU that has a license, but the license body is simply %license. This will be filled in by 
		// a fragment
		InstallableUnitDescription iuDescription = new InstallableUnitDescription();
		iuDescription.setId("some IU");
		iuDescription.setVersion(Version.createOSGi(1, 0, 0));
		iuDescription.setLicense(MetadataFactory.createLicense(new URI("http://example.com"), "%license"));
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		// Create a bunch of fragments which spec our IU as their host
		// These fragments don't contribute language information
		for (int i = 0; i < 10; i++) {
			InstallableUnitFragmentDescription installableUnitFragmentDescription = new InstallableUnitFragmentDescription();
			installableUnitFragmentDescription.setId("fragment number: " + i);
			installableUnitFragmentDescription.setVersion(Version.createOSGi(1, 0, 0));
			installableUnitFragmentDescription.setHost(new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "some IU", ANY_VERSION, null, false, false)});
			installableUnitFragmentDescription.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, "true");
			IInstallableUnitFragment iuFragment = MetadataFactory.createInstallableUnitFragment(installableUnitFragmentDescription);
			profile.addInstallableUnit(iuFragment);
		}

		// Create fragment with a German license
		InstallableUnitFragmentDescription installableUnitFragmentDescription = new InstallableUnitFragmentDescription();
		IProvidedCapability providedCapability = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.localization", "de", Version.createOSGi(1, 0, 0));
		ArrayList list = new ArrayList();
		list.add(providedCapability);
		installableUnitFragmentDescription.addProvidedCapabilities(list);
		installableUnitFragmentDescription.setId("german fragment");
		installableUnitFragmentDescription.setVersion(Version.createOSGi(1, 0, 0));
		installableUnitFragmentDescription.setHost(new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "some IU", ANY_VERSION, null, false, false)});
		installableUnitFragmentDescription.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, "true");
		installableUnitFragmentDescription.setProperty("de.license", germanLicense);
		IInstallableUnitFragment iuFragment = MetadataFactory.createInstallableUnitFragment(installableUnitFragmentDescription);
		profile.addInstallableUnit(iuFragment);

		// Create a French fragment with an fr_CA license
		installableUnitFragmentDescription = new InstallableUnitFragmentDescription();
		providedCapability = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.localization", "fr", Version.createOSGi(1, 0, 0));
		list = new ArrayList();
		list.add(providedCapability);
		installableUnitFragmentDescription.addProvidedCapabilities(list);
		installableUnitFragmentDescription.setId("cnd french fragment");
		installableUnitFragmentDescription.setVersion(Version.createOSGi(1, 0, 0));
		installableUnitFragmentDescription.setHost(new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "some IU", ANY_VERSION, null, false, false)});
		installableUnitFragmentDescription.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, "true");
		installableUnitFragmentDescription.setProperty("fr_CA.license", canadianFRLicense);
		iuFragment = MetadataFactory.createInstallableUnitFragment(installableUnitFragmentDescription);

		profile.addInstallableUnit(iuFragment);
		profile.addInstallableUnit(iu);

		profileRegistry.updateProfile(profile);
		profileRegistry.unlockProfile(profile);

		ILicense license = IUPropertyUtils.getLicense(iu, Locale.GERMAN);
		assertEquals("1.0", germanLicense, license.getBody());
		license = IUPropertyUtils.getLicense(iu, Locale.CANADA_FRENCH);
		assertEquals("1.1", canadianFRLicense, license.getBody());
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
