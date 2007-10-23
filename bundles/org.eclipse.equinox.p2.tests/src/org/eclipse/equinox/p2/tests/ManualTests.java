/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import junit.framework.*;

/**
 * This is the master test suite for all automated provisioning tests that require some
 * manual set. These tests can't be run automatically as part of a build.
 */
public class ManualTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite(AutomatedTests.class.getName());
		suite.addTestSuite(org.eclipse.equinox.p2.tests.full.DirectorTest.class);
		return suite;
	}
}
