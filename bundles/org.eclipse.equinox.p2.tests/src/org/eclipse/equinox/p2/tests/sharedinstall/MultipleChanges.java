/*******************************************************************************
 * Copyright (c) 2013 Ericsson AB and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Ericsson AB - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.sharedinstall;

import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.p2.tests.reconciler.dropins.ReconcilerTestSuite;

public class MultipleChanges extends AbstractSharedInstallTest {

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(MultipleChanges.class.getName());
		suite.addTest(new MultipleChanges("testMultipleChangesWithExecution"));
		return suite;
	}

	public MultipleChanges(String name) {
		super(name);
	}

	public void testMultipleChangesWithExecution() {
		assertInitialized();
		setupReadOnlyInstall();
		System.out.println(readOnlyBase);
		System.out.println(userBase);

		{ //install verifier and feature1
			installFeature1AndVerifierInUser();
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("expectedBundleList", "p2TestBundle1,org.eclipse.equinox.p2.tests.verifier");
			verificationProperties.setProperty("checkProfileResetFlag", "false");
			verificationProperties.setProperty("not.sysprop.eclipse.ignoreUserConfiguration", "");
			executeVerifier(verificationProperties);

			assertTrue(isInUserBundlesInfo("p2TestBundle1"));
			assertProfileStatePropertiesHasKey(getUserProfileFolder(), "_simpleProfileRegistry_internal_" + getMostRecentProfileTimestampFromBase());
		}

		{ //Add the verifier in the base and check that the wizard opens
			installVerifierInBase();
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "true");
			verificationProperties.setProperty("checkMigrationWizard.assumeMigrated", "true");
			executeVerifier(verificationProperties);
			installFeature1InUser(); //We are doing this because the code that actually do the installation in the wizard is *not* invoked
		}

		{ //Add feature 2 in the base and check the wizard opens
			installFeature2InBase();
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "true");
			executeVerifier(verificationProperties);
		}
	}
}
