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

import java.util.*;
import org.eclipse.equinox.internal.p2.ui.model.CategoryElement;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.query.AvailableIUCollector;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.p2.tests.MockQueryable;
import org.osgi.framework.Version;

/**
 * Tests for {@link AvailableIUCollector}.
 */
public class AvailableIUCollectorTest extends QueryTest {
	protected AvailableIUCollector createCollector() {
		return createCollector(true);
	}

	protected AvailableIUCollector createCollector(boolean makeCategories) {
		return new AvailableIUCollector(new MockQueryable(), null, makeCategories, true);
	}

	/**
	 * Returns the IU corresponding to the collected element.
	 */
	protected IInstallableUnit getIU(Object collected) {
		return ((IIUElement) collected).getIU();
	}

	/**
	 * Tests collecting items AvailableIUCollector doesn't care about.
	 */
	public void testCollectObject() {
		AvailableIUCollector collector = createCollector();
		Object object = new Object();
		collector.accept(object);
		assertEquals("1.0", 1, collector.size());
		assertEquals("1.1", object, collector.iterator().next());
	}

	/**
	 * Tests collecting an IU.
	 */
	public void testCollectIU() {
		AvailableIUCollector collector = createCollector();
		IInstallableUnit unit = createIU("f1");
		collector.accept(unit);
		assertEquals("1.0", 1, collector.size());
		IInstallableUnit collectedIU = getIU(collector.iterator().next());
		assertEquals("1.1", unit, collectedIU);
	}

	/**
	 * Tests collecting a category when makeCategory=true.
	 */
	public void testMakeCategory() {
		AvailableIUCollector collector = createCollector(true);
		Map properties = new HashMap();
		properties.put(IInstallableUnit.PROP_TYPE_CATEGORY, "true");
		IInstallableUnit category = createIU("category", new Version(1, 0, 0), NO_REQUIRES, properties, false);
		IInstallableUnit unit = createIU("basicIU");
		collector.accept(category);
		collector.accept(unit);
		assertEquals("1.0", 2, collector.size());
		boolean categoryFound = false;
		for (Iterator it = collector.iterator(); it.hasNext();) {
			Object element = it.next();
			IInstallableUnit collected = getIU(element);
			if (collected.equals(category)) {
				categoryFound = true;
				assertTrue("1.1", element instanceof CategoryElement);
			} else {
				assertEquals("1.2", unit, collected);
			}
		}
		assertTrue("1.3", categoryFound);
	}

	/**
	 * Tests collecting a category when makeCategory=false
	 */
	public void testNoMakeCategory() {
		AvailableIUCollector collector = createCollector(false);
		Map properties = new HashMap();
		properties.put(IInstallableUnit.PROP_TYPE_CATEGORY, "true");
		IInstallableUnit category = createIU("category", new Version(1, 0, 0), NO_REQUIRES, properties, false);
		IInstallableUnit unit = createIU("basicIU");
		collector.accept(category);
		collector.accept(unit);
		assertEquals("1.0", 2, collector.size());
		boolean categoryFound = false;
		for (Iterator it = collector.iterator(); it.hasNext();) {
			Object element = it.next();
			IInstallableUnit collected = getIU(element);
			if (collected.equals(category)) {
				categoryFound = true;
				assertFalse("1.1", element instanceof CategoryElement);
			} else {
				assertEquals("1.2", unit, collected);
			}
		}
		assertTrue("1.3", categoryFound);
	}

	/**
	 * Tests hiding installed IUs.
	 */
	public void testHideInstalled() {
		IProfile profile = createProfile("TestProfile");

		AvailableIUCollector collector = createCollector(true);
		IInstallableUnit installed = createIU("installed");
		IInstallableUnit notInstalled = createIU("notInstalled");
		install(profile, new IInstallableUnit[] {installed}, true, createPlanner(), createEngine());
		collector.markInstalledIUs(profile, true);

		//now feed in the installed and non-installed units, and the installed unit should be ignored.
		collector.accept(installed);
		collector.accept(notInstalled);
		assertEquals("1.1", 1, collector.size());
		Object iuElement = collector.iterator().next();
		assertEquals("1.2", notInstalled, getIU(iuElement));
	}

	protected Query getMockQuery() {
		return new IUPropertyQuery("key", "value");
	}
}
