/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import org.eclipse.equinox.internal.p2.engine.phases.AuthorityChecker;
import org.eclipse.equinox.internal.p2.engine.phases.CertificateChecker;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests that the "trust always" preferences are read back from the profile
 * scope.
 */
public class TrustPreferencesTest extends AbstractProvisioningTest {

	private IProfile profile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		profile = createProfile(TrustPreferencesTest.class.getName() + '.' + getName());
	}

	public void testCertificateCheckerTrustAlways() {
		CertificateChecker checker = new CertificateChecker(getAgent());
		checker.setProfile(profile);
		assertOK(checker.setTrustAlways(true));
		assertTrue(checker.isTrustAlways());
	}

	public void testAuthorityCheckerTrustAlways() {
		AuthorityChecker checker = new AuthorityChecker(getAgent(), profile);
		assertOK(checker.setTrustAlways(true));
		assertTrue(checker.isTrustAlways());
	}
}
