/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.omniVersion;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Tests the OmniVersion implementation of Version and VersionRange.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CommonPatternsTest.class, FormatArrayTest.class, FormatATest.class, FormatDTest.class,
		FormatNTest.class, FormatProcessingTest.class, FormatPTest.class, FormatQTest.class, FormatRTest.class,
		FormatSTest.class, FormatTest.class, FormatRangeTest.class, MultiplicityTest.class, OSGiRangeTest.class,
		OSGiVersionTest.class, RawRangeTest.class, RawRangeWithOriginalTest.class, RawVersionTest.class,
		RawWithOriginalTest.class, IntersectionTest.class })
public class AllTests {
// test suite
}
