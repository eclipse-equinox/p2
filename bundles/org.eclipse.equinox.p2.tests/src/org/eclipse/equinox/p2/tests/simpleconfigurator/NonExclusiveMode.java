/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - tests
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.simpleconfigurator;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorConstants;
import org.eclipse.equinox.p2.tests.*;
import org.eclipse.equinox.p2.tests.embeddedequinox.EmbeddedEquinox;
import org.osgi.framework.*;

public class NonExclusiveMode extends AbstractProvisioningTest {

	static String BUNDLE_JAR_DIRECTORY = "simpleConfiguratorTest/bundlesTxt";
	EmbeddedEquinox equinox = null;
	BundleContext equinoxContext = null;

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		equinox.shutdown();
	}

	//Assert that all files are in the bundles
	private void assertJarsInstalled(File[] jars, Bundle[] bundles) {
		for (int i = 0; i < jars.length; i++) {
			boolean found = false;
			String jarName = getManifestEntry(jars[i], Constants.BUNDLE_SYMBOLICNAME);
			for (int j = 0; j < bundles.length; j++) {
				String bundleName = bundles[j].getSymbolicName();
				if (bundleName.equalsIgnoreCase(jarName))
					found = true;
			}
			if (!found)
				fail("Bundle should not be present:  " + jarName);
		}
	}

	public void testBundlesTxt() throws Exception {
		File[] jars = getBundleJars(TestData.getFile(BUNDLE_JAR_DIRECTORY, ""));

		// Create a bundles.info containing all the jars passed
		File bundlesTxt = createBundlesTxt(jars);

		// for test purposes create an install.area and configuration.area located in the local bundle data area.
		File installarea = TestActivator.context.getDataFile(getName() + "/eclipse");
		File configarea = new File(installarea, "configuration");

		File simpleConfiguratorBundle = getSimpleConfiguratorBundle(jars);
		URL osgiBundle = getFrameworkBundle(jars);

		File otherBundle = getTestData("myBundle", "testData/simpleConfiguratorTest/myBundle_1.0.0.jar");

		Map frameworkProperties = new HashMap();
		// note that any properties you do not want to be inherited from the hosting Equinox will need
		// to be nulled out.  Otherwise you will pick them up from the hosting env.
		frameworkProperties.put("osgi.framework", null);
		frameworkProperties.put("osgi.install.area", installarea.toURL().toExternalForm());
		frameworkProperties.put("osgi.configuration.area", configarea.toURL().toExternalForm());
		frameworkProperties.put("osgi.bundles", "reference:" + otherBundle.toURL().toExternalForm() + ",reference:" + simpleConfiguratorBundle.toURL().toExternalForm() + "@1:start"); // should point to simple configurator
		frameworkProperties.put("org.eclipse.equinox.simpleconfigurator.configUrl", bundlesTxt.toURL().toExternalForm());
		frameworkProperties.put(SimpleConfiguratorConstants.PROP_KEY_EXCLUSIVE_INSTALLATION, "false");

		try {
			equinox = new EmbeddedEquinox(frameworkProperties, new String[] {}, new URL[] {osgiBundle});
			equinoxContext = equinox.startFramework();

			assertJarsInstalled(jars, equinoxContext.getBundles());
			assertJarsInstalled(new File[] {otherBundle}, equinoxContext.getBundles());
			assertEquals(4, equinoxContext.getBundles().length);
		} finally {
			equinox.shutdown();
		}
	}

	private File getSimpleConfiguratorBundle(File[] jars) {
		for (int i = 0; i < jars.length; i++) {
			File bundleJar = jars[i];
			if (bundleJar.getName().startsWith("org.eclipse.equinox.simpleconfigurator"))
				return bundleJar;
		}
		return null;
	}

	private File createBundlesTxt(File[] jars) throws IOException {
		File bundlesTxt = File.createTempFile("bundles", ".txt");
		bundlesTxt.deleteOnExit();

		BufferedWriter bundlesTxtOut = new BufferedWriter(new FileWriter(bundlesTxt));

		for (int i = 0; i < jars.length; i++) {
			File bundleJar = jars[i];
			bundlesTxtOut.write(getBundlesTxtEntry(bundleJar) + "\n");
		}

		bundlesTxtOut.close();

		return bundlesTxt;
	}

	private String getBundlesTxtEntry(File bundleJar) {
		String name = getManifestEntry(bundleJar, Constants.BUNDLE_SYMBOLICNAME);
		String version = getManifestEntry(bundleJar, Constants.BUNDLE_VERSION);
		// <name>,<version>,file:<file>,<startlevel>,true
		return name + "," + version + "," + "file:" + bundleJar.getAbsolutePath() + "," + getStartLevel(name) + ",true";
	}

	private String getManifestEntry(File bundleFile, String entry) {
		try {
			JarFile bundleJar = new JarFile(bundleFile);
			String value = bundleJar.getManifest().getMainAttributes().getValue(entry);
			if (value.indexOf(";") > -1) {
				String[] valueElements = value.split(";");
				return valueElements[0];
			}
			return value;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	private URL getFrameworkBundle(File[] bundleJars) {
		for (int i = 0; i < bundleJars.length; i++) {
			File bundleJar = bundleJars[i];
			if (bundleJar.getName().startsWith("org.eclipse.osgi"))
				try {
					return bundleJar.toURL();
				} catch (MalformedURLException e) {
					fail("Can't find the osgi bundle");
				}
		}
		return null;
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

	private File[] getBundleJars(File directory) {
		FilenameFilter bundleFilter = new FilenameFilter() {
			public boolean accept(File directoryName, String filename) {
				return !filename.startsWith(".") && !filename.equals("CVS");
			}
		};
		return directory.listFiles(bundleFilter);
	}

}
