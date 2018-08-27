/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
