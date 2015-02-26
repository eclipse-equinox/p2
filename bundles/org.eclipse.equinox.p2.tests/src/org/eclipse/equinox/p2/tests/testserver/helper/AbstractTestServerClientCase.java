/*******************************************************************************
 *  Copyright (c) 2009, 2015 Cloudsmith and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      Cloudsmith - initial API and implementation
 *      Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.testserver.helper;

import java.security.cert.Certificate;
import junit.framework.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.tests.TestActivator;

public class AbstractTestServerClientCase extends TestCase {

	public AbstractTestServerClientCase() {
		super();
	}

	public AbstractTestServerClientCase(String name) {
		super(name);
	}

	public void run(TestResult result) {
		Protectable p = new ProtectedRunner(result);
		result.runProtected(this, p);
	}

	/**
	 * Returns a URL string part consisting of http://localhost:<port>
	 * @return String with first part of URL
	 */
	protected String getBaseURL() {
		return "http://localhost:" + System.getProperty(TestServerController.PROP_TESTSERVER_PORT, "8080");
	}

	protected static IProvisioningAgent getAgent() {
		//get the global agent for the currently running system
		return ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgent.class);
	}

	protected void basicRun(TestResult result) {
		super.run(result);
	}

	public static void oneTimeSetUp() throws Exception {
		TestServerController.checkSetUp();
	}

	public static void oneTimeTearDown() throws Exception {
		TestServerController.checkTearDown();
	}

	public void tearDown() throws Exception {
		// if a test is run out or order - this must be done
		TestServerController.checkTearDown();
	}

	public void setUp() throws Exception {
		// if a test is run out or order - this must be done
		TestServerController.checkSetUp();
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

	private int counter = 0;

	public int getLoginCount() {
		return counter;
	}

	/**
	 * Makes the controller use a login service that will succeed in logging in to the test server.
	 * The login counter is reset. See {@link #getLoginCount}.
	 */
	public void setAladdinLoginService() {
		counter = 0;
		TestServerController.setServiceUI(new AladdinNotSavedService());
	}

	/**
	 * Makes the controller use a login service that will not succeed in logging in to the test server.
	 * The login counter is reset. See {@link #getLoginCount}.
	 */
	public void setBadLoginService() {
		counter = 0;
		TestServerController.setServiceUI(new AladdinNotSavedService());
	}

	public void clearLoginService() {
		counter = 0;
		TestServerController.setServiceUI(null);
	}

	public class AladdinNotSavedService extends UIServices {

		public AuthenticationInfo getUsernamePassword(String location) {
			return new AuthenticationInfo("Aladdin", "open sesame", false);
		}

		public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
			assertEquals("Aladdin", previousInfo.getUserName());
			assertEquals("open sesame", previousInfo.getPassword());
			assertEquals(false, previousInfo.saveResult());
			return previousInfo;
		}

		/**
		 * Not used
		 */
		public TrustInfo getTrustInfo(Certificate[][] untrustedChain, String[] unsignedDetail) {
			return new TrustInfo(null, false, true);
		}
	}

	/**
	 * Service that tries to login with the wrong password.
	 * @author henrik
	 *
	 */
	public class BadLoginService extends UIServices {

		public AuthenticationInfo getUsernamePassword(String location) {
			return new AuthenticationInfo("moria", "friend", false);
		}

		public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
			assertEquals("moria", previousInfo.getUserName());
			assertEquals("friend", previousInfo.getPassword());
			assertEquals(false, previousInfo.saveResult());
			return previousInfo;
		}

		/**
		 * Not used
		 */
		public TrustInfo getTrustInfo(Certificate[][] untrustedChain, String[] unsignedDetail) {
			return new TrustInfo(null, false, true);
		}

	}

}
