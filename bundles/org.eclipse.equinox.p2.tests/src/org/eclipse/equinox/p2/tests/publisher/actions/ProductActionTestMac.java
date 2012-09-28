/*******************************************************************************
 *  Copyright (c) 2008, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *   IBM - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import static org.easymock.EasyMock.*;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.okStatus;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collections;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.RootIUAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.equinox.p2.tests.TestData;

/**
 * Tests for {@link ProductAction} specific to Mac.
 */
@SuppressWarnings({"unchecked"})
public class ProductActionTestMac extends ActionTest {

	private File executablesFeatureLocation = null;
	private Capture<RootIUAdvice> rootIUAdviceCapture;
	private Capture<ProductFileAdvice> productFileAdviceCapture;
	private String source = "";

	@Override
	protected IPublisherInfo createPublisherInfoMock() {
		//override to create a nice mock, because we don't care about other method calls.
		return createNiceMock(IPublisherInfo.class);
	}

	protected void insertPublisherInfoBehavior() {
		publisherInfo.addAdvice(EasyMock.and(EasyMock.isA(RootIUAdvice.class), EasyMock.capture(rootIUAdviceCapture)));
		publisherInfo.addAdvice(EasyMock.and(EasyMock.isA(ProductFileAdvice.class), EasyMock.capture(productFileAdviceCapture)));
		//Return an empty list every time getAdvice is called
		expect(publisherInfo.getAdvice((String) anyObject(), anyBoolean(), (String) anyObject(), (Version) anyObject(), (Class) anyObject())).andReturn(Collections.emptyList());
		expectLastCall().anyTimes();
	}

	public void setUp() throws Exception {
		configSpec = AbstractPublisherAction.createConfigSpec("carbon", "macosx", "x86");
		rootIUAdviceCapture = new Capture<RootIUAdvice>();
		productFileAdviceCapture = new Capture<ProductFileAdvice>();
		setupPublisherInfo();
		setupPublisherResult();
	}

	/**
	 * Tests that correct advice is created for the org.eclipse.platform product.
	 */
	public void testPlatformProduct() throws Exception {
		ProductFile productFile = new ProductFile(TestData.getFile("ProductActionTest", "platform.product").toString());
		addContextIU("org.eclipse.platform.feature.group", "3.8.3");
		testAction = new ProductAction(source, productFile, flavorArg, executablesFeatureLocation);
		IStatus status = testAction.perform(publisherInfo, publisherResult, null);
		assertThat(status, is(okStatus()));

		IExecutableAdvice launchAdvice = productFileAdviceCapture.getValue();
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
