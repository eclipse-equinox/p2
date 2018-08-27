/*******************************************************************************
 * Copyright (c) 2004, 2018 Tasktop Technologies and others.
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

package org.eclipse.equinox.p2.discovery.tests.core.util;

import static org.junit.Assert.assertEquals;

import org.eclipse.equinox.internal.p2.discovery.model.*;
import org.eclipse.equinox.internal.p2.discovery.util.CatalogItemComparator;
import org.junit.Before;
import org.junit.Test;

public class CatalogConnectorComparatorTest {

	private CatalogCategory category;

	private CatalogItemComparator comparator;

	@Before
	public void setUp() throws Exception {
		category = new CatalogCategory();
		comparator = new CatalogItemComparator();
	}

	private Group addGroup(String id) {
		Group group = new Group();
		group.setId(id);
		category.getGroup().add(group);
		return group;
	}

	private CatalogItem addConnectorDescriptor(String id, String name, String groupId) {
		CatalogItem connector = new CatalogItem();
		connector.setId(id);
		connector.setName(name);
		connector.setGroupId(groupId);
		connector.setCategory(category);
		category.getItems().add(connector);
		return connector;
	}

	@Test
	public void testOrderByGroup() {
		addGroup("1"); //$NON-NLS-1$
		addGroup("2"); //$NON-NLS-1$
		CatalogItem t1 = addConnectorDescriptor("b", "btest", "2"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		CatalogItem t2 = addConnectorDescriptor("a", "atest", "2"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		CatalogItem t3 = addConnectorDescriptor("c", "ctest", "1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		CatalogItem t4 = addConnectorDescriptor("d", "dtest", "1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		CatalogItem t5 = addConnectorDescriptor("0", "0test", null); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals(-1, comparator.compare(t2, t1));
		assertEquals(1, comparator.compare(t1, t2));
		assertEquals(-1, comparator.compare(t3, t4));
		assertEquals(1, comparator.compare(t4, t3));

		assertEquals(-1, comparator.compare(t1, t5));
		assertEquals(1, comparator.compare(t5, t1));
		assertEquals(-1, comparator.compare(t2, t5));
		assertEquals(1, comparator.compare(t5, t2));
		assertEquals(-1, comparator.compare(t3, t5));
		assertEquals(1, comparator.compare(t5, t3));
		assertEquals(-1, comparator.compare(t4, t5));
		assertEquals(1, comparator.compare(t5, t4));

		assertEquals(-1, comparator.compare(t3, t1));
		assertEquals(1, comparator.compare(t1, t3));
		assertEquals(-1, comparator.compare(t3, t2));
		assertEquals(1, comparator.compare(t2, t3));

		assertEquals(-1, comparator.compare(t4, t1));
		assertEquals(1, comparator.compare(t1, t4));
		assertEquals(-1, comparator.compare(t4, t2));
		assertEquals(1, comparator.compare(t2, t4));
	}
}
