/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ericsson AB - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

public class InstallInUserSpace extends AbstractSharedInstallTest {

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(InstallInUserSpace.class.getName());
		suite.addTest(new InstallInUserSpace("testInstallInUserSpace"));
		return suite;
	}

	public InstallInUserSpace(String name) {
		super(name);
	}

	public void testInstallInUserSpace() throws IOException {
		assertInitialized();
		setupReadOnlyInstall();
		System.out.println(readOnlyBase);
		System.out.println(userBase);

		installFeature1AndVerifierInUser();
		assertTrue(isInUserBundlesInfo("p2TestBundle1"));
		assertTrue(isInUserBundlesInfo("org.eclipse.swt")); //this verifies that we have the bundles from the base installed in the user bundles.info

		assertTrue(getUserBundleInfoTimestamp().exists());
		assertTrue(getConfigIniTimestamp().exists());
		assertProfileStatePropertiesHasKey(getUserProfileFolder(), IProfile.STATE_PROP_SHARED_INSTALL);
		assertProfileStatePropertiesHasValue(getUserProfileFolder(), IProfile.STATE_SHARED_INSTALL_VALUE_INITIAL);
		assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());
		assertEquals(runningWithReconciler ? 3 : 2, getProfileTimestampsFromUser().length);
	}
}
