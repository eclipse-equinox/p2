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

import java.util.*;
import junit.framework.TestCase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.discovery.Policy;
import org.eclipse.equinox.internal.p2.discovery.model.*;
import org.eclipse.equinox.p2.discovery.tests.core.mock.MockBundleDiscoveryStrategy;

/**
 * @author David Green
 * @author Steffen Pingel
 */
public class BundleDiscoveryStrategyTest extends TestCase {

	private MockBundleDiscoveryStrategy discoveryStrategy;

	private final List<CatalogCategory> categories = new ArrayList<CatalogCategory>();

	private final List<CatalogItem> connectors = new ArrayList<CatalogItem>();

	private final List<Certification> certifications = new ArrayList<Certification>();

	private final List<Tag> tags = new ArrayList<Tag>();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		discoveryStrategy = new MockBundleDiscoveryStrategy();
		discoveryStrategy.setPolicy(new Policy(true));
		discoveryStrategy.setCategories(categories);
		discoveryStrategy.setItems(connectors);
		discoveryStrategy.setCertifications(certifications);
		discoveryStrategy.setTags(tags);
	}

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

	public void testCustomTag() throws CoreException {
		discoveryStrategy.performDiscovery(new NullProgressMonitor());

		CatalogItem connector = findConnectorById("org.eclipse.mylyn.discovery.test.tagged"); //$NON-NLS-1$
		assertEquals(new HashSet<Tag>(Arrays.asList(new Tag("Custom", "Custom"))), connector.getTags()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Arrays.asList(new Tag("task", "Tasks"), new Tag("Custom", "Custom")), discoveryStrategy.getTags()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
	}

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
