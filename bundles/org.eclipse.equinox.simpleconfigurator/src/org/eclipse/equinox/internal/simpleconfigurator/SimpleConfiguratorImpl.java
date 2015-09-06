/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *      Ericsson AB (Pascal Rapicault) - Bug 397216 -[Shared] Better shared configuration change discovery
 *      Red Hat, Inc (Krzysztof Daniel) - Bug 421935: Extend simpleconfigurator to
 * read .info files from many locations 
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.simpleconfigurator.utils.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/*
 * SimpleConfigurator provides ways to install bundles listed in a file
 * accessible by the specified URL and expect states for it in advance without
 * actual application.
 * 
 * In every methods of SimpleConfiguration object,
 * 
 * 1. A value will be gotten by @{link BundleContext#getProperty(key) with
 * {@link SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION} as a key.
 * 2. If it equals "true", it will do exclusive installation, which means that
 * the bundles will not be listed in the specified url but installed at the time
 * of the method call except SystemBundle will be uninstalled. Otherwise, no
 * uninstallation will not be done.
 */
public class SimpleConfiguratorImpl implements Configurator {

	private static URL configurationURL = null;
	private static Object configurationLock = new Object();

	private BundleContext context;
	private ConfigApplier configApplier;
	private Bundle bundle;

	//for change detection in the base when running in shared install mode
	private static final long NO_TIMESTAMP = -1;
	public static final String BASE_TIMESTAMP_FILE_BUNDLESINFO = ".baseBundlesInfoTimestamp"; //$NON-NLS-1$
	public static final String KEY_BUNDLESINFO_TIMESTAMP = "bundlesInfoTimestamp";
	public static final String KEY_EXT_TIMESTAMP = "extTimestamp";
	public static final String PROP_IGNORE_USER_CONFIGURATION = "eclipse.ignoreUserConfiguration"; //$NON-NLS-1$

	public SimpleConfiguratorImpl(BundleContext context, Bundle bundle) {
		this.context = context;
		this.bundle = bundle;
	}

	public URL getConfigurationURL() throws IOException {
		String specifiedURL = context.getProperty(SimpleConfiguratorConstants.PROP_KEY_CONFIGURL);
		if (specifiedURL == null)
			specifiedURL = "file:" + SimpleConfiguratorConstants.CONFIGURATOR_FOLDER + "/" + SimpleConfiguratorConstants.CONFIG_LIST;

		try {
			//If it is not a file URL use it as is
			if (!specifiedURL.startsWith("file:"))
				return new URL(specifiedURL);
		} catch (MalformedURLException e) {
			return null;
		}

		try {
			// if it is an absolute file URL, use it as is
			boolean done = false;
			URL url = null;
			String file = specifiedURL;
			while (!done) {
				// TODO what is this while loop for?  nested file:file:file: urls?
				try {
					url = Utils.buildURL(file);
					file = url.getFile();
				} catch (java.net.MalformedURLException e) {
					done = true;
				}
			}
			if (url != null && new File(url.getFile()).isAbsolute())
				return url;

			//if it is an relative file URL, then resolve it against the configuration area
			// TODO Support relative file URLs when not on Equinox
			URL[] configURL = EquinoxUtils.getConfigAreaURL(context);

			URL result = chooseConfigurationURL(url, configURL);
			if (result != null) {
				return result;
			}
		} catch (MalformedURLException e) {
			return null;
		}

		//Last resort
		try {
			return Utils.buildURL(specifiedURL);
		} catch (MalformedURLException e) {
			//Ignore
		}

		return null;
	}

	/**
	 * This method is public for testing purposes only.
	 * @param relativeURL - a relative URL of the configuration
	 * @param configURL - an array of parent config URLs to which relativeURL can be appended. 
	 */
	public URL chooseConfigurationURL(URL relativeURL, URL[] configURL) throws MalformedURLException {
		if (configURL != null) {
			File userConfig = new File(configURL[0].getFile(), relativeURL.getFile());
			if (configURL.length == 1)
				return userConfig.exists() ? userConfig.toURL() : null;

			File sharedConfig = new File(configURL[1].getFile(), relativeURL.getFile());
			if (!userConfig.exists())
				return sharedConfig.exists() ? sharedConfig.toURL() : null;

			if (!sharedConfig.exists())
				return userConfig.toURL();

			if (Boolean.TRUE.toString().equals(System.getProperty(PROP_IGNORE_USER_CONFIGURATION)))
				return sharedConfig.toURL();

			long[] sharedBundlesInfoTimestamp = getCurrentBundlesInfoBaseTimestamp(sharedConfig);
			long[] lastKnownBaseTimestamp = getLastKnownBundlesInfoBaseTimestamp(userConfig.getParentFile());

			if ((lastKnownBaseTimestamp[0] == sharedBundlesInfoTimestamp[0] && lastKnownBaseTimestamp[1] == sharedBundlesInfoTimestamp[1]) || lastKnownBaseTimestamp[0] == NO_TIMESTAMP) {
				return userConfig.toURL();
			} else {
				System.setProperty(PROP_IGNORE_USER_CONFIGURATION, Boolean.TRUE.toString());
				return sharedConfig.toURL();
			}
		}
		return null;
	}

	private long[] getLastKnownBundlesInfoBaseTimestamp(File configFolder) {
		long[] result = new long[] {NO_TIMESTAMP, NO_TIMESTAMP};
		File storedSharedTimestamp = new File(configFolder, BASE_TIMESTAMP_FILE_BUNDLESINFO);
		if (!storedSharedTimestamp.exists())
			return result;

		Properties p = new Properties();
		InputStream is = null;
		try {
			try {
				is = new BufferedInputStream(new FileInputStream(storedSharedTimestamp));
				p.load(is);
				if (p.get(KEY_BUNDLESINFO_TIMESTAMP) != null) {
					result[0] = Long.valueOf((String) p.get(KEY_BUNDLESINFO_TIMESTAMP)).longValue();
				}
				if (p.get(KEY_EXT_TIMESTAMP) != null) {
					result[1] = Long.valueOf((String) p.get(KEY_EXT_TIMESTAMP)).longValue();
				}
			} finally {
				is.close();
			}
		} catch (IOException e) {
			return result;
		}
		return result;
	}

	public static long[] getCurrentBundlesInfoBaseTimestamp(File sharedBundlesInfo) {
		if (!sharedBundlesInfo.exists())
			return new long[] {NO_TIMESTAMP, NO_TIMESTAMP};
		long lastModified = sharedBundlesInfo.lastModified();
		long extLastModified = SimpleConfiguratorUtils.getExtendedTimeStamp();
		return new long[] {lastModified, extLastModified};
	}

	public void applyConfiguration(URL url) throws IOException {
		synchronized (configurationLock) {
			if (Activator.DEBUG)
				System.out.println("applyConfiguration() URL=" + url);
			if (url == null)
				return;
			configurationURL = url;

			if (this.configApplier == null)
				configApplier = new ConfigApplier(context, bundle);
			configApplier.install(url, isExclusiveInstallation());
		}
	}

	private boolean isExclusiveInstallation() {
		String value = context.getProperty(SimpleConfiguratorConstants.PROP_KEY_EXCLUSIVE_INSTALLATION);
		if (value == null || value.trim().length() == 0)
			value = "true";
		return Boolean.parseBoolean(value);
	}

	public void applyConfiguration() throws IOException {
		synchronized (configurationLock) {
			configurationURL = getConfigurationURL();
			applyConfiguration(configurationURL);
		}
	}

	public URL getUrlInUse() {
		synchronized (configurationLock) {
			return configurationURL;
		}
	}
}
