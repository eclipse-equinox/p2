/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *     Rapicorp, Inc (Pascal Rapicault) - Bug 394156 - Add support for updates from one namespace to another
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.query;

import java.net.URI;
import java.util.*;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.query.AvailableIUWrapper;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.MockQueryable;
import org.junit.Assert;

/**
 * Tests for {@link AvailableIUWrapper}.
 */
public class AvailableIUWrapperTest extends AbstractQueryTest {
	protected AvailableIUWrapper createWrapper() {
		return createWrapper(true);
	}

	protected AvailableIUWrapper createWrapper(boolean makeCategories) {
		return new AvailableIUWrapper(new MockQueryable(), null, makeCategories, true);
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
		AvailableIUWrapper wrapper = createWrapper();
		Collector<Object> collector = new Collector<Object>();
		Object object = new Object();
		collector.accept(object);
		Collection results = wrapper.getElements(collector);
		assertEquals("1.0", 1, results.size());
		assertEquals("1.1", object, results.iterator().next());
	}

	/**
	 * Tests collecting an IU.
	 */
	public void testCollectIU() {
		AvailableIUWrapper wrapper = createWrapper();
		Collector<IInstallableUnit> collector = new Collector<IInstallableUnit>();
		IInstallableUnit unit = createIU("f1");
		collector.accept(unit);
		Collection results = wrapper.getElements(collector);
		assertEquals("1.0", 1, results.size());
		IInstallableUnit collectedIU = getIU(results.iterator().next());
		assertEquals("1.1", unit, collectedIU);
	}

	/**
	 * Tests collecting a category when makeCategory=true.
	 */
	public void testMakeCategory() {
		AvailableIUWrapper wrapper = createWrapper(true);
		Collector<IInstallableUnit> collector = new Collector<IInstallableUnit>();
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(InstallableUnitDescription.PROP_TYPE_CATEGORY, "true");
		IInstallableUnit category = createIU("category", Version.createOSGi(1, 0, 0), NO_REQUIRES, properties, false);
		IInstallableUnit unit = createIU("basicIU");
		collector.accept(category);
		collector.accept(unit);

		Collection results = wrapper.getElements(collector);
		assertEquals("1.0", 2, collector.size());
		boolean categoryFound = false;
		for (Iterator it = results.iterator(); it.hasNext();) {
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
		AvailableIUWrapper wrapper = createWrapper(false);
		Collector<IInstallableUnit> collector = new Collector<IInstallableUnit>();
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(InstallableUnitDescription.PROP_TYPE_CATEGORY, "true");
		IInstallableUnit category = createIU("category", Version.createOSGi(1, 0, 0), NO_REQUIRES, properties, false);
		IInstallableUnit unit = createIU("basicIU");
		collector.accept(category);
		collector.accept(unit);

		Collection results = wrapper.getElements(collector);
		assertEquals("1.0", 2, results.size());
		boolean categoryFound = false;
		for (Iterator it = results.iterator(); it.hasNext();) {
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
		AvailableIUWrapper wrapper = createWrapper(true);
		Collector<IInstallableUnit> collector = new Collector<IInstallableUnit>();
		IInstallableUnit installed = createIU("installed");
		IInstallableUnit notInstalled = createIU("notInstalled");
		install(profile, new IInstallableUnit[] {installed}, true, createPlanner(), createEngine());
		wrapper.markInstalledIUs(profile, true);

		//now feed in the installed and non-installed units, and the installed unit should be ignored.
		collector.accept(installed);
		collector.accept(notInstalled);

		Collection results = wrapper.getElements(collector);

		assertEquals("1.1", 1, results.size());
		Object iuElement = results.iterator().next();
		assertEquals("1.2", notInstalled, getIU(iuElement));
	}

	public void testUpdate() {
		//Create the IU that will be detected as an update
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		iud.setId("NewB");
		iud.setVersion(Version.create("1.0.0"));

		String orExpression = "providedCapabilities.exists(pc | pc.namespace == 'org.eclipse.equinox.p2.iu' && pc.name == 'B')";
		IExpression expr = ExpressionUtil.parse(orExpression);
		IMatchExpression<IInstallableUnit> matchExpression = ExpressionUtil.getFactory().matchExpression(expr);

		Collection<IMatchExpression<IInstallableUnit>> updateExpression = new ArrayList<IMatchExpression<IInstallableUnit>>();
		updateExpression.add(matchExpression);
		iud.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(updateExpression, IUpdateDescriptor.HIGH, (String) null, (URI) null));
		IInstallableUnit newIUB = MetadataFactory.createInstallableUnit(iud);

		//create the IU being updated
		IInstallableUnit installed = createIU("B");

		//Setup the profile
		IProfile profile = createProfile("TestProfile");
		AvailableIUWrapper wrapper = createWrapper(true);
		Collector<IInstallableUnit> collector = new Collector<IInstallableUnit>();
		installAsRoots(profile, new IInstallableUnit[] {installed}, true, createPlanner(), createEngine());
		wrapper.markInstalledIUs(profile, true);

		//now feed in the installed and non-installed units, and the installed unit should be ignored.
		collector.accept(newIUB);

		Collection results = wrapper.getElements(collector);

		//Verify 
		assertEquals("1.1", 1, results.size());
		Object iuElement = results.iterator().next();
		assertEquals("1.2", newIUB, getIU(iuElement));
		Assert.assertTrue(((AvailableIUElement) iuElement).isUpdate());
	}

	protected IQuery getMockQuery() {
		return QueryUtil.createIUPropertyQuery("key", "value");
	}
}
