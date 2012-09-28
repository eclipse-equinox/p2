/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 *   SAP AG - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher;

import junit.framework.*;
import org.eclipse.equinox.p2.tests.publisher.actions.*;

public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(AbstractPublisherActionTest.class);
		suite.addTestSuite(AccumulateConfigDataActionTest.class);
		suite.addTestSuite(AdviceFileParserTest.class);
		suite.addTestSuite(ANYConfigCUsActionTest.class);
		suite.addTestSuite(BundlesActionTest.class);
		suite.addTestSuite(CategoryPublisherTest.class);
		suite.addTestSuite(ConfigCUsActionTest.class);
		suite.addTestSuite(ContextRepositoryTest.class);
		suite.addTestSuite(DefaultCUsActionTest.class);
		suite.addTestSuite(EquinoxExecutableActionTest.class);
		suite.addTestSuite(EquinoxLauncherCUActionTest.class);
		suite.addTestSuite(FeaturesActionTest.class);
		suite.addTestSuite(JREActionTest.class);
		suite.addTestSuite(LocalizationTests.class);
		suite.addTestSuite(LocalUpdateSiteActionTest.class);
		suite.addTestSuite(MD5GenerationTest.class);
		suite.addTestSuite(ProductActionTest.class);
		suite.addTestSuite(ProductActionCapturingTest.class);
		suite.addTestSuite(ProductActionTestMac.class);
		suite.addTestSuite(ProductActionWithJRELocationTest.class);
		suite.addTestSuite(ProductContentTypeTest.class);
		suite.addTestSuite(ProductFileAdviceTest.class);
		suite.addTestSuite(ProductFileTest.class);
		suite.addTestSuite(RootFilesActionTest.class);
		suite.addTestSuite(RootIUActionTest.class);
		suite.addTestSuite(GeneralPublisherTests.class);
		suite.addTestSuite(VersionAdviceTest.class);
		return suite;
	}

}