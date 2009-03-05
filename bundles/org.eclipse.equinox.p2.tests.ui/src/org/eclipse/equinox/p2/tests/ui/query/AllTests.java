/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import junit.framework.*;

/**
 * Performs all UI query tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(AnyRequiredCapabilityTest.class);
		suite.addTestSuite(AvailableIUWrapperTest.class);
		suite.addTestSuite(CategoryElementWrapperTest.class);
		suite.addTestSuite(IUPropertyUtilsTest.class);
		suite.addTestSuite(LatestIUVersionElementWrapperTest.class);
		suite.addTestSuite(QueryDescriptorTest.class);
		suite.addTestSuite(QueryableMetadataRepositoryManagerTest.class);
		return suite;
	}
}
