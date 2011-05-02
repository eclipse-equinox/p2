/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.util.*;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class FragmentMethodTest extends TestCase {
	private static final String PROP_FRAG = "propFrag";
	private static final String PROP_IU = "propIU";
	private static final String TEST_REQUIRED = "testRequired";
	IInstallableUnit iu1;
	IInstallableUnit iu3;
	Collection<IProvidedCapability> iu1Caps;
	Collection<IProvidedCapability> iu3Caps;

	protected void setUp() throws Exception {
		super.setUp();
		iu1 = createIU("iu.test1");
		iu3 = createIUFragment("iu.fragment");
		iu1Caps = iu1.getProvidedCapabilities();
		iu3Caps = iu3.getProvidedCapabilities();
		HashSet hash = new HashSet();
		hash.add(iu1);
		hash.add(iu3);
		//		Collection result = new ResolutionHelper(new Hashtable(), null).attachCUs(hash);
		for (Iterator iterator = hash.iterator(); iterator.hasNext();) {
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
		iu1Caps = null;
		iu3Caps = null;
		super.tearDown();
	}

	public void testCapabilities() {
		Collection<IProvidedCapability> mergedCapabilities = iu1.getProvidedCapabilities();
		for (IProvidedCapability capability : mergedCapabilities) {
			FragmentTest.assertContainsWithEquals(mergedCapabilities, capability);
		}

		//The fragment property is not set
		assertNull(iu1.getProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT));

		//The fragment does not contain iu namespace
		Collection<IProvidedCapability> initialFragmentCapabilities = iu3Caps;
		for (IProvidedCapability capability : initialFragmentCapabilities) {
			if (capability.getNamespace().equals(IInstallableUnit.NAMESPACE_IU_ID)) {
				assertDoesNotContain(mergedCapabilities, capability);
				break;
			}
		}

		assertEquals("The fragment capabilities should not change", initialFragmentCapabilities, iu3.getProvidedCapabilities());
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

	protected void assertEquals(String message, Collection<? extends Object> expected, Collection<? extends Object> actual) {
		if (expected == null && actual == null)
			return;
		if (expected == actual)
			return;
		if (expected == null || actual == null)
			assertTrue(message + ".1", false);
		if (expected.size() != actual.size())
			assertTrue(message + ".2", false);
		if (!expected.containsAll(actual))
			fail(message + ".3");
	}

	public static void assertDoesNotContain(Collection<? extends Object> objects, Object searched) {
		if (objects.contains(searched))
			throw new AssertionFailedError("The array should not contain the searched element");
	}

	public void testProperties() {
		assertNotNull("The property is missing", iu3.getProperty(PROP_FRAG));
		assertNotNull("The property is missing", iu1.getProperty(PROP_IU));
		assertNull("The property should not be available", iu1.getProperty("doesnotexist"));
	}

	public IInstallableUnit createIUFragment(String name) {
		InstallableUnitFragmentDescription iu = new InstallableUnitFragmentDescription();
		iu.setId(name);
		iu.setVersion(Version.createOSGi(1, 0, 0));
		iu.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		iu.setProperty(PROP_FRAG, "value");
		IRequirement[] reqs = new IRequirement[] {MetadataFactory.createRequirement("eclipse.touchpoint", "bundle", VersionRange.emptyRange, null, false, true), MetadataFactory.createRequirement(TEST_REQUIRED, TEST_REQUIRED, VersionRange.emptyRange, null, true, false)};
		iu.setHost(reqs);
		IProvidedCapability[] cap = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("testCapabilityInFragment", "testCapabilityInFragment", Version.createOSGi(1, 0, 0))};
		iu.setCapabilities(cap);
		return MetadataFactory.createInstallableUnitFragment(iu);
	}

	public IInstallableUnit createIU(String name) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(name);
		iu.setVersion(Version.createOSGi(1, 0, 0));
		iu.setTouchpointType(AbstractProvisioningTest.TOUCHPOINT_OSGI);
		iu.setProperty(PROP_IU, "valueIU");
		IProvidedCapability[] cap = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("eclipse.touchpoint", "bundle", Version.createOSGi(1, 0, 0)), MetadataFactory.createProvidedCapability("testCapability", "testCapability", Version.createOSGi(1, 0, 0))};
		iu.setCapabilities(cap);
		return MetadataFactory.createInstallableUnit(iu);
	}
}
