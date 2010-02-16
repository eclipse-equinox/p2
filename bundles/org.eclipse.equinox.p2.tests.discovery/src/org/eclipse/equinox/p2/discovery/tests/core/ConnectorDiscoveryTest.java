/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.discovery.tests.core;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.p2.discovery.tests.core.mock.CatalogItemMockFactory;
import org.eclipse.equinox.p2.discovery.tests.core.mock.MockDiscoveryStrategy;
import org.osgi.framework.Version;

/**
 * @author David Green
 */
public class ConnectorDiscoveryTest extends TestCase {

	private Catalog connectorDiscovery;

	private MockDiscoveryStrategy mockDiscoveryStrategy;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		connectorDiscovery = new Catalog();
		mockDiscoveryStrategy = new MockDiscoveryStrategy();
		connectorDiscovery.getDiscoveryStrategies().add(mockDiscoveryStrategy);
	}

	public void testPlatformFilter_None() throws CoreException {
		connectorDiscovery.performDiscovery(new NullProgressMonitor());
		assertEquals(mockDiscoveryStrategy.getConnectorCount(), connectorDiscovery.getItems().size());
	}

	public void testPlatformFilter_NegativeMatch() throws CoreException {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				platformFilter("(& (osgi.os=macosx) (osgi.ws=carbon))");
			}
		});
		// test to ensure that all non-matching platform filters are not discovered
		Dictionary<Object, Object> environment = new Properties();
		environment.put("osgi.os", "win32");
		environment.put("osgi.ws", "windows");
		connectorDiscovery.setEnvironment(environment);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertTrue(connectorDiscovery.getItems().isEmpty());
	}

	public void testPlatformFilter_PositiveMatch() throws CoreException {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				platformFilter("(& (osgi.os=macosx) (osgi.ws=carbon))");
			}
		});
		Dictionary<Object, Object> environment = new Properties();

		// test to ensure that all matching platform filters are discovered
		environment.put("osgi.os", "macosx");
		environment.put("osgi.ws", "carbon");
		connectorDiscovery.setEnvironment(environment);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertFalse(connectorDiscovery.getItems().isEmpty());
		assertEquals(mockDiscoveryStrategy.getConnectorCount(), connectorDiscovery.getItems().size());
	}

	public void testFeatureFilter_PositiveMatch() throws CoreException {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				featureFilter("com.foo.bar.feature", "[1.0,2.0)");
			}
		});
		Map<String, Version> featureToVersion = new HashMap<String, Version>();
		featureToVersion.put("com.foo.bar.feature", new Version("1.1"));
		connectorDiscovery.setFeatureToVersion(featureToVersion);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertFalse(connectorDiscovery.getItems().isEmpty());
		assertEquals(mockDiscoveryStrategy.getConnectorCount(), connectorDiscovery.getItems().size());
	}

	public void testFeatureFilter_NegativeMatch_VersionMismatch() throws CoreException {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				featureFilter("com.foo.bar.feature", "[1.2,2.0)");
			}
		});
		Map<String, Version> featureToVersion = new HashMap<String, Version>();
		featureToVersion.put("com.foo.bar.feature", new Version("1.1"));
		connectorDiscovery.setFeatureToVersion(featureToVersion);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertTrue(connectorDiscovery.getItems().isEmpty());
	}

	public void testFeatureFilter_NegativeMatch_NotPresent() throws CoreException {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				featureFilter("com.foo.bar.feature", "[1.2,2.0)");
			}
		});
		Map<String, Version> featureToVersion = new HashMap<String, Version>();
		connectorDiscovery.setFeatureToVersion(featureToVersion);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertTrue(connectorDiscovery.getItems().isEmpty());
	}

	public void testCategorization() throws CoreException {
		connectorDiscovery.performDiscovery(new NullProgressMonitor());
		assertTrue(!connectorDiscovery.getItems().isEmpty());
		assertTrue(!connectorDiscovery.getCategories().isEmpty());

		for (CatalogItem connector : connectorDiscovery.getItems()) {
			assertNotNull(connector.getCategory());
			assertEquals(connector.getCategoryId(), connector.getCategory().getId());
			assertTrue(connector.getCategory().getItems().contains(connector));
		}
	}

	public void testMultipleStrategies() throws CoreException {
		MockDiscoveryStrategy strategy = new MockDiscoveryStrategy();
		strategy.setConnectorMockFactory(mockDiscoveryStrategy.getConnectorMockFactory());
		strategy.setCategoryMockFactory(mockDiscoveryStrategy.getCategoryMockFactory());
		connectorDiscovery.getDiscoveryStrategies().add(strategy);

		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertEquals(mockDiscoveryStrategy.getConnectorMockFactory().getCreatedCount(),
				connectorDiscovery.getItems().size());
		assertEquals(mockDiscoveryStrategy.getCategoryMockFactory().getCreatedCount(),
				connectorDiscovery.getCategories().size());
	}
}
