/*******************************************************************************
 * Copyright (c) 2013, 2018 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.discovery.tests.core.util;

import static org.junit.Assert.assertEquals;

import org.eclipse.equinox.internal.p2.discovery.compatibility.util.TransportUtil;
import org.junit.Test;

public class TransportUtilTest {
	@Test
	public void testGetFileNameForJar() throws Exception {
		assertEquals("lib_1.0.jar", TransportUtil.getFileNameFor("lib-1.0.jar")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testGetFileNameForUrl() throws Exception {
		assertEquals("lib_1.0.jar", TransportUtil //$NON-NLS-1$
				.getFileNameFor("http://www.eclipse.org/downloads/download.php?file=/discovery/lib-1.0.jar")); //$NON-NLS-1$
	}

	@Test
	public void testGetFileNameForUrlWithQuery() throws Exception {
		assertEquals("lib_1.0.jar_r_1_protocol_http", TransportUtil.getFileNameFor( //$NON-NLS-1$
				"http://www.eclipse.org/downloads/download.php?file=/discovery/lib-1.0.jar&r=1&protocol=http")); //$NON-NLS-1$
	}

	@Test
	public void testGetFileNameForUrlEndingWithSlash() throws Exception {
		assertEquals("a.jar", TransportUtil.getFileNameFor("a.jar/")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testGetFileNameForUrlWithFilesystemReservedCharacters() throws Exception {
		assertEquals("1_2_3_4_5_6_7_8_9_", TransportUtil.getFileNameFor("1<2>3:4\"5\\6|7?8*9+")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}