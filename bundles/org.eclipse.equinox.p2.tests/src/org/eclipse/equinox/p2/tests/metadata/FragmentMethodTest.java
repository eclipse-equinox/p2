/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.util.*;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.resolution.ResolutionHelper;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.core.Version;

public class FragmentMethodTest extends TestCase {
	private static final String PROP_FRAG = "propFrag";
	private static final String PROP_IU = "propIU";
	private static final String TEST_REQUIRED = "testRequired";
	IInstallableUnit iu1;
	IInstallableUnit iu3;
	RequiredCapability[] iu1Deps;
	RequiredCapability[] iu3Deps;
	ProvidedCapability[] iu1Caps;
	ProvidedCapability[] iu3Caps;

	protected void setUp() throws Exception {
		super.setUp();
		iu1 = createIU("iu.test1");
		iu3 = createIUFragment("iu.fragment");
		iu1Caps = iu1.getProvidedCapabilities();
		iu3Caps = iu3.getProvidedCapabilities();
		iu1Deps = iu1.getRequiredCapabilities();
		iu3Deps = iu3.getRequiredCapabilities();
		HashSet hash = new HashSet();
		hash.add(iu1);
		hash.add(iu3);
		Collection result = new ResolutionHelper(new Hashtable(), null).attachCUs(hash);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			if (iu.getId().equals(iu1.getId()))
				iu1 = iu;
			if (iu.getId().equals(iu3.getId()))
				iu3 = iu;
		}
	}

	protected void tearDown() throws Exception {
		iu1 = null;
		iu3 = null;
		iu1Deps = null;
		iu3Deps = null;
		iu1Caps = null;
		iu3Caps = null;
		super.tearDown();
	}

	public void testCapabilities() {
		ProvidedCapability[] iuCapabilities = iu1Caps;
		ProvidedCapability[] initialFragmentCapabilities = iu3Caps;

		ProvidedCapability[] mergedCapabilities = iu1.getProvidedCapabilities();
		for (int i = 0; i < iuCapabilities.length; i++) {
			FragmentTest.assertContainsWithEquals(mergedCapabilities, iuCapabilities[i]);
		}

		//The fragment property is not set
		assertNull(iu1.getProperty(IInstallableUnit.PROP_TYPE_FRAGMENT));

		//The fragment does not contain iu namespace
		for (int i = 0; i < initialFragmentCapabilities.length; i++) {
			if (initialFragmentCapabilities[i].getNamespace().equals(IInstallableUnit.NAMESPACE_IU_ID)) {
				assertDoesNotContain(mergedCapabilities, initialFragmentCapabilities[i]);
				break;
			}
		}

		assertEquals("The fragment capabilities should not change", initialFragmentCapabilities, iu3.getProvidedCapabilities(), false);
	}

	protected void assertEquals(String message, Object[] expected, Object[] actual) {
		if (expected == null && actual == null)
			return;
		if (expected == null || actual == null)
			fail(message);
		if (expected.length != actual.length)
			fail(message);
		for (int i = 0; i < expected.length; i++)
			assertEquals(message, expected[i], actual[i]);
	}

	protected void assertEquals(String message, Object[] expected, Object[] actual, boolean orderImportant) {
		// if the order in the array must match exactly, then call the other method
		if (orderImportant) {
			assertEquals(message, expected, actual);
			return;
		}
		// otherwise use this method and check that the arrays are equal in any order
		if (expected == null && actual == null)
			return;
		if (expected == actual)
			return;
		if (expected == null || actual == null)
			assertTrue(message + ".1", false);
		if (expected.length != actual.length)
			assertTrue(message + ".2", false);
		boolean[] found = new boolean[expected.length];
		for (int i = 0; i < expected.length; i++) {
			for (int j = 0; j < expected.length; j++) {
				if (!found[j] && expected[i].equals(actual[j]))
					found[j] = true;
			}
		}
		for (int i = 0; i < found.length; i++)
			if (!found[i])
				assertTrue(message + ".3." + i, false);
	}

	public static void assertDoesNotContain(Object[] objects, Object searched) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i].equals(searched))
				throw new AssertionFailedError("The array should not contain the searched element");
		}
	}

	public void testProperties() {
		assertNotNull("The property is missing", iu3.getProperty(PROP_FRAG));
		assertNotNull("The property is missing", iu1.getProperty(PROP_IU));
		assertNull("The property should not be available", iu1.getProperty("doesnotexist"));
	}

	public IInstallableUnit createIUFragment(String name) {
		InstallableUnitFragmentDescription iu = new InstallableUnitFragmentDescription();
		iu.setId(name);
		iu.setVersion(new Version(1, 0, 0));
		iu.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		iu.setProperty(PROP_FRAG, "value");
		RequiredCapability[] reqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability("eclipse.touchpoint", "bundle", VersionRange.emptyRange, null, false, true), MetadataFactory.createRequiredCapability(TEST_REQUIRED, TEST_REQUIRED, VersionRange.emptyRange, null, true, false)};
		iu.setHost(reqs);
		ProvidedCapability[] cap = new ProvidedCapability[] {MetadataFactory.createProvidedCapability("testCapabilityInFragment", "testCapabilityInFragment", new Version(1, 0, 0))};
		iu.setCapabilities(cap);
		return MetadataFactory.createInstallableUnitFragment(iu);
	}

	public IInstallableUnit createIU(String name) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(name);
		iu.setVersion(new Version(1, 0, 0));
		iu.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		iu.setProperty(PROP_IU, "valueIU");
		ProvidedCapability[] cap = new ProvidedCapability[] {MetadataFactory.createProvidedCapability("eclipse.touchpoint", "bundle", new Version(1, 0, 0)), MetadataFactory.createProvidedCapability("testCapability", "testCapability", new Version(1, 0, 0))};
		iu.setCapabilities(cap);
		return MetadataFactory.createInstallableUnit(iu);
	}
}
