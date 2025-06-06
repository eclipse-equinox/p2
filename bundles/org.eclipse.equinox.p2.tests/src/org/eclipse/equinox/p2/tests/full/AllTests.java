/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import junit.framework.*;

/**
 * Performs all automated full end-to-end install/update/rollback tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());

		// TODO re-enable all tests after resolution of https://bugs.eclipse.org/366540
		/*
		suite.addTestSuite(RepoValidator.class);

		suite.addTestSuite(End2EndTest35.class);
		suite.addTestSuite(End2EndTest36.class);
		suite.addTestSuite(End2EndTest37.class);
		suite.addTestSuite(End2EndTestCurrent.class);

		suite.addTest(FromPreviousToCurrent.suite());
		suite.addTest(InstallCurrentFromPrevious.suite());
		 */
		return suite;
	}

}
