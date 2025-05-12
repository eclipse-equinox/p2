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

public class TestInitialRun extends AbstractSharedInstallTest {

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(TestInitialRun.class.getName());
		suite.addTest(new TestInitialRun("testInitialRun"));
		return suite;
	}

	public TestInitialRun(String name) {
		super(name);
	}

	public void testInitialRun() throws IOException {
		assertInitialized();
		setupReadOnlyInstall();
		System.out.println(readOnlyBase);
		System.out.println(userBase);

		//Here we are invoking the director app with -listInstalledRoots to force the profile to be loaded.
		startEclipseAsUser();
		assertFalse(getUserBundleInfo().exists());
		assertFalse(getUserBundleInfoTimestamp().exists());
		assertProfileStatePropertiesHasKey(getUserProfileFolder(), IProfile.STATE_PROP_SHARED_INSTALL);
		assertProfileStatePropertiesHasValue(getUserProfileFolder(), IProfile.STATE_SHARED_INSTALL_VALUE_INITIAL);
		assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());
	}

}
