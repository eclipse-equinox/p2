/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import junit.framework.*;

/**
 * Performs all automated core tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(AggregateQueryTest.class);
		suite.addTestSuite(BackupTest.class);
		suite.addTestSuite(CollectorTest.class);
		suite.addTestSuite(CompoundQueryableTest.class);
		suite.addTestSuite(FileUtilsTest.class);
		suite.addTestSuite(OrderedPropertiesTest.class);
		suite.addTestSuite(ProvisioningAgentTest.class);
		suite.addTestSuite(QueryTest.class);
		suite.addTestSuite(URLUtilTest.class);
		return suite;
	}

}
