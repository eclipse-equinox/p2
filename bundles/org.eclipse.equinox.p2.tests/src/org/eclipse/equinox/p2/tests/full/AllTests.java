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
package org.eclipse.equinox.p2.tests.full;

import junit.framework.*;

/**
 * Performs all automated download manager tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(End2EndTest.class);

		//TODO upgrade these tests to exercise 3.6->3.7 scenarios. See bug 324962
		//		suite.addTest(From35to36.suite());
		//		suite.addTest(Install36from35.suite());
		return suite;
	}

}
