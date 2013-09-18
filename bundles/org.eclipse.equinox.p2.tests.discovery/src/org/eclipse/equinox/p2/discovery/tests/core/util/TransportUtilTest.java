/*******************************************************************************
 * Copyright (c) 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.discovery.tests.core.util;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.discovery.compatibility.util.TransportUtil;

public class TransportUtilTest extends TestCase {

	public void testGetFileNameForJar() throws Exception {
		assertEquals("lib_1.0.jar", TransportUtil.getFileNameFor("lib-1.0.jar")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testGetFileNameForUrl() throws Exception {
		assertEquals("lib_1.0.jar", TransportUtil.getFileNameFor("http://www.eclipse.org/downloads/download.php?file=/discovery/lib-1.0.jar")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testGetFileNameForUrlWithQuery() throws Exception {
		assertEquals("lib_1.0.jar_r_1_protocol_http", TransportUtil.getFileNameFor("http://www.eclipse.org/downloads/download.php?file=/discovery/lib-1.0.jar&r=1&protocol=http")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testGetFileNameForUrlEndingWithSlash() throws Exception {
		assertEquals("a.jar", TransportUtil.getFileNameFor("a.jar/")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testGetFileNameForUrlWithFilesystemReservedCharacters() throws Exception {
		assertEquals("1_2_3_4_5_6_7_8_9_", TransportUtil.getFileNameFor("1<2>3:4\"5\\6|7?8*9+")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}