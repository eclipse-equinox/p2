/*******************************************************************************
 * Copyright (c) 2009, 2018 Cloudsmith Inc and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Cloudsmith Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.repository;

import junit.framework.*;

/**
 * Performs all automated repository bundle tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(CacheManagerTest.class);
		suite.addTestSuite(RepositoryHelperTest.class);
		suite.addTestSuite(RepositoryExtensionPointTest.class);
		suite.addTestSuite(FileReaderTest2.class);
		suite.addTest(new JUnit4TestAdapter(ChecksumHelperTest.class));
		return suite;
	}
}
