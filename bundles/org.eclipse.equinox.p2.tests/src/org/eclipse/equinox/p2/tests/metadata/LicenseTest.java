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
package org.eclipse.equinox.p2.tests.metadata;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.provisional.p2.metadata.ILicense;
import org.eclipse.equinox.internal.provisional.p2.metadata.License;

/**
 * Tests for License class
 */
public class LicenseTest extends TestCase {
	public void testNormalize() {
		ILicense licenseOne = new License(null, "a   b");
		ILicense licenseTwo = new License(null, "a\t\n\r  \t\n\r  b");
		assertEquals("1.0", licenseOne.getDigest(), licenseTwo.getDigest());

		licenseOne = new License(null, "   a b  c  ");
		licenseTwo = new License(null, "a\t\nb\r  \t\n\r  c");
		assertEquals("1.1", licenseOne.getDigest(), licenseTwo.getDigest());
	}
}
