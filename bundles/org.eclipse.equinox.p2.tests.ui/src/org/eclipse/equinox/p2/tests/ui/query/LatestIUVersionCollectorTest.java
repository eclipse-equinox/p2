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

import org.eclipse.equinox.internal.p2.ui.model.IUElement;
import org.eclipse.equinox.internal.p2.ui.query.AvailableIUCollector;
import org.eclipse.equinox.internal.p2.ui.query.LatestIUVersionCollector;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.tests.MockQueryable;
import org.osgi.framework.Version;

/**
 * Tests for {@link LatestIUVersionCollector}. This has all the tests of the superclass,
 * plus some extras for testing the latest IU capabilities.
 */
public class LatestIUVersionCollectorTest extends AvailableIUCollectorTest {

	protected AvailableIUCollector createCollector(boolean makeCategories) {
		return new LatestIUVersionCollector(new MockQueryable(), null, makeCategories);
	}

	/**
	 * Returns the IU corresponding to the collected element.
	 */
	protected IInstallableUnit getIU(Object collected) {
		if (collected instanceof IInstallableUnit)
			return (IInstallableUnit) collected;
		return ((IUElement) collected).getIU();
	}

	/**
	 * Tests collecting items AvailableIUCollector doesn't care about.
	 */
	public void testCollectObject() {
		AvailableIUCollector collector = createCollector();
		Object object = new Object();
		collector.accept(object);
		assertEquals("1.0", 0, collector.size());
	}

	/**
	 * Tests that only the latest version is collected.
	 */
	public void testCollectLatestIU() {
		AvailableIUCollector collector = createCollector();
		IInstallableUnit unit1 = createIU("f1", new Version(1, 0, 0));
		IInstallableUnit unit2 = createIU("f1", new Version(1, 0, 1));
		collector.accept(unit1);
		collector.accept(unit2);
		assertEquals("1.0", 1, collector.size());
		IInstallableUnit collectedIU = getIU(collector.iterator().next());
		assertEquals("1.1", unit2, collectedIU);
	}
}
