/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pascal Rapicault - Support for bundled macosx http://bugs.eclipse.org/57349
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.IOException;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class LazyManipulator implements Manipulator {

	private final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + '=' + FrameworkAdmin.class.getName() //$NON-NLS-1$
			+ ')';
	private final static String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME //$NON-NLS-1$
			+ "=Eclipse.exe)"; //$NON-NLS-1$
	private final static String filterFwAdmin = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ')'; //$NON-NLS-1$ ;

	private Manipulator manipulator;
	private final IProfile profile;
	private final IProvisioningAgent agent;

	public LazyManipulator(IProvisioningAgent agent, IProfile profile) {
		this.profile = profile;
		this.agent = agent;
	}

	private void loadDelegate() {
		if (manipulator != null) {
			return;
		}

		manipulator = getFrameworkManipulator();
		if (manipulator == null) {
			throw new IllegalStateException(Messages.failed_acquire_framework_manipulator);
		}

		LauncherData launcherData = manipulator.getLauncherData();
		launcherData.setFwConfigLocation(Util.getConfigurationFolder(profile));
		launcherData.setLauncher(Util.getLauncherPath(profile));
		launcherData.setLauncherConfigLocation(Util.getLauncherConfigLocation(profile));
		launcherData.setOS(Util.getOSFromProfile(profile));
		launcherData.setHome(Util.getInstallFolder(profile));

		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			// if fwJar is not included, this exception will be thrown. But ignore it.
			LogHelper.log(Util.createError(Messages.error_loading_manipulator, e));
			throw new IllegalStateException(Messages.error_loading_manipulator);
		} catch (FrameworkAdminRuntimeException e) {
			LogHelper.log(Util.createError(Messages.error_loading_manipulator, e));
		} catch (IOException e) {
			LogHelper.log(Util.createError(Messages.error_loading_manipulator, e));
			throw new IllegalStateException(Messages.error_loading_manipulator);
		}
		// TODO These values should be inserted by a configuration unit (bug 204124)
		manipulator.getConfigData().setProperty("eclipse.p2.profile", profile.getProfileId()); //$NON-NLS-1$
		manipulator.getConfigData().setProperty("eclipse.p2.data.area", //$NON-NLS-1$
				Util.getAgentLocation(agent).getRootLocation().toString());
	}

	public static FrameworkAdmin getFrameworkAdmin() {
		ServiceTracker<FrameworkAdmin, FrameworkAdmin> fwAdminTracker = null;
		try {
			Filter filter = Activator.getContext().createFilter(filterFwAdmin);
			fwAdminTracker = new ServiceTracker<>(Activator.getContext(), filter, null);
			fwAdminTracker.open();
			FrameworkAdmin fwAdmin = fwAdminTracker.getService();
			return fwAdmin;
		} catch (InvalidSyntaxException e) {
			// Can't happen we are writing the filter ourselves
			return null;
		} finally {
			if (fwAdminTracker != null) {
				fwAdminTracker.close();
			}
		}
	}

	private static Manipulator getFrameworkManipulator() {
		FrameworkAdmin fwAdmin = getFrameworkAdmin();
		if (fwAdmin != null) {
			return fwAdmin.getManipulator();
		}
		return null;
	}

	@Override
	public void save(boolean backup) throws IOException, FrameworkAdminRuntimeException {
		if (manipulator != null) {
			manipulator.save(backup);
		}
	}

	// DELEGATE METHODS

	@Override
	public BundlesState getBundlesState() throws FrameworkAdminRuntimeException {
		loadDelegate();
		return manipulator.getBundlesState();
	}

	@Override
	public ConfigData getConfigData() throws FrameworkAdminRuntimeException {
		loadDelegate();
		return manipulator.getConfigData();
	}

	@Override
	public BundleInfo[] getExpectedState() throws IllegalStateException, IOException, FrameworkAdminRuntimeException {
		loadDelegate();
		return manipulator.getExpectedState();
	}

	@Override
	public LauncherData getLauncherData() throws FrameworkAdminRuntimeException {
		loadDelegate();
		return manipulator.getLauncherData();
	}

	@Override
	public long getTimeStamp() {
		loadDelegate();
		return manipulator.getTimeStamp();
	}

	@Override
	public void initialize() {
		loadDelegate();
		manipulator.initialize();
	}

	@Override
	public void load() throws IllegalStateException, FrameworkAdminRuntimeException {
		loadDelegate();
	}

	@Override
	public void setConfigData(ConfigData configData) {
		loadDelegate();
		manipulator.setConfigData(configData);
	}

	@Override
	public void setLauncherData(LauncherData launcherData) {
		loadDelegate();
		manipulator.setLauncherData(launcherData);
	}
}
