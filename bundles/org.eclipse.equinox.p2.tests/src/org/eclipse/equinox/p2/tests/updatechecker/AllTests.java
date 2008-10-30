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
package org.eclipse.equinox.p2.tests.updatechecker;

import junit.framework.*;

/**
 * All automated tests for the org.eclipse.equinox.p2.updatechecker bundle
 */
public class AllTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(UpdateCheckerTest.class);
		return suite;
	}
}
