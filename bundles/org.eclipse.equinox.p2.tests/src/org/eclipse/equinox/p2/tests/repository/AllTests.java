/*******************************************************************************
 * Copyright (c) 2009, 2012 Cloudsmith Inc and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		return suite;
	}
}
