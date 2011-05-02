/*******************************************************************************
 * Copyright (c) 2004, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.discovery.tests.core.util;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.util.CatalogCategoryComparator;

public class CatalogCategoryComparatorTest extends TestCase {

	private CatalogCategoryComparator comparator;

	private CatalogCategory category1;

	private CatalogCategory category2;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		comparator = new CatalogCategoryComparator();
		category1 = new CatalogCategory();
		category2 = new CatalogCategory();
	}

	public void testSortByRelevanceInequal() {
		category1.setRelevance("100"); //$NON-NLS-1$
		category2.setRelevance("50"); //$NON-NLS-1$
		assertEquals(-1, comparator.compare(category1, category2));
		assertEquals(1, comparator.compare(category2, category1));
	}

	public void testSortByRelevanceOneNotSpecified() {
		category1.setRelevance("10"); //$NON-NLS-1$
		assertEquals(-1, comparator.compare(category1, category2));
		assertEquals(1, comparator.compare(category2, category1));
	}

	public void testSortByRelevanceSame() {
		category1.setRelevance("10"); //$NON-NLS-1$
		category1.setName("test"); //$NON-NLS-1$
		category1.setId("1"); //$NON-NLS-1$
		category2.setRelevance("10"); //$NON-NLS-1$
		category2.setName("test"); //$NON-NLS-1$
		category2.setId("1"); //$NON-NLS-1$
		assertEquals(0, comparator.compare(category1, category2));
		assertEquals(0, comparator.compare(category2, category1));
	}

	public void testSortByRelevanceSameIdsDiffer() {
		category1.setRelevance("10"); //$NON-NLS-1$
		category1.setName("test"); //$NON-NLS-1$
		category1.setId("a"); //$NON-NLS-1$
		category2.setRelevance("10"); //$NON-NLS-1$
		category2.setName("test"); //$NON-NLS-1$
		category2.setId("b"); //$NON-NLS-1$
		assertEquals(-1, comparator.compare(category1, category2));
		assertEquals(1, comparator.compare(category2, category1));
	}

	public void testSortByRelevanceSameNamesDiffer() {
		category1.setRelevance("10"); //$NON-NLS-1$
		category1.setName("a"); //$NON-NLS-1$
		category1.setId("a"); //$NON-NLS-1$
		category2.setRelevance("10"); //$NON-NLS-1$
		category2.setName("b"); //$NON-NLS-1$
		category2.setId("a"); //$NON-NLS-1$
		assertEquals(-1, comparator.compare(category1, category2));
		assertEquals(1, comparator.compare(category2, category1));
	}
}
