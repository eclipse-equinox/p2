/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import static org.junit.Assert.fail;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractFwkAdminTest {
	private ServiceTracker<Object, FrameworkAdmin> fwAdminTracker;
	private File testFolder;

	/**
	 * Copy an input stream to an output stream.
	 * Optionally close the streams when done.
	 * Return the number of bytes written.
	 */
	public static int copyStream(InputStream in, boolean closeIn, OutputStream out, boolean closeOut) throws IOException {
		try {
			int written = 0;
			byte[] buffer = new byte[16 * 1024];
			int len;
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
				written += len;
			}
			return written;
		} finally {
			try {
				if (closeIn) {
					in.close();
				}
			} finally {
				if (closeOut) {
					out.close();
				}
			}
		}
	}

	public static boolean delete(File file) {
		if (!file.exists())
			return true;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (File child : children) {
				delete(child);
			}
		}
		return file.delete();
	}

	public FrameworkAdmin getEquinoxFrameworkAdmin() throws BundleException {
		final String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + "=" + FrameworkAdmin.class.getName() + ")";
		final String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)";
		final String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)";
		final String filterFwAdmin = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ")";

		String FWK_ADMIN_EQ = "org.eclipse.equinox.frameworkadmin.equinox";
		Bundle b = Platform.getBundle(FWK_ADMIN_EQ);
		if (b == null)
			throw new IllegalStateException("Bundle: " + FWK_ADMIN_EQ + " is required for this test");
		b.start();

		if (fwAdminTracker == null) {
			Filter filter;
			try {
				filter = Activator.getContext().createFilter(filterFwAdmin);
				fwAdminTracker = new ServiceTracker<>(Activator.getContext(), filter, null);
				fwAdminTracker.open();
			} catch (InvalidSyntaxException e) {
				// never happens
				e.printStackTrace();
			}
		}
		return fwAdminTracker.getService();
	}

	protected File getTestFolder(String name) {
		return getTestFolder(name, true);
	}

	protected File getTestFolder(String name, boolean clean) {
		Location instanceLocation = Platform.getInstanceLocation();
		URL url = instanceLocation != null ? instanceLocation.getURL() : null;
		if (instanceLocation == null || !instanceLocation.isSet() || url == null) {
			testFolder = Activator.getContext().getDataFile(name);
		} else {
			testFolder = new File(url.getFile(), name);
		}

		if (clean && testFolder.exists())
			delete(testFolder);
		testFolder.mkdirs();
		return testFolder;
	}

	@Before
	public void runTest() throws Throwable {
		//clean up after success
		if (testFolder != null && testFolder.exists()) {
			delete(testFolder);
			testFolder = null;
		}
	}

	@After
	public void tearDown() throws Exception {
		if (fwAdminTracker != null) {
			fwAdminTracker.close();
		}
	}

	public void assertNotContent(File file, String search) {
		assertNotContent(null, file, search);
	}

	public void assertNotContent(String message, File file, String search) {
		if (!file.exists())
			fail("File: " + file.toString() + " can't be found.");
		try {
			String failure = null;
			StringBuilder fileContent = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new FileReader(file)) ){
				while (reader.ready()) {
					String line = reader.readLine();
					fileContent.append(line).append('\n');
					if (line.indexOf(search) >= 0 && failure == null) {
						failure = "The string: " + search + " was not expected in file '" + file.getAbsolutePath() + "'";
					}
				}
				if (failure != null) {
					// dump whole file content
					fail((message != null ? message : failure) + "\n" + fileContent);
				}
			}
		} catch (FileNotFoundException e) {
			//ignore, caught before
		} catch (IOException e) {
			fail("String: " + search + " not found in " + file.getAbsolutePath());
		}
	}

	private String getProperty(File file, String property) {
		Properties p = new Properties();
		try (FileInputStream fis = new FileInputStream(file)) {
			p.load(fis);
		} catch (FileNotFoundException e) {
			fail("Can't find file " + file);
		} catch (IOException e) {
			fail("Error reading " + file);
		}
		return p.getProperty(property);
	}

	public void assertPropertyContains(File file, String property, String text) {
		String value = getProperty(file, property);
		if (value == null)
			fail("property: " + property + " not found in: " +file);

		int index = value.indexOf(text);
		if (index == -1)
			fail(text + " not found in property:" + property + " for file: " +file);
	}

	public void assertNotPropertyContains(File file, String property, String text) {
		String value = getProperty(file, property);
		if (value == null)
			return;

		int index = value.indexOf(text);
		if (index != -1)
			fail(text + " found in property:" + property + " for file: " +file);
	}

	public void assertContent(File file, String... search) {
		assertContent(null, file, search);
	}

	public void assertContent(String message, File file, String... lines) {
		if (!file.exists())
			fail("File: " + file.toString() + " can't be found.");
		int idx = 0;
		StringBuilder fileContent = new StringBuilder();
		try {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				while (reader.ready()) {
					String line = reader.readLine();
					fileContent.append(line).append('\n');
					if (line.indexOf(lines[idx]) >= 0) {
						if(++idx >= lines.length)
							return;
					}
				}
			}
		} catch (FileNotFoundException e) {
			//ignore, caught before
		} catch (IOException e) {
			fail("String: " + lines[idx] + " not found in " + file.getAbsolutePath());
		}
		fail("String: " + lines[idx] + " not found in\n" + fileContent);
	}

	public void startSimpleConfiguratorManipulator() {
		final String SIMPLECONFIGURATOR_MANIPULATOR = "org.eclipse.equinox.simpleconfigurator.manipulator";
		Bundle manipulatorBundle = Platform.getBundle(SIMPLECONFIGURATOR_MANIPULATOR);
		if (manipulatorBundle == null)
			fail("Bundle: " + SIMPLECONFIGURATOR_MANIPULATOR + " is required for this test");
		try {
			manipulatorBundle.start();
		} catch (BundleException e) {
			fail("Exception while starting up " + SIMPLECONFIGURATOR_MANIPULATOR + ' ' + e.getMessage());
		}
	}

	/*
	 * Copy
	 * - if we have a file, then copy the file
	 * - if we have a directory then merge
	 */
	public static void copy(String message, File source, File target) {
		if (!source.exists())
			return;
		target.getParentFile().mkdirs();
		if (source.isDirectory()) {
			if (target.exists() && target.isFile())
				target.delete();
			if (!target.exists())
				target.mkdirs();
			File[] children = source.listFiles();
			for (File child : children) {
				copy(message, child, new File(target, child.getName()));
			}
			return;
		}
		try (InputStream input = new BufferedInputStream(new FileInputStream(source));
				OutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {

			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} catch (IOException e) {
			fail(message + ": " + e);
		}
	}

	/*
	 * Look up and return a file handle to the given entry in the bundle.
	 */
	protected File getTestData(String message, String entry) {
		if (entry == null)
			fail(message + " entry is null.");
		URL base = Activator.getContext().getBundle().getEntry(entry);
		if (base == null)
			fail(message + " entry not found in bundle: " + entry);
		try {
			String osPath = IPath.fromOSString(FileLocator.toFileURL(base).getPath()).toOSString();
			File result = new File(osPath);
			if (!result.getCanonicalPath().equals(result.getPath()))
				fail(message + " result path: " + result.getPath() + " does not match canonical path: " + result.getCanonicalFile().getPath());
			return result;
		} catch (IOException e) {
			fail(message + ": " + e);
		}
		// avoid compile error... should never reach this code
		return null;
	}

	protected Manipulator getFrameworkManipulator(File configuration, File launcher) throws BundleException {
		startSimpleConfiguratorManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configuration);
		launcherData.setLauncher(launcher);

		return manipulator;
	}

	//This is a dumb helper writing out the values as they have been passed to it.
	protected void writeEclipseIni(File location, String[] lines) {
		location.getParentFile().mkdirs();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(location))){

			for (String line : lines) {
				bw.write(line);
				bw.newLine();
			}
			bw.flush();

		} catch (IOException e) {
			fail("Fail writing eclipse.ini file in " + location);
		}
	}

	//This is a dumb helper writing out the values as they have been passed to it
	protected void writeConfigIni(File location, Properties properties) {
		location.getParentFile().mkdirs();
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(location);
			properties.store(out, "#header");
		} catch (IOException e) {
			fail("Faile writing config.ini in" + location);
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			out = null;
		}
	}

	public void assertContains(String message, BundleInfo[] bundles, URI location) {
		for (BundleInfo bundle : bundles) {
			if (bundle.getLocation().equals(location)) {
				return;
			}
		}
		fail(message + " Can't find the bundle info " + location);
	}

}
