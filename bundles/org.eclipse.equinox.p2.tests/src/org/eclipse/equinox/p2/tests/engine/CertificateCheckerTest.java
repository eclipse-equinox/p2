/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUICheckUnsigned;
import org.eclipse.equinox.internal.provisional.p2.engine.CertificateChecker;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;
import org.osgi.framework.ServiceRegistration;

/**
 * Tests for {@link CertificateChecker}.
 */
public class CertificateCheckerTest extends AbstractProvisioningTest {
	class CertificateTestService implements IServiceUI, IServiceUICheckUnsigned {
		public boolean unsignedReturnValue = true;
		public boolean wasPrompted = false;

		public AuthenticationInfo getUsernamePassword(String location) {
			return null;
		}

		public AuthenticationInfo getUsernamePassword(String location, AuthenticationInfo previousInfo) {
			return null;
		}

		public boolean promptForUnsignedContent(String[] details) {
			wasPrompted = true;
			return unsignedReturnValue;
		}

		public Certificate[] showCertificates(Certificate[][] certificates) {
			return null;
		}

	}

	CertificateChecker checker;
	ServiceRegistration serviceReg;
	CertificateTestService serviceUI;
	File unsigned;

	protected void setUp() throws Exception {
		checker = new CertificateChecker();
		try {
			unsigned = TestData.getFile("CertificateChecker", "unsigned.jar");
		} catch (IOException e) {
			fail("0.99", e);
		}
		assertTrue("1.0", unsigned != null);
		assertTrue("1.0", unsigned.exists());
		serviceUI = new CertificateTestService();
		serviceReg = EngineActivator.getContext().registerService(IServiceUI.class.getName(), serviceUI, null);
	}

	protected void tearDown() throws Exception {
		if (serviceReg != null)
			serviceReg.unregister();
	}

	/**
	 * Tests that installing unsigned content is not allowed when the policy says it must fail.
	 */
	public void testPolicyAllow() {
		try {
			//if the service is consulted it will say no
			serviceUI.unsignedReturnValue = false;
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_ALLOW);
			checker.add(unsigned);
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.OK, result.getSeverity());
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that installing unsigned content is not allowed when the policy says it must fail.
	 */
	public void testPolicyFail() {
		try {
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_FAIL);
			checker.add(unsigned);
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.ERROR, result.getSeverity());

		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that installing unsigned content with the "prompt" policy and the prompt succeeds.
	 */
	public void testPolicyPromptSuccess() {
		try {
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			serviceUI.unsignedReturnValue = true;
			checker.add(unsigned);
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.OK, result.getSeverity());
			assertTrue("1.1", serviceUI.wasPrompted);
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}

	/**
	 * Tests that the default policy for unsigned content is to prompt.
	 */
	public void testPolicyDefault() {
		System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		serviceUI.unsignedReturnValue = true;
		checker.add(unsigned);
		IStatus result = checker.start();
		assertEquals("1.0", IStatus.OK, result.getSeverity());
		assertTrue("1.1", serviceUI.wasPrompted);
	}

	/**
	 * Tests that installing unsigned content with the "prompt" policy and the prompt says no.
	 */
	public void testPolicyPromptCancel() {
		try {
			System.getProperties().setProperty(EngineActivator.PROP_UNSIGNED_POLICY, EngineActivator.UNSIGNED_PROMPT);
			serviceUI.unsignedReturnValue = false;
			checker.add(unsigned);
			IStatus result = checker.start();
			assertEquals("1.0", IStatus.CANCEL, result.getSeverity());
			assertTrue("1.1", serviceUI.wasPrompted);
		} finally {
			System.getProperties().remove(EngineActivator.PROP_UNSIGNED_POLICY);
		}
	}
}
