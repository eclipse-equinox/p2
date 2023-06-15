/*******************************************************************************
 *  Copyright (c) 2008, 2020 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB - Ongoing development
 *     Red Hat, Inc. - Fragment support added. Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.p2.engine.SurrogateProfileHandler;
import org.eclipse.equinox.internal.p2.jarprocessor.StreamProcessor;
import org.eclipse.equinox.internal.p2.update.Configuration;
import org.eclipse.equinox.internal.p2.update.Feature;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.internal.p2.updatesite.Activator;
import org.eclipse.equinox.internal.simpleconfigurator.manipulator.SimpleConfiguratorManipulatorImpl;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;

public class AbstractReconcilerTest extends AbstractProvisioningTest {
	public static final String VERIFIER_BUNDLE_ID = "org.eclipse.equinox.p2.tests.verifier";
	protected static File output;
	protected static Set<File> toRemove = new HashSet<>();
	private static boolean initialized = false;
	private static Properties archiveAndRepositoryProperties = null;

	private String propertyToPlatformArchive;
	protected boolean debug = false;

	static {
		loadPlatformZipPropertiesFromFile();
	}

	/*
			 * Constructor for the class.
			 */
	public AbstractReconcilerTest(String name) {
		super(name);
	}

	public AbstractReconcilerTest(String name, String propertyToPlatformArchive) {
		super(name);
		this.propertyToPlatformArchive = propertyToPlatformArchive;
	}

	/*
	 * Set up the platform binary download and get it ready to run the tests.
	 * This method is not intended to be called by clients, it will be called
	 * automatically when the clients use a ReconcilerTestSuite. If the executable isn't
	 * found on the file-system after the call, then we fail.
	 */
	public void initialize() throws Exception {
		initialized = false;
		File file = getPlatformZip();
		output = getUniqueFolder();
		toRemove.add(output);
		// for now we will exec to un-tar archives to keep the executable bits
		if (file.getName().toLowerCase().endsWith(".zip")) {
			try {
				FileUtils.unzipFile(file, output);
			} catch (IOException e) {
				fail("0.99", e);
			}
		} else if (file.getName().toLowerCase().endsWith(".dmg")) {
			extractDmg("1.1", file);
		} else {
			untar("1.0", file);
		}
		File exe = new File(output, getExeFolder() + "eclipse.exe");
		if (!exe.exists()) {
			exe = new File(output, getExeFolder() + "eclipse");
			if (!exe.exists())
				fail("Executable file: " + exe.getAbsolutePath() + "(or .exe) not found after extracting: " + file.getAbsolutePath() + " to: " + output);
			if (!exe.canExecute()) {
				// Try first to set --x--x--x, then --x------ if we can't do the former
				if (!exe.setExecutable(true, false) || !exe.setExecutable(true, true)) {
					fail("Executable file: " + exe.getAbsolutePath() + " is not executable");
				}
			}
		}
		initialized = true;
	}

	public void assertInitialized() {
		assertTrue("Test suite not initialized, check log for previous errors.", initialized);
	}

	/*
	 * Run the given command.
	 */
	protected static int run(String message, String[] commandArray) {
		try {
			Process process = Runtime.getRuntime().exec(commandArray, null, output);
			StreamProcessor.start(process.getErrorStream(), StreamProcessor.STDERR, true);
			StreamProcessor.start(process.getInputStream(), StreamProcessor.STDOUT, true);
			process.waitFor();
			return process.exitValue();
		} catch (IOException e) {
			fail(message, e);
		} catch (InterruptedException e) {
			fail(message, e);
		}
		return -1;
	}

	protected static int run(String message, String[] commandArray, File outputFile) {
		PrintStream out = System.out;
		PrintStream err = System.err;
		try {
			outputFile.getParentFile().mkdirs();
			try (PrintStream fileStream = new PrintStream(new FileOutputStream(outputFile))) {
				System.setErr(fileStream);
				System.setOut(fileStream);
				return run(message, commandArray);
			}
		} catch (FileNotFoundException e) {
			return -1;
		} finally {
			System.setOut(out);
			System.setErr(err);
		}
	}

	/*
	 * Untar the given file in the output directory.
	 */
	private void untar(String message, File file) {
		String name = file.getName();
		File gzFile = new File(output, name);
		output.mkdirs();
		run(message, new String[] {"cp", file.getAbsolutePath(), gzFile.getAbsolutePath()});
		run(message, new String[] {"tar", "-zpxf", gzFile.getAbsolutePath()});
		gzFile.delete();
	}

	/*
	 * extract dmg given file in the output directory.
	 */
	private void extractDmg(String message, File file) {
		output.mkdirs();
		run(message, new String[] { "hdiutil", "attach", file.getAbsolutePath() });
		run(message, new String[] { "cp", "-r", "/Volumes/Eclipse/Eclipse.app", output.getAbsolutePath() + "/" });
		run(message, new String[] { "hdiutil", "detach", "/Volumes/Eclipse" });
		run(message, new String[] { "xattr", "-rc", output.getAbsolutePath() + "/Eclipse.app" });
	}

	/*
	 * Return a file object with a unique name in a temporary location.
	 */
	public static File getUniqueFolder() {
		String tempDir = System.getProperty("java.io.tmpdir");
		return new File(tempDir, getUniqueString());
	}

	/*
	 * Helper method to return the install location. Return null if it is unavailable.
	 */
	public static File getInstallLocation() {
		Location installLocation = ServiceHelper.getService(TestActivator.getContext(), Location.class, Location.INSTALL_FILTER);
		if (installLocation == null || !installLocation.isSet())
			return null;
		URL url = installLocation.getURL();
		if (url == null)
			return null;
		return URLUtil.toFile(url);
	}

	private String getValueFor(String property) {
		if (property == null)
			return null;
		String result = TestActivator.getContext().getProperty(property);
		if (result == null && archiveAndRepositoryProperties == null)
			return null;
		if (result == null)
			archiveAndRepositoryProperties.getProperty(property);
		if (result == null)
			result = archiveAndRepositoryProperties.getProperty(property + '.' + Platform.getOS());
		return result;
	}

	/*
	 * Return a file handle pointing to the platform binary zip. Method never returns null because
	 * it will fail an assert before that.
	 */
	private File getPlatformZip() {
		String property = null;
		File file = null;
		if (propertyToPlatformArchive != null) {
			property = getValueFor(propertyToPlatformArchive);
			String message = "Need to set the " + "\"" + propertyToPlatformArchive + "\" system property with a valid path to the platform binary drop or copy the archive to be a sibling of the install folder.";
			if (property == null) {
				fail(message);
			}
			file = new File(property);
			assertNotNull(message, file);
			assertTrue(message, file.exists());
			assertTrue("File is zero length: " + file.getAbsolutePath(), file.length() > 0);
			return file;
		}

		property = getValueFor("org.eclipse.equinox.p2.reconciler.tests.platform.archive");
		if (property == null) {
			// the releng test framework copies the zip so let's look for it...
			// it will be a sibling of the eclipse/ folder that we are running
			File installLocation = getInstallLocation();
			if (Platform.getWS().equals(Platform.WS_COCOA))
				installLocation = installLocation.getParentFile().getParentFile();

			if (installLocation != null) {
				// parent will be "eclipse" and the parent's parent will be "eclipse-testing"
				File parent = installLocation.getParentFile();
				if (parent != null) {
					parent = parent.getParentFile();
					if (parent != null) {
						File[] children = parent.listFiles((FileFilter) pathname -> {
							String name = pathname.getName();
							return name.startsWith("eclipse-platform-");
						});
						if (children != null && children.length == 1)
							file = children[0];
					}
				}
			}
		} else {
			file = new File(property);
			try {
				file = file.getCanonicalFile();
			} catch (IOException e) {
				// then use the non canonical one...
			}
		}
		StringBuffer detailedMessage = new StringBuffer(600);
		detailedMessage.append(" propertyToPlatformArchive was ").append(propertyToPlatformArchive == null ? " not set " : propertyToPlatformArchive).append('\n');
		detailedMessage.append(" org.eclipse.equinox.p2.reconciler.tests.platform.archive was ").append(property == null ? " not set " : property).append('\n');
		detailedMessage.append(" install location is ").append(getInstallLocation()).append('\n');
		String message = "Need to set the \"org.eclipse.equinox.p2.reconciler.tests.platform.archive\" system property with a valid path to the platform binary drop or copy the archive to be a sibling of the install folder.";
		assertNotNull(message + "\n" + detailedMessage, file);
		assertTrue(message + "\nThe file '" + file.getAbsolutePath() + "' does not exist", file.exists());
		assertTrue("File is zero length: " + file.getAbsolutePath(), file.length() > 0);
		return file;
	}

	/*
	 * Add the given bundle to the given folder (do a copy).
	 * The folder can be one of dropins, plugins or features.
	 * If the file handle points to a directory, then do a deep copy.
	 */
	public void add(String message, String target, File file) {
		if (!(target.startsWith("dropins") || target.startsWith("plugins") || target.startsWith("features")))
			fail("Destination folder for resource copying should be either dropins, plugins or features.");
		File destinationParent = new File(output, getRootFolder() + target);
		destinationParent.mkdirs();
		copy(message, file, new File(destinationParent, file.getName()));
	}

	/*
	 * Create a link file in the links folder. Point it to the given extension location.
	 */
	public void createLinkFile(String message, String filename, String extensionLocation) {
		File file = new File(output, getRootFolder() + "links/" + filename + ".link");
		file.getParentFile().mkdirs();
		Properties properties = new Properties();
		properties.put("path", extensionLocation);
		try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file));) {
			properties.store(stream, null);
		} catch (IOException e) {
			fail(message, e);
		}
	}

	/*
	 * Delete the link file with the given name from the links folder.
	 */
	public void removeLinkFile(String message, String filename) {
		File file = new File(output, getRootFolder() + "links/" + filename + ".link");
		file.delete();
	}

	public void add(String message, String target, File[] files) {
		assertNotNull(files);
		for (File file : files) {
			add(message, target, file);
		}
	}

	/*
	 * Remove the given filename from the given folder.
	 */
	public boolean remove(String message, String target, String filename) {
		if (!(target.startsWith("dropins") || target.startsWith("plugins") || target.startsWith("features")))
			fail("Target folder for resource deletion should be either dropins, plugins or features.");
		File folder = new File(output, getRootFolder() + target);
		File targetFile = new File(folder, filename);
		if (!targetFile.exists())
			return false;
		return delete(targetFile);
	}

	/*
	 * Remove the files with the given names from the target folder.
	 */
	public void remove(String message, String target, String[] names) {
		assertNotNull(names);
		for (String name : names) {
			remove(message, target, name);
		}
	}

	/*
	 * Return a boolean value indicating whether or not a bundle with the given id
	 * is listed in the bundles.info file. Ignore the version number and return true
	 * if there are any matches in the file.
	 */
	public boolean isInBundlesInfo(String bundleId) throws IOException {
		return isInBundlesInfo(bundleId, null);
	}

	protected File getBundlesInfo() {
		return new File(output, getRootFolder() + "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
	}

	public boolean isInBundlesInfo(String bundleId, String version) throws IOException {
		return isInBundlesInfo(bundleId, version, null);
	}

	public boolean isInBundlesInfo(String bundleId, String version, String location) throws IOException {
		return isInBundlesInfo(getBundlesInfo(), bundleId, version, location);
	}

	public boolean isInBundlesInfo(File bundlesInfo, String bundleId, String version) throws IOException {
		return isInBundlesInfo(bundlesInfo, bundleId, version, null);
	}

	public BundleInfo[] loadBundlesInfo(File location) throws IOException {
		SimpleConfiguratorManipulator manipulator = new SimpleConfiguratorManipulatorImpl();
		try (InputStream input = new BufferedInputStream(new FileInputStream(location));) {
			return manipulator.loadConfiguration(input, new File(output, "eclipse").toURI());
		}
	}

	public void saveBundlesInfo(BundleInfo[] bundles, File location) throws IOException {
		SimpleConfiguratorManipulator manipulator = new SimpleConfiguratorManipulatorImpl();
		manipulator.saveConfiguration(bundles, location, null);
	}

	/*
	 * Return a boolean value indicating whether or not a bundle with the given id
	 * is listed in the bundles.info file. If the version is non-null, check to ensure the
	 * version is the expected one. If the location is non-null then do a String#contains check.
	 */
	public boolean isInBundlesInfo(File bundlesInfo, String bundleId, String version, String location) throws IOException {
		BundleInfo[] infos = loadBundlesInfo(bundlesInfo);
		for (int i = 0; infos != null && i < infos.length; i++) {
			BundleInfo info = infos[i];
			if (!bundleId.equals(info.getSymbolicName()))
				continue;
			if (version != null && !version.equals(info.getVersion()))
				continue;
			if (location == null)
				return true;
			return info.getLocation().toString().contains(location);
		}
		return false;
	}

	public void reconcile(String message) {
		reconcile(message, false);
	}

	/*
	 * Run the reconciler to discover changes in the drop-ins folder and update the system state.
	 */
	public void reconcile(String message, boolean clean) {
		List<String> args = new ArrayList<>();
		args.add("-application");
		args.add("org.eclipse.equinox.p2.reconciler.application");
		if (clean)
			args.add("-clean");
		runEclipse(message, args.toArray(new String[args.size()]));
	}

	/*
	 * If a bundle with the given id and version exists in the bundles.info file then
	 * throw an AssertionFailedException.
	 */
	public void assertDoesNotExistInBundlesInfo(String message, String bundleId, String version) {
		try {
			assertTrue(message, !isInBundlesInfo(bundleId, version));
		} catch (IOException e) {
			fail(message, e);
		}
	}

	/*
	 * If a bundle with the given id in the bundles.info file then throw an AssertionFailedException.
	 */
	public void assertDoesNotExistInBundlesInfo(String message, String bundleId) {
		assertDoesNotExistInBundlesInfo(message, bundleId, null);
	}

	/*
	 * If a bundle with the given id and version does not exist in the bundles.info file then
	 * throw an AssertionFailedException.
	 */
	public void assertExistsInBundlesInfo(String message, String bundleId, String version) {
		assertExistsInBundlesInfo(message, bundleId, version, null);
	}

	public void assertExistsInBundlesInfo(String message, String bundleId, String version, String location) {
		try {
			assertTrue(message, isInBundlesInfo(bundleId, version, location));
		} catch (IOException e) {
			fail(message, e);
		}
	}

	/*
	 * If a bundle with the given id does not exist in the bundles.info file then throw an AssertionFailedException.
	 */
	public void assertExistsInBundlesInfo(String message, String bundleId) {
		assertExistsInBundlesInfo(message, bundleId, null);
	}

	/*
	 * Clean up the temporary data used to run the tests.
	 * This method is not intended to be called by clients, it will be called
	 * automatically when the clients use a ReconcilerTestSuite.
	 */
	public void cleanup() throws Exception {
		// rm -rf eclipse sub-dir
		boolean leaveDirty = Boolean.parseBoolean(TestActivator.getContext().getProperty("p2.tests.doNotClean"));
		if (leaveDirty)
			return;
		for (File next : toRemove) {
			delete(next);
		}
		output = null;
		toRemove.clear();
	}

	/*
	 * Read and return the configuration object. Will not return null.
	 */
	public Configuration getConfiguration() {
		File configLocation = new File(output, getRootFolder() + "configuration/org.eclipse.update/platform.xml");
		File installLocation = new File(output, getRootFolder());
		return loadConfiguration(configLocation, installLocation);
	}

	public Configuration loadConfiguration(File configLocation, File installLocation) {
		try {
			return Configuration.load(configLocation, installLocation.toURI().toURL());
		} catch (ProvisionException e) {
			fail("Error while reading configuration from " + configLocation);
		} catch (MalformedURLException e) {
			fail("Unable to convert install location to URL " + installLocation);
		}
		assertTrue("Unable to read configuration from " + configLocation, false);
		// avoid compiler error
		return null;
	}

	/*
	 * Save the given configuration to disk.
	 */
	public void save(String message, Configuration configuration) {
		File configLocation = new File(output, getRootFolder() + "configuration/org.eclipse.update/platform.xml");
		File installLocation = new File(output, getRootFolder());
		try {
			configuration.save(configLocation, installLocation.toURI().toURL());
		} catch (ProvisionException e) {
			fail(message, e);
		} catch (MalformedURLException e) {
			fail(message, e);
		}
	}

	/*
	 * Iterate over the sites in the given configuration and remove the one which
	 * has a url matching the given location.
	 */
	public boolean removeSite(Configuration configuration, String location) throws IOException, URISyntaxException {
		File left = new File(new URI(location)).getCanonicalFile();
		for (Site tempSite : configuration.getSites()) {
			String siteURL = tempSite.getUrl();
			File right = new File(new URI(siteURL)).getCanonicalFile();
			if (left.equals(right)) {
				return configuration.removeSite(tempSite);
			}
		}
		return false;
	}

	/*
	 * Create and return a new feature object with the given parameters.
	 */
	public Feature createFeature(Site site, String id, String version, String url) {
		Feature result = new Feature(site);
		result.setId(id);
		result.setVersion(version);
		result.setUrl(url);
		return result;
	}

	/*
	 * Create and return a new site object with the given parameters.
	 */
	public Site createSite(String policy, boolean enabled, boolean updateable, String uri, String[] plugins) {
		Site result = new Site();
		result.setPolicy(policy);
		result.setEnabled(enabled);
		result.setUpdateable(updateable);
		result.setUrl(uri);
		if (plugins != null)
			for (String plugin : plugins) {
				result.addPlugin(plugin);
			}
		return result;
	}

	/*
	 * Copy the bundle with the given id to the specified location. (location
	 * is parent directory)
	 */
	public void copyBundle(String bundlename, File source, File destination) throws IOException {
		if (destination == null)
			destination = output;
		destination = new File(destination, "eclipse/plugins");
		if (source == null) {
			Bundle bundle = TestActivator.getBundle(bundlename);
			if (bundle == null) {
				throw new IOException("Could not find: " + bundlename);
			}
			String location = bundle.getLocation();
			if (location.startsWith("reference:"))
				location = location.substring("reference:".length());
			source = new File(FileLocator.toFileURL(new URL(location)).getFile());
		}
		destination = new File(destination, source.getName());
		if (destination.exists())
			return;
		FileUtils.copy(source, destination, new File(""), false);
		// if the target of the copy doesn't exist, then signal an error
		assertTrue("Unable to copy " + source + " to " + destination, destination.exists());
	}

	/*
	 * Assert that a feature with the given id exists in the configuration. If
	 * a version is specified then match the version, otherwise any version will
	 * do.
	 */
	public void assertFeatureExists(String message, Configuration configuration, String id, String version) {
		List<Site> sites = configuration.getSites();
		assertNotNull(message, sites);
		boolean found = false;
		for (Site site : sites) {
			Feature[] features = site.getFeatures();
			for (int i = 0; features != null && i < features.length; i++) {
				if (id.equals(features[i].getId())) {
					if (version == null)
						found = true;
					else if (version.equals(features[i].getVersion()))
						found = true;
				}
			}
		}
		assertTrue(message, found);
	}

	/*
	 * Return a boolean value indicating whether or not the IU with the given ID and version
	 * is installed. We do this by loading the profile registry and seeing if it is there.
	 */
	public boolean isInstalled(String id, String version) {
		File location = new File(output, getRootFolder() + "/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), location, new SurrogateProfileHandler(getAgent()), false);
		IProfile[] profiles = registry.getProfiles();
		assertEquals("1.0 Should only be one profile in registry.", 1, profiles.length);
		IQueryResult<IInstallableUnit> queryResult = profiles[0].query(QueryUtil.createIUQuery(id, Version.create(version)), null);
		return !queryResult.isEmpty();
	}

	public IInstallableUnit getRemoteIU(String id, String version) {
		File location = new File(output, getRootFolder() + "/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), location, new SurrogateProfileHandler(getAgent()), false);
		IProfile[] profiles = registry.getProfiles();
		assertEquals("1.0 Should only be one profile in registry.", 1, profiles.length);
		IQueryResult<IInstallableUnit> queryResult = profiles[0].query(QueryUtil.createIUQuery(id, Version.create(version)), null);
		assertEquals("1.1 Should not have more than one IU wth the same ID and version.", 1, queryResultSize(queryResult));
		return queryResult.iterator().next();
	}

	protected int runEclipse(String message, String[] args) {
		return runEclipse(message, null, args);
	}

	/*
	 * Note: code modified so we no longer return int 13 if at all possible. Instead we try and
	 * read the error log and fail with the log contents as the fail message. Users of this method should
	 * not expect 13 to be returned.
	 */
	protected int runEclipse(String message, File location, String[] args) {
		return runEclipse(message, location, args, null);
	}

	protected int runEclipse(String message, File location, String[] args, File extensions) {
		File root = new File(Activator.getBundleContext().getProperty("java.home"));
		root = new File(root, "bin");
		File exe = new File(root, "javaw.exe");
		if (!exe.exists())
			exe = new File(root, "java");
		assertTrue("Java executable not found in: " + exe.getAbsolutePath(), exe.exists());
		List<String> command = new ArrayList<>();
		Collections.addAll(command, (new File(location == null ? output : location, getExeFolder() + "eclipse")).getAbsolutePath(), "--launcher.suppressErrors", "-nosplash", "-vm", exe.getAbsolutePath());
		Collections.addAll(command, args);
		Collections.addAll(command, "-vmArgs", "-Dosgi.checkConfiguration=true", "-Dosgi.dataAreaRequiresExplicitInit=false");
		// command-line if you want to run and allow a remote debugger to connect
		if (debug)
			Collections.addAll(command, "-Xdebug", "-Xnoagent", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8787");
		int result = run(message, command.toArray(new String[command.size()]));
		// 13 means that we wrote something out in the log file.
		// so try and parse it and fail via that message if we can.
		if (result == 13)
			parseExitdata(message);
		return result;
	}

	protected void debugEclipse(boolean on) {
		this.debug = on;
	}

	protected void parseExitdata(String message) {
		// if the exit data contains a message telling us the location of the log file, then get it
		String data = TestActivator.getContext().getProperty("eclipse.exitdata");
		if (data == null)
			return;
		String log = null;
		// big hack but for now assume the log file path is the last segment of the error message
		for (StringTokenizer tokenizer = new StringTokenizer(data); tokenizer.hasMoreTokens();)
			log = tokenizer.nextToken();
		if (log == null)
			return;
		// remove trailing "."
		if (log.endsWith("."))
			log = log.substring(0, log.length() - 1);
		String errors = read(log);
		if (errors == null)
			return;
		// fail using the text from the log file
		assertOK(message, new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, errors));
	}

	// Fully read the file pointed to by the given path
	private String read(String path) {
		File file = new File(path);
		if (!file.exists())
			return null;
		StringBuilder buffer = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				buffer.append(line);
				buffer.append('\n');
			}
		} catch (IOException e) {
			// TODO
		}
		return buffer.toString();
	}

	public int runInitialize(String message) {
		return runEclipse(message, new String[] {"-initialize"});
	}

	public int runDirectorToUpdate(String message, String sourceRepo, String iuToInstall, String iuToUninstall) {
		return runEclipse(message, new String[] {"-application", "org.eclipse.equinox.p2.director", "-repository", sourceRepo, "-installIU", iuToInstall, "-uninstallIU", iuToUninstall});
	}

	public int runDirectorToRevert(String message, String sourceRepo, String timestampToRevertTo) {
		return runEclipse(message, new String[] {"-application", "org.eclipse.equinox.p2.director", "-repository", sourceRepo, "-revert", timestampToRevertTo});
	}

	public int runDirectorToInstall(String message, File installFolder, String sourceRepo, String iuToInstall) {
		String[] command = new String[] {//
				"-application", "org.eclipse.equinox.p2.director", //
				"-repository", sourceRepo, "-installIU", iuToInstall, //
				"-destination", installFolder.getAbsolutePath(), //
				"-bundlepool", installFolder.getAbsolutePath(), //
				"-roaming", "-profile", "PlatformProfile", "-profileProperties", "org.eclipse.update.install.features=true", //
				"-p2.os", Platform.getOS(), "-p2.ws", Platform.getWS(), "-p2.arch", Platform.getOSArch()};
		return runEclipse(message, command);
	}

	public int runVerifierBundle(File destination) {
		if (destination == null)
			destination = output;
		String message = "Running the verifier bundle at: " + destination;
		return runEclipse(message, destination, new String[] {"-application", "org.eclipse.equinox.p2.tests.verifier.application", "-consoleLog"});
	}

	public int installAndRunVerifierBundle(File destination) {
		if (destination == null)
			destination = output;
		try {
			copyBundle(VERIFIER_BUNDLE_ID, null, destination);
		} catch (IOException e) {
			fail("Could not find the verifier bundle");
		}
		int returnCode = runVerifierBundle(destination);
		deleteVerifierBundle(destination);
		return returnCode;
	}

	public int installAndRunVerifierBundle35(File destination) {
		if (destination == null)
			destination = output;
		try {
			copyBundle(VERIFIER_BUNDLE_ID, getTestData(VERIFIER_BUNDLE_ID + "3.5", "testData/VerifierBundle35/org.eclipse.equinox.p2.tests.verifier_1.0.0.jar"), destination);
		} catch (IOException e) {
			fail("Could not find the verifier bundle");
		}
		int returnCode = runVerifierBundle(destination);
		deleteVerifierBundle(destination);
		return returnCode;
	}

	private void deleteVerifierBundle(File destination) {
		if (destination == null)
			destination = output;
		destination = new File(destination, getRootFolder() + "plugins");
		File[] verifierBundle = destination.listFiles((FilenameFilter) (dir, name) -> {
			if (name.startsWith(VERIFIER_BUNDLE_ID))
				return true;
			return false;
		});
		if (verifierBundle != null && verifierBundle.length > 0)
			verifierBundle[0].delete();
	}

	private static void loadPlatformZipPropertiesFromFile() {
		File installLocation = getInstallLocation();
		if (installLocation != null) {
			// parent will be "eclipse" and the parent's parent will be "eclipse-testing"
			File parent = installLocation.getParentFile();
			if (parent != null) {
				parent = parent.getParentFile();
				if (parent != null) {
					File propertiesFile = new File(parent, "equinoxp2tests.properties");
					if (!propertiesFile.exists())
						return;
					archiveAndRepositoryProperties = new Properties();
					try {
						try (InputStream is = new BufferedInputStream(new FileInputStream(propertiesFile))) {
							archiveAndRepositoryProperties.load(is);
						}
					} catch (IOException e) {
						return;
					}
				}
			}
		}
	}

	static protected String getRootFolder() {
		String eclipseFolder = "eclipse/";
		if (Platform.getWS().equals(Platform.WS_COCOA))
			eclipseFolder = "Eclipse.app/Contents/Eclipse/";
		return eclipseFolder;
	}

	static protected String getExeFolder() {
		String eclipseFolder = "eclipse/";
		if (Platform.getWS().equals(Platform.WS_COCOA))
			eclipseFolder = "Eclipse.app/Contents/MacOS/";
		return eclipseFolder;
	}
}