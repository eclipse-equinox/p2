/*******************************************************************************
 * Copyright (c) 2009, 2018 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.discovery.tests;

import org.eclipse.equinox.p2.discovery.tests.core.*;
import org.eclipse.equinox.p2.discovery.tests.core.util.TransportUtilTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author Steffen Pingel
 */
@RunWith(Suite.class)
@SuiteClasses({ ConnectorDiscoveryTest.class, DirectoryParserTest.class, BundleDiscoveryStrategyTest.class,
		TransportUtilTest.class })
public class AllDiscoveryTests {

	// suite.addTestSuite(RemoteBundleDiscoveryStrategyTest.class);
	// suite.addTestSuite(ConnectorDiscoveryRemoteTest.class);

}
