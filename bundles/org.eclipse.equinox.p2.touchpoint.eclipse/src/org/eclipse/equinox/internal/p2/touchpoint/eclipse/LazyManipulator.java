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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.p2.engine.Profile;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class LazyManipulator implements Manipulator {

	private final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + '=' + FrameworkAdmin.class.getName() + ')'; //$NON-NLS-1$
	private final static String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static String filterFwAdmin = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ')'; //$NON-NLS-1$;

	private Manipulator manipulator;
	private final Profile profile;

	public LazyManipulator(Profile profile) {
		this.profile = profile;
	}

	private void loadDelegate() {
		if (manipulator != null)
			return;

		manipulator = getFrameworkManipulator();
		if (manipulator == null)
			throw new IllegalStateException("Could not acquire the framework manipulator service."); //$NON-NLS-1$

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(Util.getConfigurationFolder(profile));
		launcherData.setLauncher(new File(Util.getInstallFolder(profile), Util.getLauncherName(profile)));
		try {
			manipulator.load();
		} catch (IllegalStateException e2) {
			// TODO if fwJar is not included, this exception will be thrown. But ignore it. 
			//				e2.printStackTrace();
		} catch (FrameworkAdminRuntimeException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		//TODO These values should be inserted by a configuration unit (bug 204124)
		manipulator.getConfigData().setFwDependentProp("eclipse.p2.profile", profile.getProfileId()); //$NON-NLS-1$
		manipulator.getConfigData().setFwDependentProp("eclipse.p2.data.area", Util.computeRelativeAgentLocation(profile)); //$NON-NLS-1$
	}

	private Manipulator getFrameworkManipulator() {
		ServiceTracker fwAdminTracker = null;
		try {
			Filter filter = Activator.getContext().createFilter(filterFwAdmin);
			fwAdminTracker = new ServiceTracker(Activator.getContext(), filter, null);
			fwAdminTracker.open();
			FrameworkAdmin fwAdmin = (FrameworkAdmin) fwAdminTracker.getService();
			if (fwAdmin != null)
				return fwAdmin.getManipulator();
		} catch (InvalidSyntaxException e) {
			// should not happen
			e.printStackTrace();
		} finally {
			if (fwAdminTracker != null)
				fwAdminTracker.close();
		}
		return null;
	}

	public void save(boolean backup) throws IOException, FrameworkAdminRuntimeException {
		if (manipulator != null)
			manipulator.save(backup);
	}

	// DELEGATE METHODS

	public BundlesState getBundlesState() throws FrameworkAdminRuntimeException {
		loadDelegate();
		return manipulator.getBundlesState();
	}

	public ConfigData getConfigData() throws FrameworkAdminRuntimeException {
		loadDelegate();
		return manipulator.getConfigData();
	}

	public BundleInfo[] getExpectedState() throws IllegalStateException, IOException, FrameworkAdminRuntimeException {
		loadDelegate();
		return manipulator.getExpectedState();
	}

	public LauncherData getLauncherData() throws FrameworkAdminRuntimeException {
		loadDelegate();
		return manipulator.getLauncherData();
	}

	public long getTimeStamp() {
		loadDelegate();
		return manipulator.getTimeStamp();
	}

	public void initialize() {
		loadDelegate();
		manipulator.initialize();
	}

	public void load() throws IllegalStateException, IOException, FrameworkAdminRuntimeException {
		loadDelegate();
		manipulator.load();
	}

	public void setConfigData(ConfigData configData) {
		loadDelegate();
		manipulator.setConfigData(configData);
	}

	public void setLauncherData(LauncherData launcherData) {
		loadDelegate();
		manipulator.setLauncherData(launcherData);
	}
}
