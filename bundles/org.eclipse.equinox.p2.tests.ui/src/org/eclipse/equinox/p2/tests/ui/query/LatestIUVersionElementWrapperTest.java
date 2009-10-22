/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;

import java.util.*;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.LatestIUVersionQuery;

/**
 * Tests for {@link LatestIUVersionQuery}. This has all the tests of the superclass,
 * plus some extras for testing the latest IU capabilities.
 */
public class LatestIUVersionElementWrapperTest extends AvailableIUWrapperTest {

	/**
	 * Returns the IU corresponding to the collected element.
	 */
	protected IInstallableUnit getIU(Object collected) {
		if (collected instanceof IInstallableUnit)
			return (IInstallableUnit) collected;
		return ((IIUElement) collected).getIU();
	}

	/**
	 * Tests collecting items that LatestIUVersionElementQuery should
	 * discard. 
	 */
	public void testCollectObject() {
		LatestIUVersionQuery latestIuVersionElementQuery = new LatestIUVersionQuery();
		Object object = new Object();
		List list = new ArrayList();
		list.add(object);
		Collector collector = latestIuVersionElementQuery.perform(list.iterator(), new Collector());
		assertEquals("1.0", 0, collector.size());
	}

	/**
	 * Tests that only the latest version is collected.
	 */
	public void testCollectLatestIU() {
		LatestIUVersionQuery latestIuVersionElementQuery = new LatestIUVersionQuery();
		IInstallableUnit unit1 = createIU("f1", Version.createOSGi(1, 0, 0));
		IInstallableUnit unit2 = createIU("f1", Version.createOSGi(1, 0, 1));
		List listOfIUs = new ArrayList();
		listOfIUs.add(unit1);
		listOfIUs.add(unit2);
		Collector collector = latestIuVersionElementQuery.perform(listOfIUs.iterator(), new Collector());
		assertEquals("1.0", 1, collector.size());
		IInstallableUnit collectedIU = getIU(collector.iterator().next());
		assertEquals("1.1", unit2, collectedIU);
	}

	public void testMultipleIUsAndVersions() {
		LatestIUVersionQuery latestIuVersionElementQuery = new LatestIUVersionQuery();
		IInstallableUnit unit1 = createIU("A", Version.createOSGi(1, 0, 0));
		IInstallableUnit unit2 = createIU("A", Version.createOSGi(1, 0, 1));
		IInstallableUnit unit3 = createIU("B", Version.createOSGi(1, 0, 1));
		IInstallableUnit unit4 = createIU("B", Version.createOSGi(0, 1, 1));
		IInstallableUnit unit5 = createIU("C", Version.createOSGi(0, 1, 1));

		// We should get unit 2, unit 3 and unit 5 
		List listOfIUs = new ArrayList();
		listOfIUs.add(unit1);
		listOfIUs.add(unit2);
		listOfIUs.add(unit3);
		listOfIUs.add(unit4);
		listOfIUs.add(unit5);
		Collector collector = latestIuVersionElementQuery.perform(listOfIUs.iterator(), new Collector());

		// Should be 3  units
		assertEquals("1.0", 3, collector.size());
		Collection reslts = collector.toCollection();
		assertTrue("1.2", reslts.contains(unit2));
		assertTrue("1.3", reslts.contains(unit3));
		assertTrue("1.4", reslts.contains(unit5));

	}
}
