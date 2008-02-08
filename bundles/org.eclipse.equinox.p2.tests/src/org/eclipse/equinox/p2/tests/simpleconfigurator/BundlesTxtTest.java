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
import org.eclipse.equinox.p2.tests.*;
import org.eclipse.equinox.p2.tests.embeddedequinox.EmbeddedEquinox;
import org.osgi.framework.*;

public class BundlesTxtTest extends AbstractProvisioningTest {

	static String BUNDLE_JAR_DIRECTORY = "bundlesTxt";
	int numBundles = -1;
	EmbeddedEquinox equinox = null;
	BundleContext equinoxContext = null;
	File[] bundleJars = null;
	File bundlesTxt = null;

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		equinox.shutdown();
	}

	public void testBundlesTxt() throws Exception {
		File simpleconfiguratorDataDir = TestData.getFile(BUNDLE_JAR_DIRECTORY, "");
		bundleJars = getBundleJars(simpleconfiguratorDataDir);

		numBundles = bundleJars.length;

		// Create a test bundles.txt
		createBundlesTxt();

		// for demonstration purposes this is just using an install.area and configuration.area located
		// in the local bundle data area.  For p2 testing you will want to point this to your own properties
		File installarea = TestActivator.context.getDataFile("eclipse");
		File configarea = new File(installarea, "configuration");

		File simpleConfiguratorBundle = getSimpleConfiguratorBundle();
		URL osgiBundle = getFrameworkBundle();

		Map frameworkProperties = new HashMap();
		// note that any properties you do not want to be inherited from the hosting Equinox will need
		// to be nulled out.  Otherwise you will pick them up from the hosting env.
		frameworkProperties.put("osgi.framework", null);
		frameworkProperties.put("osgi.install.area", installarea.toURL().toExternalForm());
		frameworkProperties.put("osgi.configuration.area", configarea.toURL().toExternalForm());
		frameworkProperties.put("osgi.bundles", "reference:" + simpleConfiguratorBundle.toURL().toExternalForm() + "@1:start"); // should point to simple configurator
		frameworkProperties.put("org.eclipse.equinox.simpleconfigurator.configUrl", bundlesTxt.toURL().toExternalForm());
		try {
			equinox = new EmbeddedEquinox(frameworkProperties, new String[] {}, new URL[] {osgiBundle});
			equinoxContext = equinox.startFramework();

			// verify that loaded bundles should be
			Bundle[] bundlesInContext = equinoxContext.getBundles();
			// are all bundles in list of expected bundles?
			for (int i = 0; i < bundlesInContext.length; i++) {
				Bundle bundle = bundlesInContext[i];
				boolean found = false;
				Bundle notFoundBundle = bundle;
				for (int j = 0; j < bundleJars.length; j++) {
					File expectedBundleFile = bundleJars[j];
					String name = getManifestEntry(expectedBundleFile, Constants.BUNDLE_SYMBOLICNAME);
					String bundleName = bundle.getSymbolicName();
					if (bundleName.equalsIgnoreCase(name))
						found = true;
				}
				if (!found)
					fail("Bundle should not be present:  " + notFoundBundle);
			}

			// TODO:  should we check start levels?

			// verify that all bundles expected to be loaded are in fact loaded
			for (int i = 0; i < bundleJars.length; i++) {
				File bundle = bundleJars[i];
				String name = getManifestEntry(bundle, Constants.BUNDLE_SYMBOLICNAME);
				boolean loaded = false;
				for (int j = 0; j < bundlesInContext.length; j++) {
					Bundle loadedBundle = bundlesInContext[j];
					if (loadedBundle.getSymbolicName().equalsIgnoreCase(name))
						loaded = true;
				}
				if (!loaded)
					fail("Bundle expected to be loaded but is not:  " + name);
			}

			assertEquals("Too many/too few bundles!!", numBundles, bundlesInContext.length);
		} finally {
			equinox.shutdown();
		}
	}

	private File getSimpleConfiguratorBundle() {
		for (int i = 0; i < bundleJars.length; i++) {
			File bundleJar = bundleJars[i];
			if (bundleJar.getName().startsWith("org.eclipse.equinox.simpleconfigurator"))
				return bundleJar;
		}
		return null;
	}

	private void createBundlesTxt() throws IOException {
		bundlesTxt = File.createTempFile("bundles", ".txt");
		bundlesTxt.deleteOnExit();

		BufferedWriter bundlesTxtOut = new BufferedWriter(new FileWriter(bundlesTxt));

		for (int i = 0; i < bundleJars.length; i++) {
			File bundleJar = bundleJars[i];
			bundlesTxtOut.write(getBundlesTxtEntry(bundleJar) + "\n");
		}

		bundlesTxtOut.close();

		//		FileInputStream f = new FileInputStream(bundlesTxt);
		//		BufferedInputStream b = new BufferedInputStream(f);
		//		DataInputStream d = new DataInputStream(b);
		//		while (d.available() != 0) {
		//			System.out.println(d.readLine());
		//		}
	}

	private String getBundlesTxtEntry(File bundleJar) throws IOException {
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
			} else
				return value;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	private URL getFrameworkBundle() {
		for (int i = 0; i < bundleJars.length; i++) {
			File bundleJar = bundleJars[i];
			if (bundleJar.getName().startsWith("org.eclipse.osgi"))
				try {
					return bundleJar.toURL();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
