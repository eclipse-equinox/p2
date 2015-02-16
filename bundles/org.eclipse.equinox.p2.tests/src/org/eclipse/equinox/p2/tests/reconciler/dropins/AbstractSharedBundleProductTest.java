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
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.update.Configuration;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.p2.core.ProvisionException;

public class AbstractSharedBundleProductTest extends AbstractReconcilerTest {

	public static final String PLATFORM_BASE = "platform:/base/";
	protected static String sharedLocationURL = null;

	public AbstractSharedBundleProductTest(String name) {
		super(name);
	}

	public AbstractSharedBundleProductTest(String name, String location) {
		super(name, location);
	}

	private Map<String, BundleInfo> getBootstrapBundles() {
		Map<String, BundleInfo> result = new HashMap();

		// TODO deal with fragments
		//	list.add("org.eclipse.core.net.win32.x86");
		//	list.add("org.eclipse.equinox.security.win32.x86");
		final String[] bootstrap = new String[] { //
		"org.eclipse.core.contenttype", //
				"org.eclipse.core.expressions", //
				"org.eclipse.core.jobs", //
				"org.eclipse.core.net", //
				"org.eclipse.core.runtime", //
				"org.eclipse.core.runtime.compatibility.registry", //
				"org.eclipse.ecf", //
				"org.eclipse.ecf.filetransfer", //
				"org.eclipse.ecf.identity", //
				"org.eclipse.ecf.provider.filetransfer", //
				"org.eclipse.ecf.provider.filetransfer.ssl", //
				"org.eclipse.ecf.ssl", //
				"org.eclipse.equinox.app", //
				"org.eclipse.equinox.common", //
				"org.eclipse.equinox.ds", //
				"org.eclipse.equinox.frameworkadmin", //
				"org.eclipse.equinox.frameworkadmin.equinox", //
				"org.eclipse.equinox.p2.artifact.repository", //
				"org.eclipse.equinox.p2.core", //
				"org.eclipse.equinox.p2.director", //
				"org.eclipse.equinox.p2.directorywatcher", //
				"org.eclipse.equinox.p2.engine", //
				"org.eclipse.equinox.p2.extensionlocation", //
				"org.eclipse.equinox.p2.garbagecollector", //
				"org.eclipse.equinox.p2.jarprocessor", //
				"org.eclipse.equinox.p2.metadata", //
				"org.eclipse.equinox.p2.metadata.repository", //
				"org.eclipse.equinox.p2.publisher", //
				"org.eclipse.equinox.p2.ql", //
				"org.eclipse.equinox.p2.reconciler.dropins", //
				"org.eclipse.equinox.p2.repository", //
				"org.eclipse.equinox.p2.touchpoint.eclipse", //
				"org.eclipse.equinox.p2.touchpoint.natives", //
				"org.eclipse.equinox.preferences", //
				"org.eclipse.equinox.registry", //
				"org.eclipse.equinox.security", //
				"org.eclipse.equinox.simpleconfigurator", //
				"org.eclipse.equinox.simpleconfigurator.manipulator", //
				"org.eclipse.equinox.util", //
				"org.eclipse.osgi", //
				"org.eclipse.osgi.services", //
				"org.sat4j.core", //
				"org.sat4j.pb"};

		// load the bundles.info and put the results into a map for easier lookup
		BundleInfo[] infos = null;
		try {
			infos = loadBundlesInfo(getBundlesInfo());
		} catch (IOException e) {
			fail("Exception occurred loading bundles.info file from: " + getBundlesInfo().getAbsolutePath(), e);
		}
		Map<String, BundleInfo> map = new HashMap();
		for (int i = 0; infos != null && i < infos.length; i++) {
			map.put(infos[i].getSymbolicName(), infos[i]);
			// always add the launcher bundles. do it here because we don't know what os/ws config we will have
			if (infos[i].getSymbolicName().contains("equinox.launcher"))
				result.put(infos[i].getSymbolicName(), infos[i]);
		}

		// just add the bootstrap bundles into the result before returning
		for (int i = 0; i < bootstrap.length; i++) {
			BundleInfo info = map.get(bootstrap[i]);
			if (info != null)
				result.put(info.getSymbolicName(), info);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.reconciler.dropins.AbstractReconcilerTest#initialize()
	 */
	public void initialize() throws Exception {

		// extract the platform archive to the output folder
		super.initialize();

		// setup the shared bundle location
		File shared = new File(output, "shared");
		shared.mkdirs();
		assertTrue("0.0", shared.isDirectory());

		// move all features to the shared location
		File features = new File(output, "eclipse/features");
		File sharedFeatureLocation = new File(shared, "features");
		move("1.0", features, sharedFeatureLocation);

		// move all bundles (except launchers) to the shared location
		File bundles = new File(output, "eclipse/plugins");
		File sharedBundleLocation = new File(shared, "plugins");
		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				return !pathname.getName().contains("equinox.launcher");
			}
		};
		move("2.0", bundles, sharedBundleLocation, filter);

		// update the bundles.info file to contain only the boostrap bundles
		// and also update their locations.
		Map<String, BundleInfo> infos = getBootstrapBundles();
		updateBundlesInfo(infos, sharedBundleLocation);

		// update the platform.xml file to a user-include policy pointing
		// to the bundles in the shared location.
		updateConfiguration(infos, shared);

		// update the config.ini file to make sure the framework JAR
		// is pointing to the right location
		updateConfigIni(infos);

		// reconcile to ensure everything is ok
		reconcile("5.0");
	}

