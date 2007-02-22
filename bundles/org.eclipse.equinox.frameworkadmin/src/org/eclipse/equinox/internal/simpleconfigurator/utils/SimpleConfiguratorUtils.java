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
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.osgi.framework.*;
import org.osgi.service.startlevel.StartLevel;

public class SimpleConfiguratorUtils {

	final static boolean DEBUG = false;

	public static final String SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME = "org.eclipse.equinox.simpleconfigurator";

	public static BundleInfo getBundleInfoFromBundle(BundleContext context, StartLevel startLevelService, Bundle bundle) {
		String symbolicName = bundle.getSymbolicName();
		Dictionary manifest = context.getBundle().getHeaders();
		String versionSt = (String) manifest.get(Constants.BUNDLE_VERSION);

		BundleInfo bInfo = new BundleInfo(symbolicName, versionSt, bundle.getLocation(), startLevelService.getBundleStartLevel(bundle), startLevelService.isBundlePersistentlyStarted(bundle));
		return bInfo;
	}

	public static String getBundleStateString(Bundle bundle) {
		StringBuffer sb = new StringBuffer();
		sb.append(bundle.getLocation());
		sb.append(" : ");
		switch (bundle.getState()) {
			case Bundle.INSTALLED :
				sb.append("INSTALLED");
				break;
			case Bundle.ACTIVE :
				sb.append("ACTIVE");
				break;
			case Bundle.RESOLVED :
				sb.append("RESOLVED");
				break;
			case Bundle.STARTING :
				sb.append("STARTING");
				break;
			case Bundle.STOPPING :
				sb.append("STOPPING");
				break;
			case Bundle.UNINSTALLED :
				sb.append("UNINSTALLED");
				break;
		}
		return sb.toString();
	}

	//	public static BundleInfo[] mergeCurrentState(BundleContext context, StartLevel startLevelService, List bundleInfoList) throws FwLauncherException {
	//		//		if (this.configApplier == null)
	//		//			configApplier = new ConfigApplier(context, this);
	//		
	//		List currentList = getInstalledBundleInfosList(context,startLevelService);
	//
	//		return mergeCurrentState(bundleInfoList, currentList);
	//	}

	public static List getInstalledBundleInfosList(BundleContext context, StartLevel startLevelService) {
		Bundle[] bundles = context.getBundles();
		List currentList = new LinkedList();

		for (int i = 0; i < bundles.length; i++) {
			BundleInfo bInfo = getBundleInfoFromBundle(context, startLevelService, bundles[i]);
			currentList.add(bInfo);
		}
		return currentList;
	}

	public static String getListSt(List list) {
		StringBuffer sb = new StringBuffer();
		for (Iterator ite2 = list.iterator(); ite2.hasNext();) {
			BundleInfo bInfo = (BundleInfo) ite2.next();
			sb.append(bInfo.toString() + "\n");
		}
		return sb.toString();
	}

	public static boolean isSystemBundleFragment(BundleContext context, Bundle bundle) {
		//Bundle[] bundles = context.getBundles();
		String symbolicNameSystem = context.getBundle(0).getSymbolicName();
		String fragmentHost = (String) bundle.getHeaders().get(Constants.FRAGMENT_HOST);
		if (fragmentHost != null) {
			String symbolic = fragmentHost.substring(0, fragmentHost.indexOf(";")).trim();
			if (symbolic.equals(symbolicNameSystem))
				return true;
			if (symbolic.equals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME))
				return true;
		}
		return false;
	}

	public static BundleInfo[] mergeState(List addedBundleInfoList, List currentBundleInfoList) {
		for (Iterator ite = addedBundleInfoList.iterator(); ite.hasNext();) {
			boolean duplicated = false;
			BundleInfo bInfo = (BundleInfo) ite.next();
			for (Iterator currentIte = currentBundleInfoList.iterator(); currentIte.hasNext();) {
				BundleInfo bInfoCurrent = (BundleInfo) currentIte.next();
				// int sl = startLevelService.getInitialBundleStartLevel();
				// if (bInfo.getStartLevel() != BundleInfo.NO_LEVEL)
				// sl = bInfo.getStartLevel();
				if (bInfoCurrent.getLocation().equals(bInfo.getLocation())) {
					bInfoCurrent.setStartLevel(bInfo.getStartLevel());
					bInfoCurrent.setMarkedAsStarted(bInfo.isMarkedAsStarted());
					duplicated = true;
					break;
				}
			}
			if (!duplicated)
				currentBundleInfoList.add(bInfo);
		}
		return Utils.getBundleInfosFromList(currentBundleInfoList);
	}

	public static List readConfiguration(URL url) throws IOException {
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
				URL baseUrl = new URL(url, "./");
				while ((line = r.readLine()) != null) {
					if (line.startsWith("#"))
						continue;
					line = line.trim();// symbolicName,version,location,startlevel,expectedState
					if (line.length() == 0)
						continue;

					// (expectedState is an integer).
					//System.out.println("line=" + line);
					if (line.startsWith(SimpleConfiguratorConstants.PARAMETER_BASEURL + "=")) {
						String baseUrlSt = line.substring((SimpleConfiguratorConstants.PARAMETER_BASEURL + "=").length());
						if (!baseUrlSt.endsWith("/"))
							baseUrlSt += "/";
						baseUrl = new URL(url, baseUrlSt);
						//						if (DEBUG)
						//							System.out.println("baseUrl=" + baseUrl);
						continue;
					}
					StringTokenizer tok = new StringTokenizer(line, ",", true);
					String symbolicName = tok.nextToken();
					if (symbolicName.equals(","))
						symbolicName = null;
					else
						tok.nextToken(); // ,

					String version = tok.nextToken();
					if (version.equals(","))
						version = null;
					else
						tok.nextToken(); // ,

					String urlSt = tok.nextToken();
					if (urlSt.equals(",")) {
						if (symbolicName != null && version != null)
							urlSt = symbolicName + "_" + version + ".jar";
						else
							urlSt = null;
					} else
						tok.nextToken(); // ,
					try {
						new URL(urlSt);
						//						if (DEBUG)
						//							System.out.println("1 urlSt=" + urlSt);
					} catch (MalformedURLException e) {
						urlSt = Utils.getUrlInFull(urlSt, baseUrl).toExternalForm();
						//						if (DEBUG)
						//							System.out.println("2 urlSt=" + urlSt);
					}

					int sl = Integer.parseInt(tok.nextToken().trim());
					tok.nextToken(); // ,
					boolean markedAsStarted = Boolean.parseBoolean(tok.nextToken());
					// URL urlBundle = null;
					// try {
					// urlBundle = new URL(urlSt);
					// } catch (MalformedURLException e) {
					// urlBundle = Utils.getFullUrl(urlSt, baseUrl);
					// }

					BundleInfo bInfo = new BundleInfo(symbolicName, version, urlSt, sl, markedAsStarted);
					bundles.add(bInfo);
					// System.out.println("tail line=" + line);
				}
			} finally {
				try {
					r.close();
				} catch (IOException ex) {
					// ignore
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			// TODO log something
			// bundleInfos = NULL_BUNDLEINFOS;
		}
		return bundles;
		// bundleInfos = (BundleInfo[]) bundles.toArray(new
		// BundleInfo[bundles.size()]);
	}

}
