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

public class DoubleBaseChange extends AbstractSharedInstallTest {

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(DoubleBaseChange.class.getName());
		suite.addTest(new DoubleBaseChange("testTwoChanges"));
		return suite;
	}

	public DoubleBaseChange(String name) {
		super(name);
	}

	public void testTwoChanges() {
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

		{ //Now change the base twice in a row and then check if the wizard opens
			installVerifierInBase();
			installFeature2InBase();

			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "true");
			executeVerifier(verificationProperties);
		}
	}
}
