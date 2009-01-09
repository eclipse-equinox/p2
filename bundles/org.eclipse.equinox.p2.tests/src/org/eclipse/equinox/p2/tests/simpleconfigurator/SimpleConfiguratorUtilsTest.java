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
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.List;
import org.eclipse.equinox.internal.simpleconfigurator.utils.BundleInfo;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimpleConfiguratorUtilsTest extends AbstractProvisioningTest {

	private static final boolean WINDOWS = java.io.File.separatorChar == '\\';

	public void testParseBundleInfoUNC() throws MalformedURLException, URISyntaxException {

		File baseFile = new File("//SERVER/some/path/");
		URI baseURI = new URI("file:////SERVER/some/path/");

		String canonicalForm = "javax.servlet,2.4.0.v200806031604,plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo canonicalInfo = SimpleConfiguratorUtils.parseBundleInfoLine(canonicalForm, baseURI);
		File canonicalFile = new File(baseFile, "plugins/javax.servlet_2.4.0.v200806031604.jar");
		final String canonicalFileURLString = canonicalFile.toURL().toExternalForm();

		String line[] = new String[6];
		line[0] = "javax.servlet,2.4.0.v200806031604,file:plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[1] = "javax.servlet,2.4.0.v200806031604,plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[2] = "javax.servlet,2.4.0.v200806031604,file:plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[3] = "javax.servlet,2.4.0.v200806031604,file:" + canonicalFile.toString() + ",4,false";
		line[4] = "javax.servlet,2.4.0.v200806031604," + canonicalFileURLString + ",4,false";
		line[5] = "javax.servlet,2.4.0.v200806031604," + canonicalFile.toURI().toString() + ",4,false";

		String relativeBundleLocation = "reference:file:plugins/javax.servlet_2.4.0.v200806031604.jar";
		String absoluteBundleLocation = "reference:" + canonicalFileURLString;

		for (int i = 0; i < line.length; i++) {
			if (line[i].indexOf('\\') != -1 && !WINDOWS)
				continue;
			BundleInfo info = SimpleConfiguratorUtils.parseBundleInfoLine(line[i], baseURI);
			assertEquals("[" + i + "]", canonicalInfo, info);
			if (info.getLocation().isAbsolute())
				assertEquals("[" + i + "]", absoluteBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
			else
				assertEquals("[" + i + "]", relativeBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
		}
	}

	public void testParseBundleInfo() throws MalformedURLException {

		File baseFile = getTempFolder();
		URI baseURI = baseFile.toURI();

		String canonicalForm = "javax.servlet,2.4.0.v200806031604,plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo canonicalInfo = SimpleConfiguratorUtils.parseBundleInfoLine(canonicalForm, baseURI);
		File canonicalFile = new File(baseFile, "plugins/javax.servlet_2.4.0.v200806031604.jar");

		String line[] = new String[6];
		line[0] = "javax.servlet,2.4.0.v200806031604,file:plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[1] = "javax.servlet,2.4.0.v200806031604,plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[2] = "javax.servlet,2.4.0.v200806031604,file:plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[3] = "javax.servlet,2.4.0.v200806031604,file:" + canonicalFile.toString() + ",4,false";
		line[4] = "javax.servlet,2.4.0.v200806031604," + canonicalFile.toURL().toExternalForm() + ",4,false";
		line[5] = "javax.servlet,2.4.0.v200806031604," + canonicalFile.toURI().toString() + ",4,false";

		String relativeBundleLocation = "reference:file:plugins/javax.servlet_2.4.0.v200806031604.jar";
		String absoluteBundleLocation = "reference:" + canonicalFile.toURL().toExternalForm();

		for (int i = 0; i < line.length; i++) {
			if (line[i].indexOf('\\') != -1 && !WINDOWS)
				continue;
			BundleInfo info = SimpleConfiguratorUtils.parseBundleInfoLine(line[i], baseURI);
			assertEquals("[" + i + "]", canonicalInfo, info);
			if (info.getLocation().isAbsolute())
				assertEquals("[" + i + "]", absoluteBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
			else
				assertEquals("[" + i + "]", relativeBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
		}
	}

	public void testParseBundleInfoWithSpaces() throws MalformedURLException {

		File baseFile = getTempFolder();
		URI baseURI = baseFile.toURI();

		String canonicalForm = "javax.servlet,2.4.0.v200806031604,plugin%20s/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo canonicalInfo = SimpleConfiguratorUtils.parseBundleInfoLine(canonicalForm, baseURI);
		File canonicalFile = new File(baseFile, "plugin s/javax.servlet_2.4.0.v200806031604.jar");

		String line[] = new String[6];
		line[0] = "javax.servlet,2.4.0.v200806031604,file:plugin s/javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[1] = "javax.servlet,2.4.0.v200806031604,plugin s\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[2] = "javax.servlet,2.4.0.v200806031604,file:plugin s\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[3] = "javax.servlet,2.4.0.v200806031604,file:" + canonicalFile.toString() + ",4,false";
		line[4] = "javax.servlet,2.4.0.v200806031604," + canonicalFile.toURL().toExternalForm() + ",4,false";
		line[5] = "javax.servlet,2.4.0.v200806031604," + canonicalFile.toURI().toString() + ",4,false";

		String relativeBundleLocation = "reference:file:plugin s/javax.servlet_2.4.0.v200806031604.jar";
		String absoluteBundleLocation = "reference:" + canonicalFile.toURL().toExternalForm();

		for (int i = 0; i < line.length; i++) {
			if (line[i].indexOf('\\') != -1 && !WINDOWS)
				continue;
			BundleInfo info = SimpleConfiguratorUtils.parseBundleInfoLine(line[i], baseURI);
			assertEquals("[" + i + "]", canonicalInfo, info);
			if (info.getLocation().isAbsolute())
				assertEquals("[" + i + "]", absoluteBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
			else
				assertEquals("[" + i + "]", relativeBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
		}
	}

	public void testParseUNCBundleInfo() throws MalformedURLException {

		if (!WINDOWS)
			return;

		File baseFile = new File("\\\\127.0.0.1\\somefolder\\");
		URI baseURI = baseFile.toURI();

		String canonicalForm = "javax.servlet,2.4.0.v200806031604,plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		BundleInfo canonicalInfo = SimpleConfiguratorUtils.parseBundleInfoLine(canonicalForm, baseURI);
		File canonicalFile = new File(baseFile, "plugins/javax.servlet_2.4.0.v200806031604.jar");

		String line[] = new String[3];
		line[0] = "javax.servlet,2.4.0.v200806031604,file:plugins/javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[1] = "javax.servlet,2.4.0.v200806031604,plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";
		line[2] = "javax.servlet,2.4.0.v200806031604,file:plugins\\javax.servlet_2.4.0.v200806031604.jar,4,false";

		//TODO: we need to fix URI.resolve for UNC paths
		//line[3] = "javax.servlet,2.4.0.v200806031604," + canonicalFile.toURI().toString() + ",4,false";

		String relativeBundleLocation = "reference:file:plugins/javax.servlet_2.4.0.v200806031604.jar";
		String absoluteBundleLocation = "reference:" + canonicalFile.toURL().toExternalForm();

		for (int i = 0; i < line.length; i++) {
			BundleInfo info = SimpleConfiguratorUtils.parseBundleInfoLine(line[i], baseURI);
			assertEquals("[" + i + "]", canonicalInfo, info);
			if (info.getLocation().isAbsolute())
				assertEquals("[" + i + "]", absoluteBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
			else
				assertEquals("[" + i + "]", relativeBundleLocation, SimpleConfiguratorUtils.getBundleLocation(info, true));
		}
	}

	public void testRead34BundlesInfo() {

		File data = getTestData("1.0", "testData/simpleConfiguratorTest/3.4.bundles.info");
		File baseFile = getTempFolder();
		URI baseURI = baseFile.toURI();
		try {
			List infos = SimpleConfiguratorUtils.readConfiguration(data.toURL(), baseURI);
			assertEquals("1.1", 2, infos.size());

			BundleInfo a = new BundleInfo("a", "1.0.0", new URI("plugins/a_1.0.0.jar"), 4, false);
			a.setBaseLocation(baseURI);
			BundleInfo b = new BundleInfo("b", "1.0.0", new URI("plugins/b_1.0.0.jar"), -1, true);
			b.setBaseLocation(baseURI);

			assertEquals("1.2", a, infos.get(0));
			assertEquals("1.3", b, infos.get(1));

		} catch (URISyntaxException e) {
			fail("1.97", e);
		} catch (MalformedURLException e) {
			fail("1.98", e);
		} catch (IOException e) {
			fail("1.99", e);
		}
	}

	public void testReadVersionLine() {
		String versionPrefix = "#version=";

		SimpleConfiguratorUtils.parseCommentLine(versionPrefix + "1");
		try {
			SimpleConfiguratorUtils.parseCommentLine(versionPrefix + "999");
		} catch (IllegalArgumentException e) {
			return;
		}
		fail("improper version error not caught");
	}

	public void testReadMissingBundleInfo() {

		File bundleInfoFile = new File(getTempFolder(), "bundle.info");
		assertFalse(bundleInfoFile.exists());
		try {
			assertEquals(0, SimpleConfiguratorUtils.readConfiguration(bundleInfoFile.toURL(), null).size());
		} catch (IOException e) {
			fail();
		}
	}
}
