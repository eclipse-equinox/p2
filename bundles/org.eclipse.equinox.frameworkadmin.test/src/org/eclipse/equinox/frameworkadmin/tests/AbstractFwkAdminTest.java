/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.tests;

import org.osgi.framework.BundleException;

import java.io.File;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

import java.io.*;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdmin;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractFwkAdminTest extends TestCase {
	private ServiceTracker fwAdminTracker;

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
				fwAdminTracker = new ServiceTracker(Activator.getContext(), filter, null);
				fwAdminTracker.open();
			} catch (InvalidSyntaxException e) {
				// never happens
				e.printStackTrace();
			}
		}
		return (FrameworkAdmin) fwAdminTracker.getService();
	}

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
		if (!file.exists())
			fail("File: " + file.toString() + " can't be found.");
		try {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				while (reader.ready()) {
					String line = reader.readLine();
					if (line.indexOf(search) >= 0)
						fail("The string: " + search + " was not expected in this file: " + file.getAbsolutePath());
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

	public void assertContent(File file, String search) {
		if (!file.exists())
			fail("File: " + file.toString() + " can't be found.");
		try {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				while (reader.ready()) {
					String line = reader.readLine();
					if (line.indexOf(search) >= 0)
						return;
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
		fail("String:" + search + " not found");
	}

	public void startSimpleConfiguratormManipulator() {
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

	public void stopSimpleConfiguratormManipulator() {
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
		startSimpleConfiguratormManipulator();
		FrameworkAdmin fwkAdmin = getEquinoxFrameworkAdmin();
		Manipulator manipulator = fwkAdmin.getManipulator();

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(configuration);
		launcherData.setLauncher(launcher);
		
		return manipulator;
	}
}
