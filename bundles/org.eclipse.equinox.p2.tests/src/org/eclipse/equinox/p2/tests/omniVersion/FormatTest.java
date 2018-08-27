/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
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

import junit.framework.TestCase;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Test of format() performing tests not covered by tests per rule.
 * 
 */
public class FormatTest extends TestCase {

	public void testEmptySegmentsRemoved() {
		Version v = Version.parseVersion("format(ndddn):1...2");
		assertNotNull(v);
		assertEquals(v.getSegment(0), Integer.valueOf(1));
		assertEquals(v.getSegment(1), Integer.valueOf(2));
	}

	public void testGreedyParsing() {
		Version v = Version.parseVersion("format(n(.n)*(.s)*):1.2.3.hello");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:1.2.3.'hello'"), v);
	}

}
