/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
 *		EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import static org.junit.Assert.assertEquals;

import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.junit.Test;

/**
 * Tests for License class
 */
public class LicenseTest {
	@Test
	public void testNormalize() {
		ILicense licenseOne = MetadataFactory.createLicense(null, "a   b");
		ILicense licenseTwo = MetadataFactory.createLicense(null, "a\t\n\r  \t\n\r  b");
		assertEquals("Licenses with normalized whitespace should have same UUID", licenseOne.getUUID(), licenseTwo.getUUID());

		licenseOne = MetadataFactory.createLicense(null, "   a b  c  ");
		licenseTwo = MetadataFactory.createLicense(null, "a\t\nb\r  \t\n\r  c");
		assertEquals("Licenses with normalized whitespace (multiple words) should have same UUID", licenseOne.getUUID(), licenseTwo.getUUID());
	}
}
