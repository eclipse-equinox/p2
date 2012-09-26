/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   SAP AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.okStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

/**
 * Tests the new ProductAction constructor which accepts a file path to an external JRE location or an external java profile.
 * The goal is to verify that the new constructors leads to the creation of a JRE unit, based on the external profile.
 * For that purpose, the checks are overall quite basic, limited mainly to verifying the existence of the JRE unit and
 * the provided java packages in the result p2 metadata. The in-depth analysis of the JRE unit properties is delegated to
 * the corresponding new tests in o.e.e.p2.test.publisher.actions.JREActionTest.
 */
public class ProductActionWithJRELocationTest extends AbstractProvisioningTest {

	public void testWithJRELocationFolder() throws Exception {
		File productFileLocation = TestData.getFile("ProductActionTest", "productWithLicense.product");
		File jreLocation = TestData.getFile("JREActionTest", "packageVersions");
		List<IProvidedCapability> expectedProvidedCapabilities = new ArrayList<IProvidedCapability>();
		expectedProvidedCapabilities.add(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", null));
		expectedProvidedCapabilities.add(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", Version.create("1.0.0")));
		testTemplate(productFileLocation, jreLocation, "a.jre.test", Version.create("1.0.0"), expectedProvidedCapabilities);
	}

	public void testWithJREProfile() throws Exception {
		File productFileLocation = TestData.getFile("ProductActionTest", "productWithLicense.product");
		File jreLocation = TestData.getFile("JREActionTest", "packageVersions/test-1.0.0.profile");
		List<IProvidedCapability> expectedProvidedCapabilities = new ArrayList<IProvidedCapability>();
		expectedProvidedCapabilities.add(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", null));
		expectedProvidedCapabilities.add(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, "my.package", Version.create("1.0.0")));
		testTemplate(productFileLocation, jreLocation, "a.jre.test", Version.create("1.0.0"), expectedProvidedCapabilities);
	}

	private void testTemplate(File productFileLocation, File jreLocation, String jreIuName, Version jreIuVersion, Collection<IProvidedCapability> expectedProvidedCapabilities) throws Exception {

		ProductFile productFile = new ProductFile(productFileLocation.getCanonicalPath());
		ProductAction testAction = new ProductAction(null, productFile, "tooling", null, jreLocation);
		IPublisherResult publisherResult = new PublisherResult();
		IStatus status = testAction.perform(new PublisherInfo(), publisherResult, null);
		assertThat(status, is(okStatus()));

		Collection<IInstallableUnit> ius = publisherResult.getIUs(jreIuName, IPublisherResult.ROOT);
		assertEquals(1, ius.size());

		IInstallableUnit jreIU = ius.iterator().next();
		assertEquals(jreIuVersion, jreIU.getVersion());

		Collection<IProvidedCapability> providedCapabilities = jreIU.getProvidedCapabilities();
		for (IProvidedCapability expectedProvidedCapability : expectedProvidedCapabilities) {
			assertTrue("Expected capability " + expectedProvidedCapability + " was not found in published JRE", verifyProvidedCapabilities(providedCapabilities, expectedProvidedCapability));
		}
	}

	private boolean verifyProvidedCapabilities(Collection<IProvidedCapability> providedCapabilities, IProvidedCapability expectedProvidedCapability) {
		for (IProvidedCapability iProvidedCapability : providedCapabilities) {
			if (iProvidedCapability.equals(expectedProvidedCapability))
				return true;
		}
		return false;
	}
}
