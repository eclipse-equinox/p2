/*******************************************************************************
 * Copyright (c) 2009, 2018 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.discovery.tests.core;

import static org.junit.Assert.assertFalse;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.compatibility.RemoteBundleDiscoveryStrategy;
import org.eclipse.equinox.p2.discovery.tests.DiscoveryTestConstants;
import org.junit.Before;
import org.junit.Test;

/**
 * A test that uses the real discovery directory and verifies that it works, and
 * that all referenced update sites appear to be available.
 *
 * @author David Green
 */
public class ConnectorDiscoveryRemoteTest {

	private Catalog connectorDiscovery;

	@Before
	public void setUp() throws Exception {
		connectorDiscovery = new Catalog();
		connectorDiscovery.setVerifyUpdateSiteAvailability(false);

		connectorDiscovery.getDiscoveryStrategies().clear();
		RemoteBundleDiscoveryStrategy remoteStrategy = new RemoteBundleDiscoveryStrategy();
		remoteStrategy.setDirectoryUrl(DiscoveryTestConstants.DISCOVERY_URL);
		connectorDiscovery.getDiscoveryStrategies().add(remoteStrategy);
	}

	@Test
	public void testRemoteDirectory() {
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertFalse(connectorDiscovery.getCategories().isEmpty());
		assertFalse(connectorDiscovery.getItems().isEmpty());
	}

}
