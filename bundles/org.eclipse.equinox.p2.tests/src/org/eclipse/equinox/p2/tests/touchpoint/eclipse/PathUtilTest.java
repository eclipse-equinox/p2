/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.equinox.internal.p2.update.PathUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Tests for {@link org.eclipse.equinox.internal.p2.update.PathUtil}.
 */
public class PathUtilTest extends AbstractProvisioningTest {
	/** Constant value indicating if the current platform is Windows */
	private static final boolean WINDOWS = java.io.File.separatorChar == '\\';

	public void testMakeRelative() throws MalformedURLException {
		if (!WINDOWS)
			return;
		Object[][] data = new Object[][] {
				// simple path
				new Object[] {"file:/c:/a/b", new URL("file:/c:/a/x"), "file:../b"},
				// common root
				new Object[] {"file:/c:/eclipse/plugins/foo.jar", new URL("file:/c:/eclipse/"), "file:plugins\\foo.jar"},
				// different drives
				new Object[] {"file:/c:/a/b", new URL("file:/d:/a/x"), "file:/c:/a/b"}, //
				new Object[] {"file:/c:/eclipse/plugins/foo.jar", new URL("file:/d:/eclipse/"), "file:/c:/eclipse/plugins/foo.jar"},
				// non-local
				new Object[] {"http:/c:/a/b", new URL("file:/c:/a/x"), "http:/c:/a/b"}, //
				new Object[] {"file:/c:/a/b", new URL("http:/c:/a/x"), "file:/c:/a/b"}, //
				//
				new Object[] {"file:/c:/a/b", new URL("file:/C:/a/x"), "file:../b"}, //
				new Object[] {"file:/c:/", new URL("file:/d:/"), "file:/c:/"}, //
				new Object[] {"file:/c:/", new URL("file:/c:/"), "file:/c:/"}, //
		};
		for (int i = 0; i < data.length; i++) {
			String location = data[i][0].toString();
			URL root = (URL) data[i][1];
			String expected = data[i][2].toString();
			String actual = PathUtil.makeRelative(location, root);
			assertEquals("2." + Integer.toString(i), expected, actual);
		}
	}
}
