/*******************************************************************************
 * Copyright (c) 2008, 2013 Red Hat, Inc. and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import junit.framework.Test;
import junit.framework.TestSuite;

public class SimpleConfiguratorTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for org.eclipse.equinox.simpleconfigurator");

		//$JUnit-BEGIN$

		suite.addTestSuite(SimpleConfiguratorTest.class);
		suite.addTestSuite(SimpleConfiguratorTestExtended.class);
		suite.addTestSuite(SimpleConfiguratorTestExtendedConfigured.class);

		suite.addTestSuite(SimpleConfiguratorUtilsTest.class);
		suite.addTestSuite(SimpleConfiguratorUtilsExtendedTest.class);
		suite.addTestSuite(SimpleConfiguratorUtilsExtendedConfiguredTest.class);

		suite.addTestSuite(BundlesTxtTest.class);
		suite.addTestSuite(BundlesTxtTestExtended.class);
		suite.addTestSuite(BundlesTxtTestExtendedConfigured.class);

		suite.addTestSuite(NonExclusiveMode.class);
		suite.addTestSuite(NonExclusiveModeExtended.class);
		suite.addTestSuite(NonExclusiveModeExtendedConfigured.class);

		//$JUnit-END$
		return suite;
	}

}
