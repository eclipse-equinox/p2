/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxConstants;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxFwConfigFileParser;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class AccumulateConfigDataAction extends AbstractPublishingAction {

	private final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + "=" + FrameworkAdmin.class.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private final static String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)"; //$NON-NLS-1$ //$NON-NLS-2$
	//String filterFwVersion = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_VERSION + "=" + props.getProperty("equinox.fw.version") + ")";
	private final static String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)"; //$NON-NLS-1$ //$NON-NLS-2$
	//String filterLauncherVersion = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_VERSION + "=" + props.getProperty("equinox.launcher.version") + ")";
	private final static String frameworkAdminFillter = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ")"; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_MANIPULATOR = "org.eclipse.equinox.simpleconfigurator.manipulator"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_EQUINOX_FRAMEWORKADMIN_EQUINOX = "org.eclipse.equinox.frameworkadmin.equinox"; //$NON-NLS-1$

	private String configSpec;
	private File configurationLocation;
	private Manipulator manipulator;
	private ServiceTracker frameworkAdminTracker;
	private File executableLocation;

	public AccumulateConfigDataAction(IPublisherInfo info, String configSpec, File configurationLocation, File executableLocation) {
		this.configSpec = configSpec;
		this.configurationLocation = configurationLocation;
		this.executableLocation = executableLocation;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		initializeFrameworkManipulator(configurationLocation, executableLocation);
		storeConfigData(info, configSpec, results);
		return Status.OK_STATUS;
	}

	protected void storeConfigData(IPublisherInfo info, String configSpec, IPublisherResult result) {
		if (result.getConfigData().containsKey(configSpec))
			return; //been here, done this

		File fwConfigFile = new File(configurationLocation, EquinoxConstants.CONFIG_INI);
		if (fwConfigFile.exists()) {
			ConfigData data = loadConfigData(fwConfigFile);
			result.getConfigData().put(configSpec, data);
		}
	}

	public void initializeFrameworkManipulator(File config, File executable) {
		createFrameworkManipulator();

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwPersistentDataLocation(config, true);
		launcherData.setLauncher(executable);
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

	public ConfigData loadConfigData(File location) {
		if (manipulator == null)
			return null;

		EquinoxFwConfigFileParser parser = new EquinoxFwConfigFileParser(Activator.getContext());
		try {
			parser.readFwConfig(manipulator, location);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return manipulator.getConfigData();
	}

	public LauncherData getLauncherData() {
		return manipulator == null ? null : manipulator.getLauncherData();
	}

	/**
	 * Obtains the framework manipulator instance. Throws an exception
	 * if it could not be created.
	 */
	protected void createFrameworkManipulator() {
		FrameworkAdmin admin = getFrameworkAdmin();
		if (admin == null)
			throw new RuntimeException("Framework admin service not found"); //$NON-NLS-1$
		manipulator = admin.getManipulator();
		if (manipulator == null)
			throw new RuntimeException("Framework manipulator not found"); //$NON-NLS-1$
	}

	private FrameworkAdmin getFrameworkAdmin() {
		if (frameworkAdminTracker == null) {
			try {
				Filter filter = Activator.getContext().createFilter(frameworkAdminFillter);
				frameworkAdminTracker = new ServiceTracker(Activator.getContext(), filter, null);
				frameworkAdminTracker.open();
			} catch (InvalidSyntaxException e) {
				// never happens
			}
		}
		FrameworkAdmin admin = (FrameworkAdmin) frameworkAdminTracker.getService();
		if (admin == null) {
			startBundle(ORG_ECLIPSE_EQUINOX_FRAMEWORKADMIN_EQUINOX);
			startBundle(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_MANIPULATOR);
			admin = (FrameworkAdmin) frameworkAdminTracker.getService();
		}
		return admin;
	}

	private boolean startBundle(String bundleId) {
		PackageAdmin packageAdmin = (PackageAdmin) ServiceHelper.getService(Activator.getContext(), PackageAdmin.class.getName());
		if (packageAdmin == null)
			return false;

		Bundle[] bundles = packageAdmin.getBundles(bundleId, null);
		if (bundles != null && bundles.length > 0) {
			for (int i = 0; i < bundles.length; i++) {
				try {
					if ((bundles[0].getState() & Bundle.RESOLVED) > 0) {
						bundles[0].start();
						return true;
					}
				} catch (BundleException e) {
					// failed, try next bundle
				}
			}
		}
		return false;
	}
}
