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

public class Cancellation extends AbstractSharedInstallTest {

	public static Test suite() {
		TestSuite suite = new ReconcilerTestSuite();
		suite.setName(Cancellation.class.getName());
		suite.addTest(new Cancellation("testCancellation"));
		return suite;
	}

	public Cancellation(String name) {
		super(name);
	}

	public void testCancellation() throws IOException {
		assertInitialized();
		setupReadOnlyInstall();
		System.out.println(readOnlyBase);
		System.out.println(userBase);

		{ //install verifier and something else in user and checks there are there
			installFeature1AndVerifierInUser();
			Properties verificationProperties = new Properties();

			//no wizard should get opened
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "false");
			executeVerifier(verificationProperties);
		}

		{ //Now change the base. Install the verifier in the base, and run the verifier as a user
			installVerifierInBase();

			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "true");
			verificationProperties.setProperty("checkMigration.cancelAnswer", "1"); //Cancel, but do later
			executeVerifier(verificationProperties);
		}

		{
			//Check again that the wizard opens, and this time tell it to not prompt again
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "true");
			verificationProperties.setProperty("checkMigration.cancelAnswer", "0"); //Don't prompt for migration again
			executeVerifier(verificationProperties);
		}

		{
			//Check again that we are not prompted anymore
			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "false");
			executeVerifier(verificationProperties);
		}

		{ //install something in the user, then install something in the base and verify that the wizard is opened again
			installFeature1InUser();
			installFeature2InBase();

			Properties verificationProperties = new Properties();
			verificationProperties.setProperty("checkMigrationWizard", "true");
			verificationProperties.setProperty("checkMigrationWizard.open", "true");
			executeVerifier(verificationProperties);
		}
	}
}
