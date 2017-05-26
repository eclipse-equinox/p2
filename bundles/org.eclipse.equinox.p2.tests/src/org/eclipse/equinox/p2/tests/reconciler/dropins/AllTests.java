/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - Fragment support added.
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import junit.framework.*;
import org.eclipse.equinox.p2.tests.sharedinstall.AbstractSharedInstallTest;

/**
 * To run the reconciler tests, you must perform some manual setup steps:
 * 1) Download the platform runtime binary zip (latest build or the one you want to test).
 * 2) Set the following system property to the file system path of the binary zip. For example:
 * 
 * -Dorg.eclipse.equinox.p2.reconciler.tests.platform.archive=c:/tmp/eclipse-platform-3.4-win32.zip
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(BasicTests.suite());
		suite.addTest(ConfigurationTests.suite());
		suite.addTest(FeaturePatchTest.suite());
		suite.addTest(SharedInstallTests.suite());
		suite.addTest(SharedInstallTestsProfileSpoofEnabled.suite());
		if (!AbstractSharedInstallTest.WINDOWS) {
			suite.addTest(SharedInstallTestsProfileSpoofEnabledConfigured.suite());
		}
		suite.addTest(Bug362692.suite());
		return suite;
	}
}
