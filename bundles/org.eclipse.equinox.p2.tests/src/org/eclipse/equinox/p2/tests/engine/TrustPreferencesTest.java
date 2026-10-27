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

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.phases.AuthorityChecker;
import org.eclipse.equinox.internal.p2.engine.phases.CertificateChecker;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests that the "trust always" preferences of {@link CertificateChecker} and
 * {@link AuthorityChecker} are read from the profile scope and fall back to the
 * default scope.
 */
public class TrustPreferencesTest extends AbstractProvisioningTest {

	private IProfile profile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		profile = createProfile(TrustPreferencesTest.class.getName() + '.' + getName());
	}

	@Override
	protected void tearDown() throws Exception {
		IEclipsePreferences defaults = DefaultScope.INSTANCE.getNode(EngineActivator.ID);
		defaults.remove(CertificateChecker.TRUST_ALWAYS_PROPERTY);
		defaults.remove(AuthorityChecker.TRUST_ALL_AUTHORITIES);
		super.tearDown();
	}

	public void testCertificateCheckerTrustAlwaysFromProfile() {
		CertificateChecker checker = new CertificateChecker(getAgent());
		checker.setProfile(profile);
		assertFalse(checker.isTrustAlways());
		assertOK(checker.setTrustAlways(true));
		assertTrue(checker.isTrustAlways());
		assertOK(checker.setTrustAlways(false));
		assertFalse(checker.isTrustAlways());
	}

	public void testCertificateCheckerTrustAlwaysFromDefaults() {
		CertificateChecker checker = new CertificateChecker(getAgent());
		checker.setProfile(profile);
		DefaultScope.INSTANCE.getNode(EngineActivator.ID).putBoolean(CertificateChecker.TRUST_ALWAYS_PROPERTY, true);
		assertTrue(checker.isTrustAlways());
		// a value set in the profile wins over the default
		assertOK(checker.setTrustAlways(false));
		assertFalse(checker.isTrustAlways());
	}

	public void testAuthorityCheckerTrustAlwaysFromProfile() {
		AuthorityChecker checker = new AuthorityChecker(getAgent(), profile);
		assertFalse(checker.isTrustAlways());
		assertOK(checker.setTrustAlways(true));
		assertTrue(checker.isTrustAlways());
		assertOK(checker.setTrustAlways(false));
		assertFalse(checker.isTrustAlways());
	}

	public void testAuthorityCheckerTrustAlwaysFromDefaults() {
		AuthorityChecker checker = new AuthorityChecker(getAgent(), profile);
		DefaultScope.INSTANCE.getNode(EngineActivator.ID).putBoolean(AuthorityChecker.TRUST_ALL_AUTHORITIES, true);
		assertTrue(checker.isTrustAlways());
		// a value set in the profile wins over the default
		assertOK(checker.setTrustAlways(false));
		assertFalse(checker.isTrustAlways());
	}
}
