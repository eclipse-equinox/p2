/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher;

import junit.framework.*;
import org.eclipse.equinox.p2.tests.publisher.actions.*;

public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(RootIUActionTest.class);
		suite.addTestSuite(DefaultCUsActionTest.class);
		suite.addTestSuite(BundlesActionTest.class);
		suite.addTestSuite(RootFilesActionTest.class);
		suite.addTestSuite(FeaturesActionTest.class);
		suite.addTestSuite(AccumulateConfigDataActionTest.class);
		suite.addTestSuite(JREActionTest.class);
		suite.addTestSuite(ConfigCUsActionTest.class);
		suite.addTestSuite(EquinoxLauncherCUActionTest.class);
		suite.addTestSuite(EquinoxExecutableActionTest.class);
		suite.addTestSuite(MD5GenerationTest.class);
		return suite;
	}

}