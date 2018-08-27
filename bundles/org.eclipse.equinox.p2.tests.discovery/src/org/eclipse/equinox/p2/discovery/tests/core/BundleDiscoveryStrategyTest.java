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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.Policy;
import org.eclipse.equinox.internal.p2.discovery.model.*;
import org.eclipse.equinox.p2.discovery.tests.core.mock.MockBundleDiscoveryStrategy;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David Green
 * @author Steffen Pingel
 */
public class BundleDiscoveryStrategyTest {

	private MockBundleDiscoveryStrategy discoveryStrategy;

	private final List<CatalogCategory> categories = new ArrayList<>();

	private final List<CatalogItem> connectors = new ArrayList<>();

	private final List<Certification> certifications = new ArrayList<>();

	private final List<Tag> tags = new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		discoveryStrategy = new MockBundleDiscoveryStrategy();
		discoveryStrategy.setPolicy(new Policy(true));
		discoveryStrategy.setCategories(categories);
		discoveryStrategy.setItems(connectors);
		discoveryStrategy.setCertifications(certifications);
		discoveryStrategy.setTags(tags);
	}

	@Test
	public void testDiscovery() throws CoreException {
		discoveryStrategy.performDiscovery(new NullProgressMonitor());

		assertFalse(categories.isEmpty());
		assertFalse(connectors.isEmpty());
		CatalogCategory category = findCategoryById("org.eclipse.mylyn.discovery.tests.connectorCategory1"); //$NON-NLS-1$
		assertNotNull(category);
		CatalogItem connector = findConnectorById("org.eclipse.mylyn.discovery.tests.connectorDescriptor1"); //$NON-NLS-1$
		assertNotNull(connector);
		Certification certification = findCertificationById("org.eclipse.mylyn.discovery.tests.certification1"); //$NON-NLS-1$
		assertNotNull(certification);
	}

	@Test
	public void testCustomTag() throws CoreException {
		discoveryStrategy.performDiscovery(new NullProgressMonitor());

		CatalogItem connector = findConnectorById("org.eclipse.mylyn.discovery.test.tagged"); //$NON-NLS-1$
		assertEquals(new HashSet<>(Arrays.asList(new Tag("Custom", "Custom"))), connector.getTags()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Arrays.asList(new Tag("task", "Tasks"), new Tag("Custom", "Custom")), discoveryStrategy.getTags()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
	}

	@Test
	public void testDiscoveryNoCategoriesPolicy() throws CoreException {
		discoveryStrategy.setPolicy(new Policy(false));
		discoveryStrategy.performDiscovery(new NullProgressMonitor());

		assertTrue(categories.isEmpty());
	}

	private CatalogItem findConnectorById(String id) {
		for (CatalogItem descriptor : connectors) {
			if (id.equals(descriptor.getId())) {
				return descriptor;
			}
		}
		return null;
	}

	private CatalogCategory findCategoryById(String id) {
		for (CatalogCategory descriptor : categories) {
			if (id.equals(descriptor.getId())) {
				return descriptor;
			}
		}
		return null;
	}

	private Certification findCertificationById(String id) {
		for (Certification descriptor : certifications) {
			if (id.equals(descriptor.getId())) {
				return descriptor;
			}
		}
		return null;
	}

}
