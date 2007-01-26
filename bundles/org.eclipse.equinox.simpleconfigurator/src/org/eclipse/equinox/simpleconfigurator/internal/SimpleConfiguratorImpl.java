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
package org.eclipse.equinox.simpleconfigurator.internal;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.eclipse.equinox.configurator.Configurator;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorConstants;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.osgi.framework.*;

/**
 * SimpleConfigurator provides ways to install bundles listed in a file accessible
 * by the specified URL and expect states for it in advance without actual application.
 * 
 * In every methods of SimpleConfiguration object,
 *  
 * 1. A value will be gotten by @{link BundleContext#getProperty(key) with 
 * {@link SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION} as a key.
 * 2. If it equals "true", it will do exclusive installation, which means that 
 * the bundles will not be listed in the specified url but installed at the time
 * of the method call except SystemBundle will be uninstalled. Otherwise, no uninstallation will not be done.
 */
public class SimpleConfiguratorImpl implements Configurator {

	final static BundleInfo[] NULL_BUNDLEINFOS = new BundleInfo[0];
	final static String FILTER_RUNNING_SYSTEM = "(" + FrameworkAdmin.SERVICE_PROP_KEY_RUNNING_SYSTEM_FLAG + "=true)";
	final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + "=" + FrameworkAdmin.class.getName() + ")";
	final static String FILTER_FRAMEWORKADMIN = "(&" + FILTER_OBJECTCLASS + FILTER_RUNNING_SYSTEM + ")";

	BundleContext context;

	ConfigApplier configApplier;

	SimpleConfiguratorImpl(BundleContext context) {
		this.context = context;
	}

	public void applyConfiguration(URL url) throws IOException {
		if (url == null)
			return;

		List bundleInfoList = SimpleConfiguratorUtils.readConfiguration(url);
		if (bundleInfoList.size() == 0)
			return;
		if (this.configApplier == null)
			configApplier = new ConfigApplier(context, this);
		configApplier.install(Utils.getBundleInfosFromList(bundleInfoList), this.isExclusiveInstallation());
	}

	public BundleInfo[] getExpectedStateRuntime(URL url) throws IOException {
		ServiceReference[] references = null;
		try {
			references = context.getServiceReferences(FrameworkAdmin.class.getName(), FILTER_RUNNING_SYSTEM);
		} catch (InvalidSyntaxException e) {
			// TODO Never happens.
			e.printStackTrace();
		}
		if (references == null)
			return new BundleInfo[0];
		FrameworkAdmin fwAdmin = (FrameworkAdmin) context.getService(references[0]);
		BundlesState state = fwAdmin.getManipulator().getBundlesState();
		return this.getExpectedStateRuntime(url, state);
	}

	private BundleInfo[] getExpectedStateRuntime(URL url, BundlesState state) throws IOException {
		if (!state.isFullySupported())
			throw new IllegalArgumentException("getExpectedStateRuntime(url,state) is not supported for this state implementation");

		List bundleInfoList = SimpleConfiguratorUtils.readConfiguration(url);
		BundleInfo[] toInstall = Utils.getBundleInfosFromList(bundleInfoList);
		BundleInfo[] currentBInfos = state.getExpectedState();
		List toUninstall = new LinkedList();
		boolean exclusiveInstallation = this.isExclusiveInstallation();
		if (exclusiveInstallation)
			for (int i = 0; i < currentBInfos.length; i++) {
				boolean install = false;
				for (int j = 0; j < toInstall.length; j++)
					if (currentBInfos[i].getLocation().equals(toInstall[j].getLocation())) {
						install = true;
						break;
					}
				if (!install)
					toUninstall.add(currentBInfos[i]);
			}

		for (int i = 0; i < toInstall.length; i++)
			state.installBundle(toInstall[i]);

		if (exclusiveInstallation)
			for (Iterator ite = toUninstall.iterator(); ite.hasNext();) {
				BundleInfo bInfo = (BundleInfo) ite.next();
				state.uninstallBundle(bInfo);
			}

		state.resolve(true);

		return state.getExpectedState();
	}

	private boolean isExclusiveInstallation() {
		return Boolean.parseBoolean(context.getProperty(SimpleConfiguratorConstants.PROP_KEY_EXCLUSIVE_INSTALLATION));
	}
}