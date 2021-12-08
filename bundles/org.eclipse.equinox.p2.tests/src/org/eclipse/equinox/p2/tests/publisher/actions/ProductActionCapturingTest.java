/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   SAP AG - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.okStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.IConfigAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.IExecutableAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.publisher.TestArtifactRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ProductActionCapturingTest extends ActionTest {

	File executablesFeatureLocation = null;
	String source = "";

	protected TestArtifactRepository artifactRepository = new TestArtifactRepository(getAgent());

	@Override
	public void setUp() throws Exception {
		setupPublisherInfo();
		setupPublisherResult();
	}

	/**
	 * Tests that a product file containing bundle configuration data produces appropriate
	 * IConfigAdvice (start levels, auto-start).
	 */
	public void testSetBundleConfigData() throws Exception {
		ArgumentCaptor<IPublisherAdvice> productFileAdviceCapture = ArgumentCaptor.forClass(IPublisherAdvice.class);
		addContextIU("org.eclipse.rcp.feature.group", "3.5.0.v20081110-9C9tEvNEla71LZ2jFz-RFB-t");

		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "startLevel.product").toString());
		testAction = Mockito.spy(new ProductAction(source, productFile, flavorArg, executablesFeatureLocation));

		IStatus status = testAction.perform(publisherInfo, publisherResult, null);
		verify(publisherInfo, Mockito.atLeastOnce()).addAdvice(productFileAdviceCapture.capture());
		assertThat(status, is(okStatus()));

		IConfigAdvice configAdvice = (IConfigAdvice) productFileAdviceCapture.getAllValues().stream()
				.filter(IConfigAdvice.class::isInstance).collect(Collectors.toList()).get(0);
		BundleInfo[] bundles = configAdvice.getBundles();
		assertEquals("1.0", 2, bundles.length);
		assertEquals("1.1", "org.eclipse.equinox.common", bundles[0].getSymbolicName());
		assertEquals("1.2", "1.0.0", bundles[0].getVersion());
		assertEquals("1.3", 13, bundles[0].getStartLevel());
		assertEquals("1.4", false, bundles[0].isMarkedAsStarted());

		assertEquals("2.1", "org.eclipse.core.runtime", bundles[1].getSymbolicName());
		assertEquals("2.2", "2.0.0", bundles[1].getVersion());
		assertEquals("2.3", 6, bundles[1].getStartLevel());
		assertEquals("2.4", true, bundles[1].isMarkedAsStarted());
	}

	/**
	 * Tests that correct advice is created for the org.eclipse.platform product.
	 */
	public void testPlatformProduct() throws Exception {
		ArgumentCaptor<IPublisherAdvice> productFileAdviceCapture = ArgumentCaptor.forClass(IPublisherAdvice.class);
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "platform.product").toString());
		addContextIU("org.eclipse.platform.feature.group", "1.2.3");

		testAction = Mockito.spy(new ProductAction(source, productFile, flavorArg, executablesFeatureLocation));
		IStatus status = testAction.perform(publisherInfo, publisherResult, null);
		verify(publisherInfo, Mockito.atLeastOnce()).addAdvice(productFileAdviceCapture.capture());
		assertThat(status, is(okStatus()));

		IExecutableAdvice launchAdvice = (IExecutableAdvice) productFileAdviceCapture.getAllValues().stream()
				.filter(ProductFileAdvice.class::isInstance).collect(Collectors.toList()).get(0);
		assertEquals("1.0", "eclipse", launchAdvice.getExecutableName());

		String[] programArgs = launchAdvice.getProgramArguments();
		assertEquals("2.0", 0, programArgs.length);

		String[] vmArgs = launchAdvice.getVMArguments();
		assertEquals("3.0", 0, vmArgs.length);

	}

}
