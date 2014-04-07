/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import java.io.File;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.*;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.PreviousConfigurationFinder.ConfigurationDescriptor;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.PreviousConfigurationFinder.Identifier;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PreviousConfigurationFinderTest extends AbstractProvisioningTest {

	private ConfigurationDescriptor referenceConfiguration;

	@Override
	protected void setUp() throws Exception {
		referenceConfiguration = new ConfigurationDescriptor("org.eclipse.platform", new Identifier(4, 3, 0), "12345678", "win32_win32_x86", null);
	}

	public void testUpdateToNewBuildInPlace_sameProduct() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testUpdateToNewBuildInPlace/sameProduct");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromInstallHashDir(configs, referenceConfiguration);
		assertEquals("org.eclipse.platform", match.getProductId());
		assertEquals(new Identifier(4, 2, 1), match.getVersion());
	}

	public void testUpdateToNewBuildInPlace_mixedProducts() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testUpdateToNewBuildInPlace/mixedProducts");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromInstallHashDir(configs, referenceConfiguration);
		assertEquals("org.eclipse.platform", match.getProductId());
		assertEquals(new Identifier(4, 2, 0), match.getVersion());
	}

	public void testUpdateToNewBuildInPlace_differentProduct() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testUpdateToNewBuildInPlace/differentProduct");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromInstallHashDir(configs, referenceConfiguration);
		assertEquals("org.eclipse.epp.jee", match.getProductId());
		assertEquals(new Identifier(1, 5, 1), match.getVersion());
	}

	public void testUpdateToNewBuildInPlace_noMatch() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testUpdateToNewBuildInPlace/noMatch");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromInstallHashDir(configs, referenceConfiguration);
		assertNull(match);
	}

	//
	public void testNewBuildInDifferentFolder_sameProduct() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testNewBuildInDifferentFolder/sameProduct");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromProductId(configs, referenceConfiguration);
		assertEquals("org.eclipse.platform", match.getProductId());
		assertEquals(new Identifier(4, 2, 1), match.getVersion());
	}

	public void testNewBuildInDifferentFolder_sameProductWithSameVersion() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testNewBuildInDifferentFolder/sameProductWithSameVersion");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromProductId(configs, referenceConfiguration);
		assertEquals("org.eclipse.platform", match.getProductId());
		assertEquals(new Identifier(4, 3, 0), match.getVersion());
	}

	public void testNewBuildInDifferentFolder_noMatch() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testNewBuildInDifferentFolder/noMatch");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromProductId(configs, referenceConfiguration);
		assertNull(match);
	}

	public void testNewBuildInDifferentFolder_mixedProducts() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testNewBuildInDifferentFolder/mixedProducts");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromProductId(configs, referenceConfiguration);
		assertEquals("org.eclipse.platform", match.getProductId());
		assertEquals(new Identifier(4, 2, 0), match.getVersion());
	}

	public void testNewBuildInDifferentFolder_differentConfigurations() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testNewBuildInDifferentFolder/differentConfigurations");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromProductId(configs, referenceConfiguration);
		assertEquals("org.eclipse.platform", match.getProductId());
		assertEquals(new Identifier(4, 1, 0), match.getVersion());
	}

	public void testLegacyFormat() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testLegacyFormat");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromProductId(configs, referenceConfiguration);
		assertEquals("org.eclipse.platform", match.getProductId());
		assertEquals(new Identifier(3, 8, 1), match.getVersion());
		assertNull(match.getPlatformConfig());

	}

	public void testNoMatchFromSucceedingVersion() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testSuccedingVersion/noMatch");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromProductId(configs, referenceConfiguration);
		assertNull(match);
	}

	public void testMatchFromPreceedingVersionWithDifferentPaltform() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testSuccedingVersion/matchFromSuccedingAndPreccedingWithDifferentPlatfrom");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromProductId(configs, referenceConfiguration);
		assertNotNull(match);
		assertEquals(new Identifier(3, 8, 0), match.getVersion());
		assertEquals("linux-gtk-x86_64", match.getPlatformConfig());
	}

	public void testMatchFromPreceedingVersionWithNullPaltform() throws Exception {
		File configFolder = getTestData("sameProduct", "testData/previousConfigurationFinder/testSuccedingVersion/matchFromSuccedingAndPreccedingWithNullPlatfrom");
		List<ConfigurationDescriptor> configs = new PreviousConfigurationFinder(configFolder).readPreviousConfigurations(configFolder);
		ConfigurationDescriptor match = new PreviousConfigurationFinder(configFolder).findMostRelevantConfigurationFromProductId(configs, referenceConfiguration);
		assertNotNull(match);
		assertEquals(new Identifier(3, 8, 0), match.getVersion());
		assertNull(match.getPlatformConfig());
	}

}
