/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Black box tests for API of {@link org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit}.
 */
public class InstallableUnitTest extends AbstractProvisioningTest {
	/**
	 * Tests for {@link org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit#satisfies(org.eclipse.equinox.internal.provisional.p2.metadata.RequiredCapability)}.
	 */
	public void testSatisfies() {
		IProvidedCapability[] provides = new IProvidedCapability[] {MetadataFactory.createProvidedCapability("testNamespace", "name", Version.createOSGi(1, 0, 0))};
		IInstallableUnit iu = createIU("iu", provides);

		IRequirement wrongNamespace = MetadataFactory.createRequirement("wrongNamespace", "name", VersionRange.emptyRange, null, false, false);
		IRequirement wrongName = MetadataFactory.createRequirement("testNamespace", "wrongName", VersionRange.emptyRange, null, false, false);
		IRequirement lowerVersionRange = MetadataFactory.createRequirement("testNamespace", "name", new VersionRange("[0.1,1.0)"), null, false, false);
		IRequirement higherVersionRange = MetadataFactory.createRequirement("testNamespace", "name", new VersionRange("(1.0,99.99]"), null, false, false);
		IRequirement match = MetadataFactory.createRequirement("testNamespace", "name", new VersionRange("[1.0,2.0)"), null, false, false);

		assertFalse("1.0", iu.satisfies(wrongNamespace));
		assertFalse("1.1", iu.satisfies(wrongName));
		assertFalse("1.2", iu.satisfies(lowerVersionRange));
		assertFalse("1.3", iu.satisfies(higherVersionRange));
		assertTrue("1.4", iu.satisfies(match));
	}
}
