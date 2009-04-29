/*******************************************************************************
 * Copyright (c) 2006-2009, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.metadata.repository;

import junit.framework.*;

public class ServerBasedTestCase extends TestCase {
	//	public static Test suite() {
	//		final TestSuite suite = new TestSuite();
	//		TestSetup wrapper = new TestSetup(suite) {
	//			public void setUp() throws Exception {
	//				oneTimeSetUp();
	//			}
	//
	//			public void tearDown() throws Exception {
	//				oneTimeTearDown();
	//			}
	//		};
	//		return wrapper;
	//	}

	public void run(TestResult result) {
		Protectable p = new ProtectedRunner(result);
		result.runProtected(this, p);
	}

	protected void basicRun(TestResult result) {
		super.run(result);
	}

	public static void oneTimeSetUp() throws Exception {
		AllServerTests.checkSetUp();
	}

	public static void oneTimeTearDown() throws Exception {
		AllServerTests.checkTearDown();
	}

	private class ProtectedRunner implements Protectable {
		private TestResult result;

		ProtectedRunner(TestResult result) {
			this.result = result;
		}

		public void protect() throws Exception {
			oneTimeSetUp();
			basicRun(result);
			oneTimeTearDown();
		}
	}
}
