/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.generator;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.p2.metadata.generator.features.FeatureParser;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.Feature;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.MetadataGeneratorHelper;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class PatchIUGeneration extends AbstractProvisioningTest {

	public void testGeneratedIU() {
		FeatureParser parser = new FeatureParser();
		Feature feature = parser.parse(getTestData("org.eclipse.jdt.core.feature.patch", "testData/org.eclipse.jdt.3.2.1.patch_1.0.0.jar"));
		if (feature == null)
			fail();
		IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, true, null);
		IInstallableUnitPatch patchIU = (IInstallableUnitPatch) MetadataGeneratorHelper.createGroupIU(feature, featureIU, null, true);

		//Check id
		assertEquals(patchIU.getId(), "org.eclipse.jdt.3.2.1.patch.feature.group");

		//Check applicability scope
		assertEquals(IInstallableUnit.NAMESPACE_IU_ID, ((IRequiredCapability) patchIU.getApplicabilityScope()[0][0]).getNamespace());
		assertEquals("org.eclipse.jdt.feature.group", ((TestCase) patchIU.getApplicabilityScope()[0][0]).getName());
		assertEquals(new VersionRange("[3.2.1.r321_v20060905-R4CM1Znkvre9wC-,3.2.1.r321_v20060905-R4CM1Znkvre9wC-]"), ((IRequiredCapability) patchIU.getApplicabilityScope()[0][0]).getRange());

		assertEquals("org.eclipse.jdt.core", patchIU.getRequirementsChange().get(0).applyOn().getName());
		assertEquals(VersionRange.emptyRange, patchIU.getRequirementsChange().get(0).applyOn().getRange());
		assertEquals("org.eclipse.jdt.core", patchIU.getRequirementsChange().get(0).newValue().getName());
		assertEquals(new VersionRange("[3.2.2,3.2.2]"), patchIU.getRequirementsChange().get(0).newValue().getRange());
		assertTrue(QueryUtil.isPatch(patchIU));
		assertEquals(1, patchIU.getRequirements().size());
		assertEquals(featureIU.getId(), ((IRequiredCapability) patchIU.getRequirements().iterator().next()).getName());
		assertEquals("org.eclipse.jdt.feature.group", ((IRequiredCapability) patchIU.getLifeCycle()).getName());
		assertFalse(patchIU.getLifeCycle().isGreedy());
		assertFalse(patchIU.getLifeCycle().getMin() == 0);
	}
}
