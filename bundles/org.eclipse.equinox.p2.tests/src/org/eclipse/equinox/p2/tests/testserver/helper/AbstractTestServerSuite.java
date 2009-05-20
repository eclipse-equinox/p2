/*******************************************************************************
 *  Copyright (c) 2009 Cloudsmith and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      Cloudsmith - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.testserver.helper;

import junit.framework.TestCase;

/**
 * Testsuite that manages the start and stop of the testserver for a suite of tests.
 * 
 * A derived class should implement the following:<br/><br/>
 * <code>
 *	public static Test suite() throws Exception { <br/>
 *		&nbsp;&nbsp;&nbsp;&nbsp;final TestSuite suite = new TestSuite("...name of suite..."); <br/>
 *		&nbsp;&nbsp;&nbsp;&nbsp;suite.addTest(new AbstractTestServerSuite("startServer"));<br/>
 * <br/>
 *		&nbsp;&nbsp;&nbsp;&nbsp;// Add tests in the suite - here is an example:<br/>
 *		&nbsp;&nbsp;&nbsp;&nbsp;suite.addTestSuite(ExampleTest.class);<br/>
 * <br/>
 *		&nbsp;&nbsp;&nbsp;&nbsp;suite.addTest(new AbstractTestServerSuite("startServer"));<br/>
 *      &nbsp;&nbsp;&nbsp;&nbsp;return suite;<br/>
 *  }<br/>
 * </code>
 * The tests in the suite should call {@link TestServerLauncher} to make sure the server is
 * running either started by this suite, or directly when test is run individually. This is
 * handled by the class {@link AbstractTestServerClientCase} which serves as the base for
 * tests requiring access to a running server.
 */
public class AbstractTestServerSuite extends TestCase {

	// TEMPLATE CODE: do not remove - it is useful to copy this when creating a new class.
	//	public static Test suite() throws Exception {
	//		final TestSuite suite = new TestSuite("AllServerBasedTestSuite");
	//		addToSuite(suite);
	//		return suite;
	//	}
	//
	//	public static void addToSuite(TestSuite suite) {
	//		suite.addTest(new AbstractTestServerSuite("startServer"));
	//		// AuthTest *should* run twice to make sure that second attempt produces the same result.
	//		suite.addTestSuite(AuthTest.class);
	//		suite.addTestSuite(AuthTest.class);
	//		suite.addTestSuite(HttpStatusTest.class);
	//		suite.addTestSuite(TimeoutTest.class);
	//		suite.addTest(new AbstractTestServerSuite("stopServer"));
	//	}

	public void startServer() throws Exception {
		TestServerController.oneTimeSetUp();
	}

	public void stopServer() throws Exception {
		TestServerController.oneTimeTearDown();
	}

	public AbstractTestServerSuite(String testName) {
		super(testName);
	}

}