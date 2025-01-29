/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *   IBM - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.okStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.IExecutableAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice;
import org.eclipse.equinox.p2.tests.TestData;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Tests for {@link ProductAction} specific to Mac.
 */
public class ProductActionTestMac extends ActionTest {

	private final File executablesFeatureLocation = null;
	private final String source = "";

	@Override
	public void setUp() throws Exception {
		configSpec = AbstractPublisherAction.createConfigSpec("carbon", "macosx", "x86");
		setupPublisherInfo();
		setupPublisherResult();
	}

	/**
	 * Tests that correct advice is created for the org.eclipse.platform product.
	 */
	public void testPlatformProduct() throws Exception {
		ArgumentCaptor<IPublisherAdvice> productFileAdviceCapture = ArgumentCaptor.forClass(IPublisherAdvice.class);
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "platform.product").toString());
		addContextIU("org.eclipse.platform.feature.group", "3.8.3");
		testAction = Mockito.spy(new ProductAction(source, productFile, flavorArg, executablesFeatureLocation));
		IStatus status = testAction.perform(publisherInfo, publisherResult, null);
		verify(publisherInfo, Mockito.atLeastOnce()).addAdvice(productFileAdviceCapture.capture());
		assertThat(status, is(okStatus()));

		IExecutableAdvice launchAdvice = (IExecutableAdvice) productFileAdviceCapture.getAllValues().stream()
				.filter(ProductFileAdvice.class::isInstance).collect(Collectors.toList()).get(0);
		assertEquals("1.0", "eclipse", launchAdvice.getExecutableName());

		String[] programArgs = launchAdvice.getProgramArguments();
		assertEquals("2.0", 2, programArgs.length);
		assertEquals("2.1", "-showsplash", programArgs[0]);
		assertEquals("2.2", "org.eclipse.platform", programArgs[1]);

		String[] vmArgs = launchAdvice.getVMArguments();
		assertEquals("3.0", 6, vmArgs.length);
		assertEquals("3.1", "-Xdock:icon=../Resources/Eclipse.icns", vmArgs[0]);
		assertEquals("3.2", "-XstartOnFirstThread", vmArgs[1]);
		assertEquals("3.3", "-Xms40m", vmArgs[2]);
		assertEquals("3.4", "-Xmx256m", vmArgs[3]);
		assertEquals("3.5", "-XX:MaxPermSize=256m", vmArgs[4]);
		assertEquals("3.6", "-Dorg.eclipse.swt.internal.carbon.smallFonts", vmArgs[5]);
	}
}
