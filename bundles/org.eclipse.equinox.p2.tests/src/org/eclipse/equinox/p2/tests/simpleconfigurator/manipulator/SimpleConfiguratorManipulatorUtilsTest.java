/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator.manipulator;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.equinox.internal.simpleconfigurator.manipulator.SimpleConfiguratorManipulatorUtils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.BundleInfo;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleConfiguratorManipulatorUtilsTest extends AbstractProvisioningTest {

	public void testWriteBundleInfoLine() throws URISyntaxException {
		String expectedLine = "javax.servlet,2.4.0.v200806031604,plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo bundleInfo = new BundleInfo("javax.servlet", "2.4.0.v200806031604", new URI("plugins/javax.servlet_2.4.0.v200806031604.jar"), 4, false);
		String line = SimpleConfiguratorManipulatorUtils.createBundleInfoLine(bundleInfo, false);
		assertEquals(expectedLine, line);
		assertEquals(bundleInfo, SimpleConfiguratorUtils.parseBundleInfoLine(line, null));
	}

	public void testWriteBundleInfoLineWithComma() throws URISyntaxException {
		String expectedLine = "javax.servlet,2.4.0.v200806031604,plugin%2Cs/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo bundleInfo = new BundleInfo("javax.servlet", "2.4.0.v200806031604", new URI("plugin,s/javax.servlet_2.4.0.v200806031604.jar"), 4, false);
		String line = SimpleConfiguratorManipulatorUtils.createBundleInfoLine(bundleInfo, false);
		assertEquals(expectedLine, line);
		assertEquals(bundleInfo, SimpleConfiguratorUtils.parseBundleInfoLine(line, null));
	}

	public void testWriteBundleInfoLineWithSpace() throws URISyntaxException {
		String expectedLine = "javax.servlet,2.4.0.v200806031604,plugin%20s/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo bundleInfo = new BundleInfo("javax.servlet", "2.4.0.v200806031604", new URI("plugin%20s/javax.servlet_2.4.0.v200806031604.jar"), 4, false);
		String line = SimpleConfiguratorManipulatorUtils.createBundleInfoLine(bundleInfo, false);
		assertEquals(expectedLine, line);
		assertEquals(bundleInfo, SimpleConfiguratorUtils.parseBundleInfoLine(line, null));
	}

	public void testWriteBundleInfoLineWithAtSymbol() throws URISyntaxException {
		String expectedLine = "javax.servlet,2.4.0.v200806031604,plugin@s/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo bundleInfo = new BundleInfo("javax.servlet", "2.4.0.v200806031604", new URI("plugin@s/javax.servlet_2.4.0.v200806031604.jar"), 4, false);
		String line = SimpleConfiguratorManipulatorUtils.createBundleInfoLine(bundleInfo, false);
		assertEquals(expectedLine, line);
		assertEquals(bundleInfo, SimpleConfiguratorUtils.parseBundleInfoLine(line, null));
	}

	public void testWriteVersionLine() {
		String expectedVersionPrefix = "#version=";
		String versionLine = SimpleConfiguratorManipulatorUtils.createVersionLine();
		assertTrue(versionLine.startsWith(expectedVersionPrefix));

		SimpleConfiguratorUtils.parseCommentLine(versionLine);
		try {
			SimpleConfiguratorUtils.parseCommentLine(expectedVersionPrefix + "999");
		} catch (IllegalArgumentException e) {
			return;
		}
		fail("improper version error not caught");
	}
}
