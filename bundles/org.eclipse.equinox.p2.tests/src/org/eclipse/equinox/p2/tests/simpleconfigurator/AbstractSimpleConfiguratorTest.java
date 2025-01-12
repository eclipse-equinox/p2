/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - fragment support
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.embeddedequinox.EmbeddedEquinox;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public abstract class AbstractSimpleConfiguratorTest extends AbstractProvisioningTest {
	static String BUNDLE_JAR_DIRECTORY = "simpleConfiguratorTest/bundlesTxt2";
	private EmbeddedEquinox equinox = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (equinox != null)
			equinox.shutdown();
	}

	//Assert that all files are in the bundles
	protected void assertJarsInstalled(File[] jars, Bundle[] bundles) {
		for (File jar : jars) {
			boolean found = false;
			String jarName = getManifestEntry(jar, Constants.BUNDLE_SYMBOLICNAME);
			for (Bundle bundle : bundles) {
				String bundleName = bundle.getSymbolicName();
				if (bundleName.equalsIgnoreCase(jarName))
					found = true;
			}
			if (!found)
				fail("Bundle should be present:  " + jarName);
		}
	}

	private File getLocation(String bundleId) {
		try {
			URL u = FileLocator.resolve(Platform.getBundle(bundleId).getEntry(""));
			String urlString = u.toExternalForm();
			if (urlString.startsWith("file:")) {
				return new File(urlString.substring(5));
			}
			if (urlString.startsWith("jar:")) {
				return new File(urlString.substring(9, urlString.length() - 2));
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	protected BundleContext startFramework(File bundleInfo, File[] additionalBundle) {
		try {
			File simpleConfiguratorBundle = getLocation("org.eclipse.equinox.simpleconfigurator");
			File osgiBundleLoc = getLocation("org.eclipse.osgi");

			// for test purposes create an install.area and configuration.area located in the local bundle data area.
			File installarea = TestActivator.context.getDataFile(getName() + "/" + System.currentTimeMillis() + "/eclipse");
			File configarea = new File(installarea, "configuration");
			URL osgiBundle = osgiBundleLoc.toURI().toURL();
			//if we have framework in workspace need to add the bin directory
			URL osgiBundleDevPath = null;
			if (!osgiBundle.getPath().endsWith(".jar")) {
				osgiBundleDevPath = new URL(osgiBundle, "bin/");
			}

			Map<String, String> frameworkProperties = new HashMap<>();
			// note that any properties you do not want to be inherited from the hosting Equinox will need
			// to be nulled out.  Otherwise you will pick them up from the hosting env.
			frameworkProperties.put("osgi.framework", null);
			frameworkProperties.put("osgi.install.area", installarea.toURL().toExternalForm());
			frameworkProperties.put("osgi.configuration.area", configarea.toURL().toExternalForm());
			StringBuilder osgiBundles = new StringBuilder();
			for (int i = 0; additionalBundle != null && i < additionalBundle.length; i++) {
				osgiBundles.append("reference:").append(additionalBundle[i].toURL().toExternalForm()).append(",");
			}
			osgiBundles.append("reference:").append(simpleConfiguratorBundle.toURL().toExternalForm()).append("@1:start");
			frameworkProperties.put("osgi.bundles", osgiBundles.toString());

			frameworkProperties.put("org.eclipse.equinox.simpleconfigurator.configUrl", bundleInfo.toURL().toExternalForm());
			frameworkProperties.put("osgi.dev", "bin/");

			URL[] osgiPath = osgiBundleDevPath == null ? new URL[] {osgiBundle} : new URL[] {osgiBundle, osgiBundleDevPath};
			equinox = new EmbeddedEquinox(frameworkProperties, new String[] {}, osgiPath);
			return equinox.startFramework();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	//Create a bundles.info with all the jars listed plus OSGi and SimpleConfigurator
	protected File createBundlesTxt(File[] jars) throws IOException {
		File bundlesTxt = File.createTempFile("bundles", ".txt");
		bundlesTxt.deleteOnExit();

		try (BufferedWriter bundlesTxtOut = new BufferedWriter(new FileWriter(bundlesTxt))) {

			for (File bundleJar : jars) {
				bundlesTxtOut.write(getBundlesTxtEntry(bundleJar) + "\n");
			}
			bundlesTxtOut.write(getBundlesTxtEntry(getLocation("org.eclipse.equinox.simpleconfigurator")) + "\n");
			bundlesTxtOut.write(getBundlesTxtEntry(getLocation("org.eclipse.osgi")) + "\n");

		}

		return bundlesTxt;
	}

	private String getBundlesTxtEntry(File bundleJar) {
		String name = getManifestEntry(bundleJar, Constants.BUNDLE_SYMBOLICNAME);
		String version = getManifestEntry(bundleJar, Constants.BUNDLE_VERSION);
		// <name>,<version>,file:<file>,<startlevel>,true
		return name + "," + version + "," + bundleJar.toURI() + "," + getStartLevel(name) + ",true";
	}

	private String getManifestEntry(File bundleFile, String entry) {
		try {
			String value = null;
			if (bundleFile.isDirectory()) {
				File m = new File(bundleFile, "META-INF/MANIFEST.MF");
				try (InputStream os = Files.newInputStream(m.toPath())) {
					Manifest mf = new Manifest(os);
					value = mf.getMainAttributes().getValue(entry);
				}
			} else {
				try (JarFile bundleJar = new JarFile(bundleFile)) {
					value = bundleJar.getManifest().getMainAttributes().getValue(entry);
				}
			}
			if (value.contains(";")) {
				String[] valueElements = value.split(";");
				return valueElements[0];
			}
			return value;
		} catch (IOException e) {
			return null;
		}
	}

	private int getStartLevel(String bundleName) {
		int startLevel = 4;
		// some special case start levels
		if (bundleName.matches("org.eclipse.osgi")) {
			startLevel = -1;
		} else if (bundleName.matches("org.eclipse.equinox.common")) {
			startLevel = 2;
		} else if (bundleName.matches("org.eclipse.equinox.simpleconfigurator")) {
			startLevel = 1;
		}
		return startLevel;
	}

	protected File[] getBundleJars(File directory) {
		FilenameFilter bundleFilter = (directoryName, filename) -> !filename.startsWith(".") && !filename.equals("CVS");
		return directory.listFiles(bundleFilter);
	}
}
