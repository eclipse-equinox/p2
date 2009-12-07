/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.generator;

import org.eclipse.equinox.internal.p2.metadata.generator.features.FeatureParser;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.Feature;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.MetadataGeneratorHelper;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class FeatureToIU extends AbstractProvisioningTest {

	public void testDescription() {
		Feature f = new FeatureParser().parse(getTestData("FFF 1.0.0", "testData/featureToIU/feature1.jar"));
		assertNotNull(f);
		IInstallableUnit group = MetadataGeneratorHelper.createGroupIU(f, MetadataGeneratorHelper.createFeatureJarIU(f, false));
		assertEquals(f.getDescription(), group.getProperty(IInstallableUnit.PROP_DESCRIPTION));
		assertEquals(f.getDescriptionURL(), group.getProperty(IInstallableUnit.PROP_DESCRIPTION_URL));
	}

	public void testNoDescription1() {
		Feature f = new FeatureParser().parse(getTestData("FFF 1.0.0", "testData/featureToIU/feature2.jar"));
		assertNotNull(f);
		IInstallableUnit group = MetadataGeneratorHelper.createGroupIU(f, MetadataGeneratorHelper.createFeatureJarIU(f, false));
		assertEquals(f.getDescription(), group.getProperty(IInstallableUnit.PROP_DESCRIPTION));
		assertEquals(f.getDescriptionURL(), group.getProperty(IInstallableUnit.PROP_DESCRIPTION_URL));
	}

	public void testNoDescription2() {
		Feature f = new FeatureParser().parse(getTestData("FFF 1.0.0", "testData/featureToIU/feature3.jar"));
		assertNotNull(f);
		IInstallableUnit group = MetadataGeneratorHelper.createGroupIU(f, MetadataGeneratorHelper.createFeatureJarIU(f, false));
		assertEquals(f.getDescription(), group.getProperty(IInstallableUnit.PROP_DESCRIPTION));
		assertEquals(f.getDescriptionURL(), group.getProperty(IInstallableUnit.PROP_DESCRIPTION_URL));
	}

}
