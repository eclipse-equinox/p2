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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.p2.tests.artifact.repository.MirrorRequestTest2;
import org.eclipse.equinox.p2.tests.artifact.repository.StatsTest;
import org.eclipse.equinox.p2.tests.engine.ProvisioningEventTest2;
import org.eclipse.equinox.p2.tests.testserver.helper.AbstractTestServerSuite;

public class AllTestServerTests extends AbstractTestServerSuite {

	public AllTestServerTests(String testName) {
		super(testName);
	}

	public static Test suite() throws Exception {
		final TestSuite suite = new TestSuite("AllTestServerTests");
		addToSuite(suite);
		return suite;
	}

	public static void addToSuite(TestSuite suite) {
		suite.addTest(new AbstractTestServerSuite("startServer"));

		suite.addTestSuite(FileInfoReaderTest.class);
		suite.addTestSuite(FileReaderTest.class);
		suite.addTestSuite(NTLMTest.class);
		suite.addTestSuite(MirrorRequestTest2.class);
		suite.addTestSuite(StatsTest.class);
		suite.addTestSuite(ProvisioningEventTest2.class);

		suite.addTest(new AbstractTestServerSuite("stopServer"));
	}

}
