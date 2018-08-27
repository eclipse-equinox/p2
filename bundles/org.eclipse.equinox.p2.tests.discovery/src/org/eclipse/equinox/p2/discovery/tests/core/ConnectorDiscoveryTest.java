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

import static org.junit.Assert.*;

import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.p2.discovery.tests.core.mock.CatalogItemMockFactory;
import org.eclipse.equinox.p2.discovery.tests.core.mock.MockDiscoveryStrategy;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;

/**
 * @author David Green
 */
public class ConnectorDiscoveryTest {

	private Catalog connectorDiscovery;

	private MockDiscoveryStrategy mockDiscoveryStrategy;

	@Before
	public void setUp() throws Exception {
		connectorDiscovery = new Catalog();
		mockDiscoveryStrategy = new MockDiscoveryStrategy();
		connectorDiscovery.getDiscoveryStrategies().add(mockDiscoveryStrategy);
	}

	@Test
	public void testPlatformFilter_None() {
		connectorDiscovery.performDiscovery(new NullProgressMonitor());
		assertEquals(mockDiscoveryStrategy.getConnectorCount(), connectorDiscovery.getItems().size());
	}

	@Test
	public void testPlatformFilter_NegativeMatch() {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				platformFilter("(& (osgi.os=macosx) (osgi.ws=carbon))"); //$NON-NLS-1$
			}
		});
		// test to ensure that all non-matching platform filters are not discovered
		Dictionary<Object, Object> environment = new Properties();
		environment.put("osgi.os", "win32"); //$NON-NLS-1$ //$NON-NLS-2$
		environment.put("osgi.ws", "windows"); //$NON-NLS-1$ //$NON-NLS-2$
		connectorDiscovery.setEnvironment(environment);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertTrue(connectorDiscovery.getItems().isEmpty());
	}

	@Test
	public void testPlatformFilter_PositiveMatch() {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				platformFilter("(& (osgi.os=macosx) (osgi.ws=carbon))"); //$NON-NLS-1$
			}
		});
		Dictionary<Object, Object> environment = new Properties();

		// test to ensure that all matching platform filters are discovered
		environment.put("osgi.os", "macosx"); //$NON-NLS-1$//$NON-NLS-2$
		environment.put("osgi.ws", "carbon"); //$NON-NLS-1$ //$NON-NLS-2$
		connectorDiscovery.setEnvironment(environment);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertFalse(connectorDiscovery.getItems().isEmpty());
		assertEquals(mockDiscoveryStrategy.getConnectorCount(), connectorDiscovery.getItems().size());
	}

	@Test
	public void testFeatureFilter_PositiveMatch() {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				featureFilter("com.foo.bar.feature", "[1.0,2.0)"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		Map<String, Version> featureToVersion = new HashMap<>();
		featureToVersion.put("com.foo.bar.feature", new Version("1.1")); //$NON-NLS-1$ //$NON-NLS-2$
		connectorDiscovery.setFeatureToVersion(featureToVersion);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertFalse(connectorDiscovery.getItems().isEmpty());
		assertEquals(mockDiscoveryStrategy.getConnectorCount(), connectorDiscovery.getItems().size());
	}

	@Test
	public void testFeatureFilter_NegativeMatch_VersionMismatch() {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				featureFilter("com.foo.bar.feature", "[1.2,2.0)"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		Map<String, Version> featureToVersion = new HashMap<>();
		featureToVersion.put("com.foo.bar.feature", new Version("1.1")); //$NON-NLS-1$ //$NON-NLS-2$
		connectorDiscovery.setFeatureToVersion(featureToVersion);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertTrue(connectorDiscovery.getItems().isEmpty());
	}

	@Test
	public void testFeatureFilter_NegativeMatch_NotPresent() {
		mockDiscoveryStrategy.setConnectorMockFactory(new CatalogItemMockFactory() {
			@Override
			protected void populateMockData() {
				super.populateMockData();
				featureFilter("com.foo.bar.feature", "[1.2,2.0)"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		Map<String, Version> featureToVersion = new HashMap<>();
		connectorDiscovery.setFeatureToVersion(featureToVersion);
		connectorDiscovery.performDiscovery(new NullProgressMonitor());

		assertTrue(connectorDiscovery.getItems().isEmpty());
	}

	@Test
	public void testCategorization() {
		connectorDiscovery.performDiscovery(new NullProgressMonitor());
		assertTrue(!connectorDiscovery.getItems().isEmpty());
		assertTrue(!connectorDiscovery.getCategories().isEmpty());

		for (CatalogItem connector : connectorDiscovery.getItems()) {
			assertNotNull(connector.getCategory());
			assertEquals(connector.getCategoryId(), connector.getCategory().getId());
			assertTrue(connector.getCategory().getItems().contains(connector));
		}
	}

	@Test
	public void testMultipleStrategies() {
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
