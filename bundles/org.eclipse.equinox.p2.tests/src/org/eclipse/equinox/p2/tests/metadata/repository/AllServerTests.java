/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.security.cert.Certificate;
import junit.extensions.TestSetup;
import junit.framework.*;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceRegistration;

/**
 * Performs all automated metadata repository tests that requires a local test server running
 * on port 8080.
 */
public class AllServerTests extends TestCase {

	static IServiceUI hookedAuthDialog;
	private static ServiceRegistration certificateUIRegistration;
	private static int setUpCounter = 0;

	public static Test suite() {
		final TestSuite suite = new TestSuite("AllServerBasedTestSuite");
		// AuthTest *should* run twice to make sure that second attempt produces the same result.
		suite.addTestSuite(AuthTest.class);
		suite.addTestSuite(AuthTest.class);

		TestSetup wrapper = new TestSetup(suite) {
			public void setUp() throws Exception {
				oneTimeSetUp();
			}

			public void tearDown() throws Exception {
				oneTimeTearDown();
			}
		};
		return wrapper;
	}

	//	public void testRegistration() {
	//		assertNotNull("Service was not registered", certificateUIRegistration);
	//	}

	public static void oneTimeSetUp() throws Exception {
		certificateUIRegistration = TestActivator.getContext().registerService(IServiceUI.class.getName(), new DelegatingAuthService(), null);
		setUpCounter = 1;
	}

	public static void oneTimeTearDown() throws Exception {
		certificateUIRegistration.unregister();
		setUpCounter = 0;
	}

	/**
	 * 
	 * @return true if a tear down should be performed
	 * @throws Exception
	 */
	public static synchronized void checkSetUp() throws Exception {
		if (setUpCounter == 0) {
			oneTimeSetUp();
			return;
		}
		setUpCounter++;
	}

	public static synchronized void checkTearDown() throws Exception {
		setUpCounter--;
		if (setUpCounter < 0)
			throw new IllegalStateException("Unbalanced setup/teardown");

		if (setUpCounter == 0)
			oneTimeTearDown();
		return;
	}

	static public void setServiceUI(IServiceUI hook) {
		hookedAuthDialog = hook;
	}

	public static class DelegatingAuthService implements IServiceUI {

		public AuthenticationInfo getUsernamePassword(String location) {
			if (hookedAuthDialog != null)
				return hookedAuthDialog.getUsernamePassword(location);
			return null;
		}

		public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
			if (hookedAuthDialog != null)
				return hookedAuthDialog.getUsernamePassword(location, previousInfo);
			return null;
		}

		/**
		 * No need to implement
		 */
		public Certificate[] showCertificates(Certificate[][] certificates) {
			return null;
		}

	}
}