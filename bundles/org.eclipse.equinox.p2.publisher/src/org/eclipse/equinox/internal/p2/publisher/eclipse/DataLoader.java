/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxFwConfigFileParser;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxManipulatorImpl;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class DataLoader {

	private final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + "=" + FrameworkAdmin.class.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private final static String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)"; //$NON-NLS-1$ //$NON-NLS-2$
	//String filterFwVersion = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_VERSION + "=" + props.getProperty("equinox.fw.version") + ")";
	private final static String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)"; //$NON-NLS-1$ //$NON-NLS-2$
	//String filterLauncherVersion = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_VERSION + "=" + props.getProperty("equinox.launcher.version") + ")";
	private final static String frameworkAdminFillter = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ")"; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL = "org.eclipse.equinox.simpleconfigurator.configUrl"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_MANIPULATOR = "org.eclipse.equinox.simpleconfigurator.manipulator"; //$NON-NLS-1$
	private static final String ORG_ECLIPSE_EQUINOX_FRAMEWORKADMIN_EQUINOX = "org.eclipse.equinox.frameworkadmin.equinox"; //$NON-NLS-1$
	private static final String PARAMETER_BASEURL = "org.eclipse.equinox.simpleconfigurator.baseUrl"; //$NON-NLS-1$

	private Manipulator manipulator;
	private File configurationLocation;

	public static BundleInfo[] readConfiguration(URL url) throws IOException {
		List bundles = new ArrayList();
		try {
			// System.out.println("readConfiguration(URL url):url()=" + url);
			// URL configFileUrl = getConfigFileUrl();
			// URL configFileUrl = Utils.getUrl("file",null,
			// inputFile.getAbsolutePath());
			BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
			// BufferedReader r = new BufferedReader(new FileReader(inputFile));

			String line;
			try {
				URL baseUrl = new URL(url, "./"); //$NON-NLS-1$
				while ((line = r.readLine()) != null) {
					if (line.startsWith("#")) //$NON-NLS-1$
						continue;
					line = line.trim();// symbolicName,version,location,startlevel,expectedState
					if (line.length() == 0)
						continue;

					// (expectedState is an integer).
					if (line.startsWith(PARAMETER_BASEURL + "=")) { //$NON-NLS-1$
						String baseUrlSt = line.substring((PARAMETER_BASEURL + "=").length()); //$NON-NLS-1$
						if (!baseUrlSt.endsWith("/")) //$NON-NLS-1$
							baseUrlSt += "/"; //$NON-NLS-1$
						baseUrl = new URL(url, baseUrlSt);
						continue;
					}
					StringTokenizer tok = new StringTokenizer(line, ",", true); //$NON-NLS-1$
					String symbolicName = tok.nextToken();
					if (symbolicName.equals(",")) //$NON-NLS-1$
						symbolicName = null;
					else
						tok.nextToken(); // ,

					String version = tok.nextToken();
					if (version.equals(",")) //$NON-NLS-1$
						version = null;
					else
						tok.nextToken(); // ,

					String urlSt = tok.nextToken();
					if (urlSt.equals(",")) { //$NON-NLS-1$
						if (symbolicName != null && version != null)
							urlSt = symbolicName + "_" + version + ".jar"; //$NON-NLS-1$ //$NON-NLS-2$
						else
							urlSt = null;
					} else
						tok.nextToken(); // ,
					try {
						new URL(urlSt);
					} catch (MalformedURLException e) {
						urlSt = Utils.getUrlInFull(urlSt, baseUrl).toExternalForm();
					}

					int sl = Integer.parseInt(tok.nextToken().trim());
					tok.nextToken(); // ,
					boolean markedAsStarted = Boolean.valueOf(tok.nextToken()).booleanValue();

					BundleInfo bInfo;
					try {
						bInfo = new BundleInfo(symbolicName, version, new URI(urlSt), sl, markedAsStarted);

						bundles.add(bInfo);
					} catch (URISyntaxException e) {
						e.printStackTrace();
						throw new IllegalStateException("Error coverting url based string to uri: " + e.getMessage());
					}
				}
			} finally {
				try {
					r.close();
				} catch (IOException ex) {
					// ignore
				}
			}
		} catch (MalformedURLException e) {
			// TODO log something
		}
		return (BundleInfo[]) bundles.toArray(new BundleInfo[bundles.size()]);
	}

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
				BundleInfo[] bundleInfos = readConfiguration(new URL(value));
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
		FrameworkAdmin frameworkAdmin = (FrameworkAdmin) ServiceHelper.getService(Activator.getContext(), FrameworkAdmin.class.getName(), frameworkAdminFillter);
		if (frameworkAdmin == null) {
			startBundle(ORG_ECLIPSE_EQUINOX_FRAMEWORKADMIN_EQUINOX);
			startBundle(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_MANIPULATOR);
			frameworkAdmin = (FrameworkAdmin) ServiceHelper.getService(Activator.getContext(), FrameworkAdmin.class.getName(), frameworkAdminFillter);
		}
		return frameworkAdmin;
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
