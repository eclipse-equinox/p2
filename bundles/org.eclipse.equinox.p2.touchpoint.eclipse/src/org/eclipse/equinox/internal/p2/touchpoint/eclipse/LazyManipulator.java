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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.IOException;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class LazyManipulator implements Manipulator {

	private final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + '=' + FrameworkAdmin.class.getName() + ')'; //$NON-NLS-1$
	private final static String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static String filterFwAdmin = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ')'; //$NON-NLS-1$;

	private Manipulator manipulator;
	private final IProfile profile;

	public LazyManipulator(IProfile profile) {
		this.profile = profile;
	}

	private void loadDelegate() {
		if (manipulator != null)
			return;

		manipulator = getFrameworkManipulator();
		if (manipulator == null)
			throw new IllegalStateException(Messages.failed_acquire_framework_manipulator);

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(Util.getConfigurationFolder(profile));
		launcherData.setLauncher(Util.getLauncherPath(profile));
		launcherData.setLauncherConfigLocation(Util.getLauncherConfigLocation(profile));

		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			//if fwJar is not included, this exception will be thrown. But ignore it. 
			LogHelper.log(Util.createError(Messages.error_loading_manipulator, e));
		} catch (FrameworkAdminRuntimeException e) {
			// TODO: consider throwing an exception
			LogHelper.log(Util.createError(Messages.error_loading_manipulator, e));
		} catch (IOException e) {
			// TODO: consider throwing an exception
			LogHelper.log(Util.createError(Messages.error_loading_manipulator, e));
		}
		//TODO These values should be inserted by a configuration unit (bug 204124)
		manipulator.getConfigData().setFwDependentProp("eclipse.p2.profile", profile.getProfileId()); //$NON-NLS-1$
		manipulator.getConfigData().setFwDependentProp("eclipse.p2.data.area", Util.computeRelativeAgentLocation(profile)); //$NON-NLS-1$
	}

	public static FrameworkAdmin getFrameworkAdmin() {
		ServiceTracker fwAdminTracker = null;
		try {
			Filter filter = Activator.getContext().createFilter(filterFwAdmin);
			fwAdminTracker = new ServiceTracker(Activator.getContext(), filter, null);
			fwAdminTracker.open();
			FrameworkAdmin fwAdmin = (FrameworkAdmin) fwAdminTracker.getService();
			return fwAdmin;
		} catch (InvalidSyntaxException e) {
			//Can't happen we are writing the filter ourselves
			return null;
		} finally {
			if (fwAdminTracker != null)
				fwAdminTracker.close();
		}
	}

	private Manipulator getFrameworkManipulator() {
		FrameworkAdmin fwAdmin = getFrameworkAdmin();
		if (fwAdmin != null)
			return fwAdmin.getManipulator();
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

	public void load() throws IllegalStateException, FrameworkAdminRuntimeException {
		loadDelegate();
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
