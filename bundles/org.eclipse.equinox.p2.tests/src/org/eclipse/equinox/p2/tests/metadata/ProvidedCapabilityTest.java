/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for {@link IProvidedCapability}.
 */
public class ProvidedCapabilityTest extends AbstractProvisioningTest {
	public void testEquals() {
		IProvidedCapability cap = MetadataFactory.createProvidedCapability("namespace", "name", DEFAULT_VERSION);
		IProvidedCapability equal = MetadataFactory.createProvidedCapability("namespace", "name", DEFAULT_VERSION);
		IProvidedCapability notEqual = MetadataFactory.createProvidedCapability("namespace", "name", Version.createOSGi(2, 0, 0));
		assertEquals("1.0", cap, equal);
		assertFalse("1.1", cap.equals(notEqual));
		assertFalse("1.1", notEqual.equals(cap));
	}

	public void testProperties_Unmodifiable() {
		String namespace = "aNamespace";
		String name = "name";
		Version version = Version.createOSGi(2, 0, 0);

		Map properties = new HashMap<>();
		properties.put(namespace, name);
		properties.put(IProvidedCapability.PROPERTY_VERSION, version);

		IProvidedCapability capability1 = MetadataFactory.createProvidedCapability(namespace, properties);
		IProvidedCapability capability2 = MetadataFactory.createProvidedCapability(namespace, name, version);
		assertEquals(capability1, capability2);

		try {
			capability1.getProperties().put("key", "value");
			fail("properties must be unmodifiable");
		} catch (UnsupportedOperationException e) {
			// ok
		}

		try {
			capability2.getProperties().put("key", "value");
			fail("properties must be unmodifiable");
		} catch (UnsupportedOperationException e) {
			// ok
		}
	}

	public void testProperties_Immutable() {
		String namespace = "aNamespace";
		String name = "name";
		Version version = Version.createOSGi(2, 0, 0);

		Map properties = new HashMap<>();
		properties.put(namespace, name);
		properties.put(IProvidedCapability.PROPERTY_VERSION, version);

		IProvidedCapability capability1 = MetadataFactory.createProvidedCapability(namespace, properties);
		IProvidedCapability capability2 = MetadataFactory.createProvidedCapability(namespace, name, version);

		// mutate original value
		properties.put(IProvidedCapability.PROPERTY_VERSION, Version.createOSGi(9, 9, 9));

		assertEquals(capability1, capability2);
	}

	public void testProperties_NoVersion() {
		String namespace = "aNamespace";
		String name = "name";

		Map properties = new HashMap<>();
		properties.put(namespace, name);
		// no version this time

		IProvidedCapability capability1 = MetadataFactory.createProvidedCapability(namespace, properties);
		IProvidedCapability capability2 = MetadataFactory.createProvidedCapability(namespace, name, null);
		assertEquals(capability1, capability2);
	}
}
