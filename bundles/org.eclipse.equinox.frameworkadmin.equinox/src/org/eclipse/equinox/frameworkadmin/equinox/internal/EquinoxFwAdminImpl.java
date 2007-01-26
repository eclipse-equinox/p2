/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin.equinox.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.StringTokenizer;

import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class EquinoxFwAdminImpl implements FrameworkAdmin {

	/**
	 * If both the vendor and the Bundle-Version in the manifest match, 
	 * return true. Otherwise false.
	 *  
	 * @return flag true if the ManipulatorAdmin object can handle currently running fw launch. 
	 */
	static boolean isRunningFw(BundleContext context) {
		//TODO implementation for Eclipse.exe and for Equinox
		if (!context.getProperty(Constants.FRAMEWORK_VENDOR).equals("Eclipse.org"))
			return false;
		//TODO decide if this version can be supported by this bundle.
		Dictionary header = context.getBundle(0).getHeaders();
		String versionSt = (String) header.get("Bundle-Version");
		EclipseVersion version = new EclipseVersion(versionSt);
		if (!version.equals(new EclipseVersion(EquinoxConstants.FW_VERSION))) {
			return false;
		}
		// TODO need to identify the version of eclipse.exe used for this launch, if used. 

		//		String eclipseCommandsSt = context.getProperty(EquinoxConstants.PROP_ECLIPSE_COMMANDS);
		//	StringTokenizer tokenizer = new StringTokenizer(eclipseCommandsSt,"\n");
		return true;
	}

	BundleContext context = null;

	boolean active = false;

	private boolean runningFw = false;

	EquinoxFwAdminImpl(BundleContext context) {
		this(context, false);
	}

	EquinoxFwAdminImpl(BundleContext context, boolean runningFw) {
		this.context = context;
		active = true;
		this.runningFw = runningFw;
	}

	void deactivate() {
		active = false;
	}

	/**
	 * Return the configuration location.
	 * 
	 * @see Location
	 */
	private File getConfigurationLocation() {
		ServiceTracker tracker = null;
		Filter filter = null;
		try {
			filter = context.createFilter(Location.CONFIGURATION_FILTER);
		} catch (InvalidSyntaxException e) {
			// ignore this. It should never happen as we have tested the above format.
		}
		tracker = new ServiceTracker(context, filter, null);
		tracker.open();
		Location location = (Location) tracker.getService();
		URL url = location.getURL();
		if (!url.getProtocol().equals("file"))
			return null;
		return new File(url.getFile());
	}

	private File getLauncherFile() {
		File launcherFile = null;
		String eclipseCommandsSt = context.getProperty(EquinoxConstants.PROP_ECLIPSE_COMMANDS);
		StringTokenizer tokenizer = new StringTokenizer(eclipseCommandsSt, "\n");
		boolean found = false;
		String launcherSt = null;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (found) {
				launcherSt = token;
				break;
			}
			if (token.equals("-launcher"))
				found = true;
		}
		if (launcherSt != null)
			launcherFile = new File(launcherSt);
		return launcherFile;
	}

	public Manipulator getManipulator() {
		return new EquinoxManipulatorImpl(context, this);
	}

	public Manipulator getRunningManipulator() {
		if (this.runningFw) {
			EquinoxManipulatorImpl manipulator = new EquinoxManipulatorImpl(context, this);
			//TODO refine the implementation. using some MAGIC dependent on Eclipse.exe and Equinox implementation,
			// set parameters according to the current running fw.
			// 1. retrieve location data from Location services registered by equinox fw.
			File fwJar = new File(context.getProperty(EquinoxConstants.PROP_OSGI_FW));
			File fwConfigLocation = getConfigurationLocation();
			File launcherFile = getLauncherFile();

			// 2. Create a Manipulator object fully initialized to the current running fw.
			LauncherData launcherData = manipulator.getLauncherData();
			launcherData.setFwJar(fwJar);
			launcherData.setFwPersistentDataLocation(fwConfigLocation, false);
			launcherData.setLauncher(launcherFile);
			try {
				manipulator.load(false);
			} catch (FrameworkAdminRuntimeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return manipulator;
		}
		return null;
	}

	public boolean isActive() {
		return active;
	}

	public Process launch(Manipulator manipulator, File cwd) throws IllegalArgumentException, FrameworkAdminRuntimeException, IOException {
		return new EclipseLauncherImpl(context, this).launch(manipulator, cwd);
	}

}
