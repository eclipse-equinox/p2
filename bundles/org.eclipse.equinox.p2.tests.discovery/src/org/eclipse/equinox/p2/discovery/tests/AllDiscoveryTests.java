/*******************************************************************************
 * Copyright (c) 2009, 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.discovery.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.p2.discovery.tests.core.*;
import org.eclipse.equinox.p2.discovery.tests.core.util.TransportUtilTest;

/**
 * @author Steffen Pingel
 */
public class AllDiscoveryTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for org.eclipse.equinox.p2.discovery.tests"); //$NON-NLS-1$
		suite.addTestSuite(ConnectorDiscoveryTest.class);
		suite.addTestSuite(DirectoryParserTest.class);
		suite.addTestSuite(BundleDiscoveryStrategyTest.class);
		//suite.addTestSuite(RemoteBundleDiscoveryStrategyTest.class);
		//suite.addTestSuite(ConnectorDiscoveryRemoteTest.class);
		suite.addTestSuite(TransportUtilTest.class);
		return suite;
	}

}
