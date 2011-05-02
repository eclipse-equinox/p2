/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.discovery.tests.core;

import junit.framework.TestCase;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.compatibility.RemoteBundleDiscoveryStrategy;
import org.eclipse.equinox.p2.discovery.tests.DiscoveryTestConstants;

/**
 * A test that uses the real discovery directory and verifies that it works, and that all referenced update sites appear
 * to be available.
 * 
 * @author David Green
 */
public class ConnectorDiscoveryRemoteTest extends TestCase {

	private Catalog connectorDiscovery;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		connectorDiscovery = new Catalog();
		connectorDiscovery.setVerifyUpdateSiteAvailability(false);

		connectorDiscovery.getDiscoveryStrategies().clear();
		RemoteBundleDiscoveryStrategy remoteStrategy = new RemoteBundleDiscoveryStrategy();
		remoteStrategy.setDirectoryUrl(DiscoveryTestConstants.DISCOVERY_URL);
		connectorDiscovery.getDiscoveryStrategies().add(remoteStrategy);
	}

	public void testRemoteDirectory() {
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertFalse(connectorDiscovery.getCategories().isEmpty());
		assertFalse(connectorDiscovery.getItems().isEmpty());
	}

	//	public void testVerifyAvailability() throws CoreException {
	//		connectorDiscovery.performDiscovery(new NullProgressMonitor());
	//		for (CatalogItem connector : connectorDiscovery.getConnectors()) {
	//			assertNull(connector.getAvailable());
	//		}
	//		connectorDiscovery.verifySiteAvailability(new NullProgressMonitor());
	//
	//		assertFalse(connectorDiscovery.getConnectors().isEmpty());
	//
	//		int unavailableCount = 0;
	//		for (CatalogItem connector : connectorDiscovery.getConnectors()) {
	//			assertNotNull(connector.getAvailable());
	//			if (!connector.getAvailable()) {
	//				++unavailableCount;
	//			}
	//		}
	//		if (unavailableCount > 0) {
	//			fail(String.format("%s unavailable: %s", unavailableCount, computeUnavailableConnetorDescriptorNames()));
	//		}
	//	}

	//	private String computeUnavailableConnetorDescriptorNames() {
	//		String message = "";
	//		for (CatalogItem connector : connectorDiscovery.getItems()) {
	//			if (!connector.getAvailable()) {
	//				if (message.length() > 0) {
	//					message += ", ";
	//				}
	//				message += connector.getName();
	//			}
	//		}
	//		return message;
	//	}

}
