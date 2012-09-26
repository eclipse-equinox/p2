/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.errorStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.tests.TestData;

/**
 * Test product publishing when the <code>type</code> attribute in the .product file
 * and how its semantics replaces the <code>useFeatures</code> attribute.
 * Verify that all the installable units, expected to be included in the product, are published as its requirements.
 */
public class ProductContentTypeTest extends ActionTest {

	private static final String TEST_DATA_FOLDER = "ProductContentTypeTest";
	private static final String flavor = "tooling";

	private List<IInstallableUnit> cusList;
	private IInstallableUnit featureIU = createIU("TestFeature.feature.group");
	private IInstallableUnit bundleIU = createIU("TestBundle");

	public void setUp() throws Exception {
		setupPublisherResult();
		initCUsList();
	}

	/**
	 * Publish product with attribute <code>type="bundles"</code>.
	 * Check that the generated product IU
	 * requires the default CU list + CU for the product + bundle IU.
	 */
	public void test01PublishWithBundle() throws Exception {
		testTemplate("ProductWithBundle.product", "1", cusList.size() + 2, bundleIU);
	}

	/**
	 * Publish product with attribute <code>type="features"</code>.
	 * Check that the generated product IU
	 * requires the default CU list + CU for the product + feature IU.
	 */
	public void test02PublishWithFeature() throws Exception {
		testTemplate("ProductWithFeature.product", "1", cusList.size() + 2, featureIU);
	}

	/**
	 * Publish product with attribute <code>type="mixed"</code>.
	 * Check that the generated product IU
	 * requires the default CU list + CU for the product + bundle IU + feature IU.
	 */
	public void test03PublishWithMixedContent() throws Exception {
		testTemplate("MixedContentProduct.product", "1", cusList.size() + 3, bundleIU, featureIU);
	}

	/**
	 * Publish product with invalid value for attribute <code>type</code>. 
	 */
	public void test04PublishWithInvalidContentType() throws Exception {
		File productFileLocation = TestData.getFile(TEST_DATA_FOLDER, "InvalidContentType.product");
		try {
			new ProductFile(productFileLocation.toString());
			fail("Parsing of product file with invalid content type was successful");
		} catch (IllegalArgumentException iae) {
			// success
		}
	}

	/**
	 * Publish product with attribute <code>type=""</code>.
	 */
	public void test05PublishWithEmptyContentType() throws Exception {
		File productFileLocation = TestData.getFile(TEST_DATA_FOLDER, "EmptyContentType.product");
		try {
			new ProductFile(productFileLocation.toString());
			fail("Parsing of product file with empty content type was successful");
		} catch (IllegalArgumentException iae) {
			// success
		}
	}

	/**
	 * Publish product with attributes <code>type="bundles"</code> and <code>useFeatures="true"</code>.
	 * Check that the generated product IU
	 * requires the default CU list + CU for the product + bundle IU. 
	 */
	public void test06OverrideUseFeaturesAttr() throws Exception {
		testTemplate("OverrideUseFeaturesAttr.product", "1", cusList.size() + 2, bundleIU);
	}

	/**
	 * Publish product with attributes <code>type="mixed"</code> and <code>useFeatures="true"</code>.
	 * Check that the generated product IU
	 * requires the default CU list + CU for the product + bundle IU + feature IU.
	 */
	public void test07OverrideUseFeaturesAttr2() throws Exception {
		testTemplate("OverrideUseFeaturesAttr2.product", "1", cusList.size() + 3, bundleIU, featureIU);
	}

	private void initCUsList() {
		cusList = new ArrayList<IInstallableUnit>();
		cusList.add(createIU(flavor + ".source.default"));
		cusList.add(createIU(flavor + ".osgi.bundle.default"));
		cusList.add(createIU(flavor + ".org.eclipse.update.feature.default"));
		cusList.add(createIU("a.jre.javase", Version.create("1.6.0")));
		cusList.add(createIU("config.a.jre.javase", Version.create("1.6.0")));
	}

	private void testTemplate(String productFileName, String productVersion, int expectedRequirementsSize, IInstallableUnit... requiredInstallableUnits) throws Exception {
		for (int i = 0; i < requiredInstallableUnits.length; i++) {
			publisherResult.addIU(requiredInstallableUnits[i], IPublisherResult.NON_ROOT);
		}

		File productFileLocation = TestData.getFile(TEST_DATA_FOLDER, productFileName);
		IInstallableUnit productIU = publishProduct(productFileLocation, productFileName);
		Collection<IRequirement> requirements = productIU.getRequirements();
		assertEquals("Requirements count doed not match", expectedRequirementsSize, requirements.size());
		verifyRequirementsForConfigurationUnits(requirements, productFileName, productVersion);
		for (IInstallableUnit iu : requiredInstallableUnits) {
			assertTrue("Installable unit " + iu.getId() + " is not included in the requirements", verifyRequirement(requirements, iu));
		}
	}

	private IInstallableUnit publishProduct(final File productFileLocation, final String productIUName) throws Exception {
		ProductFile productFile = new ProductFile(productFileLocation.toString());
		testAction = new ProductAction(null, productFile, flavor, null);
		IStatus status = testAction.perform(new PublisherInfo(), publisherResult, null);
		assertThat(status, is(not(errorStatus())));
		Collection<IInstallableUnit> ius = publisherResult.getIUs(productIUName, IPublisherResult.NON_ROOT);
		assertEquals(1, ius.size());
		return ius.iterator().next();
	}

	private void verifyRequirementsForConfigurationUnits(Collection<IRequirement> requirements, String productName, String productVersion) {

		List<IInstallableUnit> cusListCopy = new ArrayList<IInstallableUnit>(cusList);
		cusListCopy.add(createIU(flavor + productName + ".configuration", Version.create(productVersion)));
		for (Iterator<IInstallableUnit> cusIterator = cusListCopy.iterator(); cusIterator.hasNext();) {
			IInstallableUnit cu = cusIterator.next();
			if (verifyRequirement(requirements, cu)) {
				cusIterator.remove();
			}
		}

		assertTrue("Some of the default configuration units are not included in the product - " + cusListCopy, cusListCopy.isEmpty());
	}

	private boolean verifyRequirement(Collection<IRequirement> requirements, IInstallableUnit iu) {

		for (Iterator<IRequirement> iterator = requirements.iterator(); iterator.hasNext();) {
			IRequirement requirement = iterator.next();
			if (requirement.isMatch(iu)) {
				return true;
			}
		}

		return false;
	}
}