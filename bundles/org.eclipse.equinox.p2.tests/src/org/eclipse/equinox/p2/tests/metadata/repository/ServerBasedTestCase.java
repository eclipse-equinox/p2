/*******************************************************************************
 * Copyright (c) 2006, 2015, Cloudsmith Inc.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 * 
 * Contributors:
 *     Cloudsmith Inc. - Initial API and implementation.
 *     Red Hat Inc. - Bug 460967
 ******************************************************************************/

package org.eclipse.equinox.p2.tests.metadata.repository;

import junit.framework.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.tests.TestActivator;

public class ServerBasedTestCase extends TestCase {

	public ServerBasedTestCase() {
		super();
	}

	public ServerBasedTestCase(String name) {
		super(name);
	}

	public void run(TestResult result) {
		Protectable p = new ProtectedRunner(result);
		result.runProtected(this, p);
	}

	protected String getBaseURL() {
		return "http://localhost:" + System.getProperty(AllServerTests.PROP_TESTSERVER_PORT, "8080");
	}

	protected static IProvisioningAgent getAgent() {
		//get the global agent for the currently running system
		return ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgent.class);
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

	public void tearDown() throws Exception {
		// if a test is run out or order - this must be done
		AllServerTests.checkTearDown();
	}

	public void setUp() throws Exception {
		// if a test is run out or order - this must be done
		AllServerTests.checkSetUp();
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
