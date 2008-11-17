/*******************************************************************************
 * Copyright (c) 2008 Red Hat, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
		suite.addTestSuite(SimpleConfiguratorUtilsTest.class);
		suite.addTestSuite(BundlesTxtTest.class);
		suite.addTestSuite(NonExclusiveMode.class);
		//$JUnit-END$
		return suite;
	}

}
