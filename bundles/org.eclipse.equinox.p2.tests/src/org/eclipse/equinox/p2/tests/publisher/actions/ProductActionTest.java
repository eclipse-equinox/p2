///*******************************************************************************
// * Copyright (c) 2008 Code 9 and others. All rights reserved. This
// * program and the accompanying materials are made available under the terms of
// * the Eclipse Public License v1.0 which accompanies this distribution, and is
// * available at http://www.eclipse.org/legal/epl-v10.html
// * 
// * Contributors: 
// *   Code 9 - initial API and implementation
// ******************************************************************************/
//package org.eclipse.equinox.p2.tests.publisher.actions;
//
//import java.io.File;
//import org.easymock.Capture;
//import org.easymock.EasyMock;
//import org.eclipse.equinox.p2.publisher.actions.RootIUAdvice;
//import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
//import org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice;
//import org.junit.Before;
//import org.junit.Test;
//
//@SuppressWarnings( {"restriction", "unchecked"})
//public class ProductActionTest extends ActionTest {
//
//	ProductAction testAction;
//	Capture<RootIUAdvice> rootIUAdviceCapture;
//	Capture<ProductFileAdvice> productFileAdviceCapture;
//	File executablesFeatureLocation = null;
//	String productLocation = "";
//	String source = "";
//
//	@Before
//	public void setUp() throws Exception {
//		rootIUAdviceCapture = new Capture<RootIUAdvice>();
//		setupPublisherInfo();
//		setupPublisherResult();
//		testAction = new ProductAction(source, productLocation, flavorArg, executablesFeatureLocation);
//	}
//
//	@Test
//	public void testProductAction() throws Exception {
//		testAction.perform(publisherInfo, publisherResult);
//		verifyAction();
//		debug("Completed ProductActionTest."); //$NON-NLS-1$
//	}
//
//	private void verifyAction() {
//		RootIUAdvice captured = (RootIUAdvice) rootIUAdviceCapture.getValue();
//		//TODO: verify stuff
//	}
//
//	protected void insertPublisherInfoBehavior() {
//		publisherInfo.addAdvice(EasyMock.and(EasyMock.isA(RootIUAdvice.class), EasyMock.capture(rootIUAdviceCapture)));
//		publisherInfo.addAdvice(EasyMock.and(EasyMock.isA(ProductFileAdvice.class), EasyMock.capture(productFileAdviceCapture)));
//	}
//}
