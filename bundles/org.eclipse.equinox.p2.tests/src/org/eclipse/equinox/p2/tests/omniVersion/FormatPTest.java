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

public class FormatPTest extends TestCase {
	public void testPad() {
		Version v = Version.parseVersion("format(qp):''pm");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:''pm"), v);
	}

	public void testArrayPad() {
		Version v = Version.parseVersion("format(r):<''pm>");
		assertNotNull(v);
		assertEquals(Version.parseVersion("raw:<''pm>"), v);
	}

}
