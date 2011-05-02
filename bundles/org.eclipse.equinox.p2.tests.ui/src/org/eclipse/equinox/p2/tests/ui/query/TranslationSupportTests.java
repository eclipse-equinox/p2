/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Tests for {@link TranslationSupport}.
 */
public class TranslationSupportTests extends AbstractQueryTest {
	Profile profile;
	IQueryable oldTranslationSource;

	protected void setUp() throws Exception {
		super.setUp();
		profile = (Profile) createProfile("testLocalizedLicense");
		oldTranslationSource = TranslationSupport.getInstance().setTranslationSource(profile);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		TranslationSupport.getInstance().setTranslationSource(oldTranslationSource);
	}

	public void testFeatureProperties() {
		IMetadataRepositoryManager repoMan = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		File site = getTestData("0.1", "/testData/metadataRepo/externalized");
		URI location = site.toURI();
		IMetadataRepository repository;
		try {
			repository = repoMan.loadRepository(location, getMonitor());
		} catch (ProvisionException e) {
			fail("1.99", e);
			return;
		}
		IQueryResult result = repository.query(QueryUtil.createIUQuery("test.feature.feature.group"), getMonitor());
		assertTrue("1.0", !result.isEmpty());
		IInstallableUnit unit = (IInstallableUnit) result.iterator().next();

		ICopyright copyright = unit.getCopyright(null);
		assertEquals("1.1", "Test Copyright", copyright.getBody());
		ILicense license = unit.getLicenses(null).iterator().next();
		assertEquals("1.2", "Test License", license.getBody());
		//		assertEquals("1.3", "license.html", license.getURL().toExternalForm());
		String name = unit.getProperty(IInstallableUnit.PROP_NAME, null);
		assertEquals("1.4", "Test Feature Name", name);
		String description = unit.getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
		assertEquals("1.5", "Test Description", description);
		String provider = unit.getProperty(IInstallableUnit.PROP_PROVIDER, null);
		assertEquals("1.6", "Test Provider Name", provider);
	}

	public void testLocalizedLicense() throws URISyntaxException {
		String germanLicense = "German License";
		String canadianFRLicense = "Canadian French License";

		// Create a IU that has a license, but the license body is simply %license. This will be filled in by 
		// a fragment
		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription iuDescription = new org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription();
		iuDescription.setId("some IU");
		iuDescription.setVersion(Version.createOSGi(1, 0, 0));
		iuDescription.setLicenses(new ILicense[] {MetadataFactory.createLicense(new URI("http://example.com"), "%license")});
		iuDescription.addProvidedCapabilities(Collections.singleton(MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "some IU", Version.createOSGi(1, 0, 0))));
		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iuDescription);

		// Create a bunch of fragments which spec our IU as their host
		// These fragments don't contribute language information
		for (int i = 0; i < 10; i++) {
			org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription installableUnitFragmentDescription = new org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription();
			installableUnitFragmentDescription.setId("fragment number: " + i);
			installableUnitFragmentDescription.setVersion(Version.createOSGi(1, 0, 0));
			installableUnitFragmentDescription.setHost(new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "some IU", ANY_VERSION, null, false, false)});
			installableUnitFragmentDescription.setProperty(org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription.PROP_TYPE_FRAGMENT, "true");
			IInstallableUnitFragment iuFragment = MetadataFactory.createInstallableUnitFragment(installableUnitFragmentDescription);
			profile.addInstallableUnit(iuFragment);
		}

		// Create fragment with a German license
		org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription installableUnitFragmentDescription = new org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription();
		IProvidedCapability providedCapability = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.localization", "de", Version.createOSGi(1, 0, 0));
		ArrayList list = new ArrayList();
		list.add(providedCapability);
		installableUnitFragmentDescription.addProvidedCapabilities(list);
		installableUnitFragmentDescription.setId("german fragment");
		installableUnitFragmentDescription.setVersion(Version.createOSGi(1, 0, 0));
		installableUnitFragmentDescription.setHost(new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "some IU", ANY_VERSION, null, false, false)});
		installableUnitFragmentDescription.setProperty(org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription.PROP_TYPE_FRAGMENT, "true");
		installableUnitFragmentDescription.setProperty("de.license", germanLicense);
		IInstallableUnitFragment iuFragment = MetadataFactory.createInstallableUnitFragment(installableUnitFragmentDescription);
		profile.addInstallableUnit(iuFragment);

		// Create a French fragment with an fr_CA license
		installableUnitFragmentDescription = new org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription();
		providedCapability = MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.localization", "fr", Version.createOSGi(1, 0, 0));
		list = new ArrayList();
		list.add(providedCapability);
		installableUnitFragmentDescription.addProvidedCapabilities(list);
		installableUnitFragmentDescription.setId("cnd french fragment");
		installableUnitFragmentDescription.setVersion(Version.createOSGi(1, 0, 0));
		installableUnitFragmentDescription.setHost(new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "some IU", ANY_VERSION, null, false, false)});
		installableUnitFragmentDescription.setProperty(org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription.PROP_TYPE_FRAGMENT, "true");
		installableUnitFragmentDescription.setProperty("fr_CA.license", canadianFRLicense);
		iuFragment = MetadataFactory.createInstallableUnitFragment(installableUnitFragmentDescription);

		profile.addInstallableUnit(iuFragment);
		profile.addInstallableUnit(iu);

		ILicense license = iu.getLicenses(Locale.GERMAN.toString()).iterator().next();
		assertEquals("1.0", germanLicense, license.getBody());
		license = iu.getLicenses(Locale.CANADA_FRENCH.toString()).iterator().next();
		assertEquals("1.1", canadianFRLicense, license.getBody());
	}

	public void testBasicIU() {
		IInstallableUnit unit = createIU("f1");

		assertNull("1.1", unit.getCopyright(null));
		assertEquals("1.2", 0, unit.getLicenses(null).size());
		assertNull("1.3", unit.getProperty(IInstallableUnit.PROP_NAME, null));
		assertNull("1.4", unit.getProperty(IInstallableUnit.PROP_DESCRIPTION, null));
		assertNull("1.5", unit.getProperty(IInstallableUnit.PROP_PROVIDER, null));
	}
}
