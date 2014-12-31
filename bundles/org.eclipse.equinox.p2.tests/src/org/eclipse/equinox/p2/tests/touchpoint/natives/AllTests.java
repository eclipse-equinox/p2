/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.natives;

import junit.framework.*;

/**
 * Performs all automated director tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTestSuite(ChmodActionTest.class);
		suite.addTestSuite(CleanupzipActionTest.class);
		suite.addTestSuite(CollectActionTest.class);
		suite.addTestSuite(LinkActionTest.class);
		suite.addTestSuite(MkdirActionTest.class);
		suite.addTestSuite(NativeTouchpointTest.class);
		suite.addTestSuite(RmdirActionTest.class);
		suite.addTestSuite(UnzipActionTest.class);
		suite.addTestSuite(CopyActionTest.class);
		suite.addTestSuite(RemoveActionTest.class);
		suite.addTestSuite(BackupStoreTest.class);
		suite.addTest(new JUnit4TestAdapter(CheckAndPromptNativePackageWindowsRegistryTest.class));
		return suite;
	}
}
