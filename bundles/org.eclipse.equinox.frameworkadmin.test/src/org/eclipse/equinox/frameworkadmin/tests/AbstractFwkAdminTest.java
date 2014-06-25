/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.frameworkadmin.equinox.ParserUtils;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractFwkAdminTest extends TestCase {
	private ServiceTracker<Object, FrameworkAdmin> fwAdminTracker;
	private File testFolder;

	public AbstractFwkAdminTest(String name) {
		super(name);
	}

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
			for (int i = 0; i < children.length; i++)
				delete(children[i]);
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
				fwAdminTracker = new ServiceTracker<Object, FrameworkAdmin>(Activator.getContext(), filter, null);
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

	@Override
	protected void runTest() throws Throwable {
		super.runTest();

		//clean up after success
		if (testFolder != null && testFolder.exists()) {
			delete(testFolder);
			testFolder = null;
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (fwAdminTracker != null) {
			fwAdminTracker.close();
		}
	}

	public void assertIsFile(File file) {
		if (!file.exists())
			fail("File: " + file.toString() + " can't be found.");
		if (!file.isFile())
			fail("File: " + file.toString() + " is expected to be a file.");
	}

	public void assertIsDirectory(File file) {
		if (!file.exists())
			fail("Directory: " + file.toString() + " can't be found.");
		if (!file.isDirectory())
			fail("Directory: " + file.toString() + " is expected to be a directory.");
	}

	public void assertNothing(File file) {
		if (file.exists())
			fail("No file or directory should be there: " + file);
	}

	public void assertNotContent(File file, String search) {
		assertNotContent(null, file, search);
	}

	public void assertNotContent(String message, File file, String search) {
		if (!file.exists())
			fail("File: " + file.toString() + " can't be found.");
		try {
			BufferedReader reader = null;
			try {
				String failure = null;
				StringBuilder fileContent = new StringBuilder();
				reader = new BufferedReader(new FileReader(file));
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
			} finally {
				if (reader != null)
					reader.close();
			}
		} catch (FileNotFoundException e) {
			//ignore, caught before
		} catch (IOException e) {
			fail("String: " + search + " not found in " + file.getAbsolutePath());
		}
	}

	public void assertIniFileNotContain(File file, String argument, String value) {
		List<String> args = null;
		try {
			args = FileUtils.loadFile(file);
		} catch (IOException e) {
			fail("Can't read file " + file);
		}
		String tmp = ParserUtils.getValueForArgument(argument, args);
		if (tmp == null)
			return;

		assertTrue(tmp.indexOf(value) == -1);
	}

	private String getProperty(File file, String property) {
		Properties p = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			p.load(fis);			
		} catch (FileNotFoundException e) {
			fail("Can't find file " + file);
		} catch (IOException e) {
			fail("Error reading " + file);
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
					//ignore
				}
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

	public void assertEquals(String[] array1, String[] array2) {
		if (array1 == null || array2 == null) {
			if (array1 == array2)
				return;
			fail(array1 + " not equal to " + array2);
		}
		assertEquals(array1.length, array2.length);
		for (int i = 0; i < array1.length; i++) {
			assertEquals(array1[i], array2[i]);
		}
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
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				while (reader.ready()) {
					String line = reader.readLine();
					fileContent.append(line).append('\n');
					if (line.indexOf(lines[idx]) >= 0) {
						if(++idx >= lines.length)
							return;
					}
				}
			} finally {
				if (reader != null)
					reader.close();
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

	public void stopSimpleConfiguratorManipulator() {
		final String SIMPLECONFIGURATOR_MANIPULATOR = "org.eclipse.equinox.simpleconfigurator.manipulator";
		Bundle manipulatorBundle = Platform.getBundle(SIMPLECONFIGURATOR_MANIPULATOR);
		if (manipulatorBundle == null)
			return;
		try {
			manipulatorBundle.stop();
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
			for (int i = 0; i < children.length; i++)
				copy(message, children[i], new File(target, children[i].getName()));
			return;
		}
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new BufferedInputStream(new FileInputStream(source));
			output = new BufferedOutputStream(new FileOutputStream(target));

			byte[] buffer = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = input.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} catch (IOException e) {
			fail(message + ": " + e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close input stream on: " + source.getAbsolutePath());
					e.printStackTrace();
				}
			}
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					System.err.println("Exception while trying to close output stream on: " + target.getAbsolutePath());
					e.printStackTrace();
				}
			}
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
			String osPath = new Path(FileLocator.toFileURL(base).getPath()).toOSString();
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
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(location));
			for (int j = 0; j < lines.length; j++) {
				bw.write(lines[j]);
				bw.newLine();
			}
			bw.flush();

		} catch (IOException e) {
			fail("Fail writing eclipse.ini file");
		} finally {
			if (bw != null)
				try {
					bw.close();
				} catch (IOException e) {
					fail("Fail writing eclipse.ini file in " + location);
				}
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
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getLocation().equals(location))
				return;
		}
		fail(message + " Can't find the bundle info " + location);
	}

}
