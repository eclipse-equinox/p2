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
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.MockQueryable;

/**
 * Tests for {@link CategoryElementCollector}.
 */
public class CategoryElementCollectorTest extends AbstractQueryTest {
	private CategoryElementCollector createCollector() {
		IInstallableUnit category = createIU("default category");
		return new CategoryElementCollector(new MockQueryable(category), null);
	}

	public void testCollectObject() {
		CategoryElementCollector collector = createCollector();
		collector.accept("AnObjectThatIsNotAnIU");
		assertTrue("1.0", collector.isEmpty());
	}

	/**
	 * Tests for the {@link Collector#isEmpty()} method.
	 */
	public void testIsEmpty() {
		CategoryElementCollector collector = createCollector();
		assertTrue("1.1", collector.isEmpty());

		IInstallableUnit category1 = createIU("category1");
		collector.accept(category1);
		assertTrue("1.2", !collector.isEmpty());
	}

	/**
	 * Tests for the {@link Collector#size()} method.
	 */
	public void testSize() {
		CategoryElementCollector collector = createCollector();
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
		IRequiredCapability[] required = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "category1", null);
		IInstallableUnit nested = createIU("Nested", required);
		collector.accept(nested);
		assertEquals("1.7", 1, collector.size());
	}
}