	private void updateConfigIni(Map<String, BundleInfo> infos) {
		File location = new File(output, "eclipse/configuration/config.ini");
		Properties ini = new Properties();
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(location));
			ini.load(input);
		} catch (IOException e) {
			fail("Exception while loading config.ini from: " + location.getAbsolutePath(), e);
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
		}
		BundleInfo framework = infos.get("org.eclipse.osgi");
		assertNotNull("Unable to find framework in list of bootstrap bundles.", framework);
		ini.put("osgi.framework", framework.getLocation().toString());
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(location));
			ini.store(out, null);
		} catch (IOException e) {
			fail("Exception while saving config.ini to: " + location.getAbsolutePath(), e);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	private void updateConfiguration(Map<String, BundleInfo> infos, File shared) {
		Configuration config = new Configuration();
		config.setTransient(false);
		config.setVersion("3.0");

		// add site containing all the bootstrap bundles
		Site site = new Site();
		try {
			sharedLocationURL = shared.toURI().toURL().toExternalForm();
			site.setUrl(sharedLocationURL);
		} catch (MalformedURLException e) {
			fail("Exception occurred while converting site location to URL: " + shared.getAbsolutePath(), e);
		}
		site.setPolicy(Site.POLICY_USER_INCLUDE);
		site.setUpdateable(false);
		site.setEnabled(true);
		URI sharedURI = shared.toURI();
		for (BundleInfo info : infos.values()) {
			URI relative = URIUtil.makeRelative(info.getLocation(), sharedURI);
			site.addPlugin(relative.toString());
		}
		config.add(site);

		// add a site for platform:/base/
		site = new Site();
		site.setUrl(PLATFORM_BASE);
		site.setPolicy(Site.POLICY_USER_EXCLUDE);
		site.setUpdateable(true);
		site.setEnabled(true);
		config.add(site);

		// save the new config
		saveConfiguration(config);
	}

	public void removeBundlesFromConfiguration(Configuration config, String[] locations) {
		for (String location : locations)
			removeBundleFromConfiguration(config, location);
	}

	protected Site getSharedSite(Configuration config) {
		for (Site site : config.getSites()) {
			if (sharedLocationURL != null && sharedLocationURL.equals(site.getUrl()))
				return site;
		}
		return null;
	}

	public boolean removeBundleFromConfiguration(Configuration config, String location) {
		Site shared = getSharedSite(config);
		assertNotNull("Unable to determine shared site from configuration.", shared);
		boolean removed = shared.removePlugin(location);
		if (removed)
			return true;
		// try again with a relative path
		URI relative = null;
		try {
			relative = URIUtil.makeRelative(new URI(location), new URI(sharedLocationURL));
		} catch (URISyntaxException e) {
			fail("Exception while converting location to URI.", e);
		}
		return shared.removePlugin(relative.toString());
	}

	public void addBundlesToConfigurations(Configuration config, String[] locations) {
		for (String location : locations)
			addBundleToConfiguration(config, location);
	}

	public void addBundleToConfiguration(Configuration config, String location) {
		Site shared = getSharedSite(config);
		assertNotNull("Unable to determine shared site from configuration.", shared);
		URI relative = null;
		try {
			relative = URIUtil.makeRelative(new URI(location), new URI(sharedLocationURL));
		} catch (URISyntaxException e) {
			fail("Exception while converting location to URI.", e);
		}
		shared.addPlugin(relative.toString());
	}

	protected File getPlatformXMLLocation() {
		return new File(output, "eclipse/configuration/org.eclipse.update/platform.xml");
	}

	public void saveConfiguration(Configuration config) {
		File configLocation = getPlatformXMLLocation();
		try {
			config.save(configLocation, null);
		} catch (ProvisionException e) {
			fail("Exception occurred while saving configuration: " + configLocation.getAbsolutePath(), e);
		}
	}

	public Configuration loadConfiguration() {
		File configLocation = getPlatformXMLLocation();
		try {
			return Configuration.load(configLocation, null);
		} catch (ProvisionException e) {
			fail("Exception loading configuration.", e);
		}
		// avoid compile error
		return null;
	}

	private void updateBundlesInfo(Map<String, BundleInfo> infos, File sharedBundleLocation) {
		for (BundleInfo info : infos.values()) {
			if (info.getSymbolicName().contains("equinox.launcher"))
				continue;
			File location = new File(sharedBundleLocation, new Path(info.getLocation().toString()).lastSegment());
			assertTrue("3.1." + location.getAbsolutePath(), location.exists());
			info.setLocation(location.toURI());
		}
		// re-write the bundles.info file with the new information
		try {
			saveBundlesInfo(infos.values().toArray(new BundleInfo[infos.size()]), getBundlesInfo());
		} catch (IOException e) {
			fail("Exception occurred while saving bundles.info to: " + getBundlesInfo().getAbsolutePath(), e);
		}
	}

}
