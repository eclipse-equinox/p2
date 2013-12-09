/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import org.eclipse.equinox.internal.provisional.configuratormanipulator.ConfiguratorManipulator;
import org.eclipse.equinox.internal.provisional.configuratormanipulator.ConfiguratorManipulatorFactory;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.osgi.framework.*;
import org.osgi.service.startlevel.StartLevel;

public class EquinoxFwAdminImpl implements FrameworkAdmin {

	boolean active = false;

	private ConfiguratorManipulator configuratorManipulator = null;

	BundleContext context = null;

	private boolean runningFw = false;

	private PlatformAdmin platformAdmin;
	private StartLevel startLevelService;

	public EquinoxFwAdminImpl() {
		this(null, false);
	}

	//	private String configuratorManipulatorFactoryName = null;

	EquinoxFwAdminImpl(BundleContext context) {
		this(context, false);
	}

	EquinoxFwAdminImpl(BundleContext context, boolean runningFw) {
		this.context = context;
		this.active = true;
		this.runningFw = runningFw;
	}

	EquinoxFwAdminImpl(String configuratorManipulatorFactoryName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		this.context = null;
		this.active = true;
		this.runningFw = false;
		//		this.configuratorManipulatorFactoryName = configuratorManipulatorFactoryName;
		loadConfiguratorManipulator(configuratorManipulatorFactoryName);
	}

	/**
	 * DS component activator
	 * @param aContext The bundle context
	 */
	public void activate(BundleContext aContext) {
		this.context = aContext;
		this.runningFw = isRunningFw();
		Log.init(aContext);
	}

	void deactivate() {
		active = false;
		Log.dispose();
	}

	public ConfiguratorManipulator getConfiguratorManipulator() {
		return configuratorManipulator;
	}

	public Manipulator getManipulator() {
		return new EquinoxManipulatorImpl(context, this, platformAdmin, startLevelService, false);
	}

	public Manipulator getRunningManipulator() {
		if (!this.runningFw)
			return null;
		return new EquinoxManipulatorImpl(context, this, platformAdmin, startLevelService, true);
	}

	public boolean isActive() {
		return active;
	}

	/**
	 * If both the vendor and the Bundle-Version in the manifest match, 
	 * return true. Otherwise false.
	 *  
	 * @return flag true if the ManipulatorAdmin object can handle currently running fw launch. 
	 */
	boolean isRunningFw() {
		//TODO implementation for Eclipse.exe and for Equinox
		String fwVendor = context.getProperty(Constants.FRAMEWORK_VENDOR);
		if (!"Eclipse".equals(fwVendor)) //$NON-NLS-1$
			return false;
		//TODO decide if this version can be supported by this bundle.
		Dictionary<String, String> header = context.getBundle(0).getHeaders();
		String versionSt = header.get(Constants.BUNDLE_VERSION);
		Version version = new Version(versionSt);
		int value = version.compareTo(new Version(EquinoxConstants.FW_VERSION));
		if (value > 0) {
			return true;
		}
		// TODO need to identify the version of eclipse.exe used for this launch, if used. 
		return false;
	}

	public Process launch(Manipulator manipulator, File cwd) throws IllegalArgumentException, FrameworkAdminRuntimeException, IOException {
		//return new EclipseLauncherImpl(context, this).launch(manipulator, cwd);
		return new EclipseLauncherImpl(this).launch(manipulator, cwd);
	}

	private void loadConfiguratorManipulator(String configuratorManipulatorFactoryName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		if (configuratorManipulatorFactoryName == null)
			this.configuratorManipulator = null;
		else
			this.configuratorManipulator = ConfiguratorManipulatorFactory.getInstance(configuratorManipulatorFactoryName);
		return;
	}

	public void setPlatformAdmin(PlatformAdmin admin) {
		this.platformAdmin = admin;
	}

	public void setStartLevel(StartLevel sl) {
		this.startLevelService = sl;
	}

}
