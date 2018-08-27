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

import org.eclipse.equinox.p2.metadata.*;
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
}
