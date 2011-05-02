/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.*;

/**
 * Tests for latest query. This has all the tests of the superclass,
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
	 * Tests that only the latest version is collected.
	 */
	public void testCollectLatestIU() {
		IQuery latestIuVersionElementQuery = QueryUtil.createLatestIUQuery();
		IInstallableUnit unit1 = createIU("f1", Version.createOSGi(1, 0, 0));
		IInstallableUnit unit2 = createIU("f1", Version.createOSGi(1, 0, 1));
		List listOfIUs = new ArrayList();
		listOfIUs.add(unit1);
		listOfIUs.add(unit2);
		IQueryResult collector = latestIuVersionElementQuery.perform(listOfIUs.iterator());
		assertEquals("1.0", 1, queryResultSize(collector));
		IInstallableUnit collectedIU = getIU(collector.iterator().next());
		assertEquals("1.1", unit2, collectedIU);
	}

	public void testMultipleIUsAndVersions() {
		IQuery latestIuVersionElementQuery = QueryUtil.createLatestIUQuery();
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
		IQueryResult collector = latestIuVersionElementQuery.perform(listOfIUs.iterator());

		// Should be 3  units
		assertEquals("1.0", 3, queryResultSize(collector));
		assertContains("1.2", collector, unit2);
		assertContains("1.3", collector, unit3);
		assertContains("1.4", collector, unit5);

	}
}
