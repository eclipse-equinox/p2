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

import java.util.Collection;
import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.query.CategoryElementWrapper;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.MockQueryable;

/**
 * Tests for {@link CategoryElementWrapper}.
 */
public class CategoryElementWrapperTest extends AbstractQueryTest {
	private CategoryElementWrapper createWrapper() {
		IInstallableUnit category = createIU("default category");
		return new CategoryElementWrapper(new MockQueryable(category), null);
	}

	private IInstallableUnit createNamedCategory(String id, String name, Version version) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(id);
		iu.setVersion(version);
		iu.setProperty(IInstallableUnit.PROP_NAME, name);
		iu.setProperty(IInstallableUnit.PROP_TYPE_CATEGORY, Boolean.toString(true));
		return MetadataFactory.createInstallableUnit(iu);
	}

	public void testCollectObject() {
		CategoryElementWrapper wrapper = createWrapper();
		Collector collector = new Collector();
		collector.accept("AnObjectThatIsNotAnIU");
		Collection results = wrapper.getElements(collector);
		assertTrue("1.0", results.isEmpty());
	}

	/**
	 * Tests for the {@link Collector#isEmpty()} method.
	 */
	public void testIsEmpty() {
		CategoryElementWrapper wrapper = createWrapper();
		Collector collector = new Collector();
		assertTrue("1.1", collector.isEmpty());

		IInstallableUnit category1 = createIU("category1");
		collector.accept(category1);
		Collection results = wrapper.getElements(collector);
		assertTrue("1.2", !results.isEmpty());
	}

	/**
	 * Tests for the {@link Collector#size()} method.
	 */
	public void testSize() {
		CategoryElementWrapper wrapper = createWrapper();
		Collector collector = new Collector();
		assertEquals("1.1", 0, collector.size());

		IInstallableUnit category1 = createIU("category1");
		collector.accept(category1);
		Collection results = wrapper.getElements(collector);
		assertEquals("1.2", 1, collector.size());
		assertEquals("1.3", category1, ((CategoryElement) results.iterator().next()).getIU());

		//adding the same category twice shouldn't affect size
		collector.accept(category1);
		results = wrapper.getElements(collector);
		assertEquals("1.6", 1, results.size());

		//adding a nested category shouldn't affected size
		IRequiredCapability[] required = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "category1", null);
		IInstallableUnit nested = createIU("Nested", required);
		collector.accept(nested);
		results = wrapper.getElements(collector);
		assertEquals("1.7", 1, results.size());
	}

	public void testCategoryMerging() {
		CategoryElementWrapper wrapper = createWrapper();
		Collector collector = new Collector();
		assertEquals("1.1", 0, collector.size());

		IInstallableUnit category1 = createNamedCategory("qualifier1.foo", "Foo", DEFAULT_VERSION);
		collector.accept(category1);
		Collection results = wrapper.getElements(collector);
		assertEquals("1.2", 1, collector.size());
		assertEquals("1.3", category1, ((CategoryElement) results.iterator().next()).getIU());

		//add a second category with different id and different name
		IInstallableUnit category2 = createNamedCategory("qualifier2.foo", "Foo", DEFAULT_VERSION);
		collector.accept(category2);
		results = wrapper.getElements(collector);
		assertEquals("1.4", 1, results.size());
	}
}
