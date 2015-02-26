/*******************************************************************************
 * Copyright (c) 2008, 2015 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   Red Hat Inc. - Bug 460967
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxFwConfigFileParser;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxManipulatorImpl;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.pde.internal.publishing.Activator;
import org.osgi.framework.Constants;

public class DataLoader {

	private final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + "=" + FrameworkAdmin.class.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private final static String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)"; //$NON-NLS-1$ //$NON-NLS-2$
	//String filterFwVersion = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_VERSION + "=" + props.getProperty("equinox.fw.version") + ")";
	private final static String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)"; //$NON-NLS-1$ //$NON-NLS-2$
	//String filterLauncherVersion = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_VERSION + "=" + props.getProperty("equinox.launcher.version") + ")";
	private final static String frameworkAdminFillter = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ")"; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL = "org.eclipse.equinox.simpleconfigurator.configUrl"; //$NON-NLS-1$

	private Manipulator manipulator;
	private final File configurationLocation;

	/**
	 * 
	 * @param configurationLocation configuration file (i.e. config.ini).
	 * @param executableLocation executable file (i.e. eclipse.exe). 
	 */
	public DataLoader(File configurationLocation, File executableLocation) {
		this.configurationLocation = configurationLocation;
		initializeFrameworkManipulator(configurationLocation.getParentFile(), executableLocation);
	}

	private void initializeFrameworkManipulator(File config, File executable) {
		getFrameworkManipulator();

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwPersistentDataLocation(config, true);
		launcherData.setLauncher(executable);
		if (executable == null)
			launcherData.setHome(config.getParentFile());
		try {
			manipulator.load();
		} catch (IllegalStateException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (FrameworkAdminRuntimeException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	public ConfigData getConfigData() {
		if (manipulator == null)
			return null;

		EquinoxFwConfigFileParser parser = new EquinoxFwConfigFileParser(Activator.getContext());
		try {
			if (configurationLocation != null && configurationLocation.exists())
				parser.readFwConfig(manipulator, configurationLocation);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error loading config.", e)); //$NON-NLS-1$ //TODO: Fix error string
		} catch (URISyntaxException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error loading config.", e)); //$NON-NLS-1$ //TODO: Fix error string
		}
		ConfigData data = manipulator.getConfigData();
		String value = data.getProperty(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL);
		if (value != null) {
			try {
				//config.ini uses simpleconfigurator, read the bundles.info and replace the bundle infos
				SimpleConfiguratorManipulator simpleManipulator = ServiceHelper.getService(Activator.getContext(), SimpleConfiguratorManipulator.class);
				//input stream will be buffered and closed for us
				BundleInfo[] bundleInfos = simpleManipulator.loadConfiguration(new URL(value).openStream(), null);
				data.setBundles(bundleInfos);
			} catch (MalformedURLException e1) {
				// ignore
			} catch (IOException e1) {
				// ignore
			}
			try {
				data.setProperty(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL, EquinoxManipulatorImpl.makeRelative(value, configurationLocation.toURL()));
			} catch (MalformedURLException e) {
				//ignore
			}
		}

		return data;
	}

	public LauncherData getLauncherData() {
		return manipulator == null ? null : manipulator.getLauncherData();
	}

	/**
	 * Obtains the framework manipulator instance. Throws an exception
	 * if it could not be created.
	 */
	private void getFrameworkManipulator() {
		FrameworkAdmin admin = getFrameworkAdmin();
		if (admin == null)
			throw new RuntimeException("Framework admin service not found"); //$NON-NLS-1$
		manipulator = admin.getManipulator();
		if (manipulator == null)
			throw new RuntimeException("Framework manipulator not found"); //$NON-NLS-1$
	}

	private FrameworkAdmin getFrameworkAdmin() {
		return ServiceHelper.getService(Activator.getContext(), FrameworkAdmin.class, frameworkAdminFillter);
	}
}
