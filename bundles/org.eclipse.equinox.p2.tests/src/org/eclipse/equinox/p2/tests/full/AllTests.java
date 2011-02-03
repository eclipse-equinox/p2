/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import junit.framework.*;

/**
 * Performs all automated download manager tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(End2EndTest.class);
		//		suite.addTest(From35to36.suite());
		//		suite.addTest(Install36from35.suite());
		suite.addTest(From36to37.suite());
		suite.addTest(Install37from36.suite());
		return suite;
	}

}
