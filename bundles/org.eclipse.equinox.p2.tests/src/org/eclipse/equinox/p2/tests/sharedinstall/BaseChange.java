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
import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

public class BaseChange extends AbstractSharedInstallTest {

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(BaseChange.class.getName());
		suite.addTest(new BaseChange("testBaseChange"));
		return suite;
	}

	public BaseChange(String name) {
		super(name);
	}

	public void testBaseChange() throws IOException {
		assertInitialized();
		setupReadOnlyInstall();
		System.out.println(readOnlyBase);
		System.out.println(userBase);

		{ //install verifier and something else in user and checks there are there
			installFeature1AndVerifierInUser();
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("expectedBundleList", "p2TestBundle1,org.eclipse.equinox.p2.tests.verifier");
			verificationProperties.setProperty("checkProfileResetFlag", "false");
			verificationProperties.setProperty("not.sysprop.eclipse.ignoreUserConfiguration", "");
			executeVerifier(verificationProperties);

			assertTrue(isInUserBundlesInfo("p2TestBundle1"));
			assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());
		}

		{ //Now change the base. Install the verifier in the base, and run the verifier as a user
			installVerifierInBase();

			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("unexpectedBundleList", "p2TestBundle1");
			verificationProperties.setProperty("checkPresenceOfVerifier", "false");
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier");
			verificationProperties.setProperty("checkProfileResetFlag", "true");
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "true");
			executeVerifier(verificationProperties);
			assertTrue(isInUserBundlesInfo("p2TestBundle1")); //Despite the reset, the bundles.info is still on-disk unmodified since no provisioning has been done
			assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());

			verificationProperties = new Properties();
			//Execute the verifier another time, to check that the profile reset flag is not set
			verificationProperties.setProperty("checkProfileResetFlag", "false");
			verificationProperties.setProperty("checkPresenceOfVerifier", "false");
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier");
			executeVerifier(verificationProperties);
		}

		{ //Now add something into the user install again
			installFeature2InUser();
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("expectedBundleList", "org.eclipse.equinox.p2.tests.verifier");
			executeVerifier(verificationProperties);
			assertTrue(isInUserBundlesInfo("p2TestBundle2"));
			assertTrue(isInUserBundlesInfo("org.eclipse.equinox.p2.tests.verifier")); //It is now coming from the base
			assertFalse(isInUserBundlesInfo("p2TestBundle1"));

		}
	}
}
