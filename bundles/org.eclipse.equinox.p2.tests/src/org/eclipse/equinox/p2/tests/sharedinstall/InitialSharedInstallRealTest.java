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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

//See InitialSharedInstall
public class InitialSharedInstallRealTest extends AbstractSharedInstallTest {

	public InitialSharedInstallRealTest(String name) {
		super(name);
	}

	public void testImportFromPreviousInstall() throws IOException {
		assertInitialized();
		replaceDotEclipseProductFile(new File(output, getRootFolder()), "p2.automated.test", "1.0.1");
		installVerifierInBase();
		setupReadOnlyInstall();
		reallyReadOnly(readOnlyBase);
		System.out.println(readOnlyBase);
		System.out.println(userBase);

		//Run the verifier in read only mode to ensure that the wizard opens up
		Properties verificationProperties = new Properties();
		verificationProperties.setProperty("checkMigrationWizard", "true");
		verificationProperties.setProperty("checkMigrationWizard.open", "true");
		executeVerifierWithoutSpecifyingConfiguration(verificationProperties);
		removeReallyReadOnly(readOnlyBase);
	}
}
