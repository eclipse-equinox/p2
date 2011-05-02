/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
