/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.query.CategoryElementCollector;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.MockQueryable;

/**
 * Tests for {@link CategoryElementCollector}.
 */
public class CategoryElementCollectorTest extends AbstractQueryTest {
	private CategoryElementCollector createCollector(boolean showCategorized) {
		IInstallableUnit category = createIU("default category");
		return new CategoryElementCollector(new MockQueryable(category), null, showCategorized);
	}

	public void testCollectObject() {
		CategoryElementCollector collector = createCollector(false);
		collector.accept("AnObjectThatIsNotAnIU");
		assertTrue("1.0", collector.isEmpty());
	}

	/**
	 * Tests for the {@link Collector#isEmpty()} method.
	 */
	public void testIsEmpty() {
		//if we have an uncategorized category, the collector is not initially empty
		CategoryElementCollector collector = createCollector(true);
		assertTrue("1.0", !collector.isEmpty());

		//now create a collector with no uncategorized category
		collector = createCollector(false);
		assertTrue("1.1", collector.isEmpty());

		IInstallableUnit category1 = createIU("category1");
		collector.accept(category1);
		assertTrue("1.2", !collector.isEmpty());
	}

	/**
	 * Regression test for bug 256029 - multiple uncategorized categories created
	 */
	public void testBug256029() {
		//if we have an uncategorized category, the collector is not initially empty
		CategoryElementCollector collector = createCollector(true);
		assertTrue("1.0", !collector.isEmpty());
		assertEquals("1.1", collector.size(), 1);
	}

	/**
	 * Tests for the {@link Collector#size()} method.
	 */
	public void testSize() {
		//if we have an uncategorized category, the collector is not initially empty
		CategoryElementCollector collector = createCollector(true);
		assertEquals("1.0", 1, collector.size());

		//now create a collector with no uncategorized category
		collector = createCollector(false);
		assertEquals("1.1", 0, collector.size());

		IInstallableUnit category1 = createIU("category1");
		collector.accept(category1);
		assertEquals("1.2", 1, collector.size());
		assertEquals("1.3", category1, ((CategoryElement) collector.iterator().next()).getIU());
		assertEquals("1.4", category1, ((CategoryElement) collector.toCollection().iterator().next()).getIU());
		assertEquals("1.5", category1, ((CategoryElement) collector.toArray(CategoryElement.class)[0]).getIU());

		//adding the same category twice shouldn't affect size
		collector.accept(category1);
		assertEquals("1.6", 1, collector.size());

		//adding a nested category shouldn't affected size
		RequiredCapability[] required = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "category1", null);
		IInstallableUnit nested = createIU("Nested", required);
		collector.accept(nested);
		assertEquals("1.7", 1, collector.size());
	}
}
