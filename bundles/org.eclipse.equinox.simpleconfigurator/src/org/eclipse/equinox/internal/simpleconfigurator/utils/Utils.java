/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.equinox.internal.simpleconfigurator.Activator;

/**
 * This class was copied from 
 * org.eclipse.equinox.internal.frameworkadmin.utils package of
 * org.eclipse.equinox.frameworkadmin plugin on March 3 2007.
 * 
 * The reason why it was copied is to make simpleconfigurator dependent on 
 * any bundles(org.eclipse.equinox.framework).
 * 
 */

public class Utils {
	private final static String PATH_SEP = "/"; //$NON-NLS-1$

	public static URL checkFullUrl(URL url, String urlName) throws IllegalArgumentException {//throws ManipulatorException {
		if (url == null)
			throw new IllegalArgumentException(urlName + " is null");
		if (!url.getProtocol().endsWith("file"))
			return url;
		File file = new File(url.getFile());
		if (!file.isAbsolute())
			throw new IllegalArgumentException(urlName + "(" + url + ") does not have absolute path");
		if (file.getAbsolutePath().startsWith(PATH_SEP))
			return url;
		try {
			return getUrl("file", null, PATH_SEP + file.getAbsolutePath());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(urlName + "(" + "file:" + PATH_SEP + file.getAbsolutePath() + ") is not fully quallified");
		}
	}

	public static void deleteDir(File file) throws IOException {
		if (file.isFile()) {
			if (!file.delete())
				throw new IOException("Fail to delete File(" + file.getAbsolutePath() + ")");
			return;
		}
		File[] children = file.listFiles();
		for (int i = 0; i < children.length; i++) {
			deleteDir(children[i]);
		}
		if (!file.delete())
			throw new IOException("Fail to delete Dir(" + file.getAbsolutePath() + ")");
		return;
	}

	public static BundleInfo[] getBundleInfosFromList(List list) {
		if (list == null)
			return new BundleInfo[0];
		BundleInfo[] ret = new BundleInfo[list.size()];
		list.toArray(ret);
		return ret;
	}

	public static Dictionary getOSGiManifest(String location) {
		if (location.startsWith("file:") && !location.endsWith(".jar"))
			return basicLoadManifest(new File(location.substring("file:".length())));

		try {
			URL url = new URL("jar:" + location + "!/");
			JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
			Manifest manifest = jarConnection.getManifest();
			Attributes attributes = manifest.getMainAttributes();
			Hashtable table = new Hashtable();
			for (java.util.Iterator ite = attributes.keySet().iterator(); ite.hasNext();) {

				String key = ite.next().toString();
				// While table contains non OSGiManifest, it doesn't matter.
				table.put(key, attributes.getValue(key));
			}
			return table;
		} catch (MalformedURLException e1) {
			if (Activator.DEBUG) {
				System.err.println("location=" + location);
				e1.printStackTrace();
			}
		} catch (IOException e) {
			if (Activator.DEBUG) {
				System.err.println("location=" + location);
				e.printStackTrace();
			}
		}
		return null;
	}

	//Return a dictionary representing a manifest. The data may result from plugin.xml conversion  
	private static Dictionary basicLoadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		ZipFile jarFile = null;
		try {
			String fileExtention = bundleLocation.getName();
			fileExtention = fileExtention.substring(fileExtention.lastIndexOf('.') + 1);
			if ("jar".equalsIgnoreCase(fileExtention) && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				manifestStream = new BufferedInputStream(new FileInputStream(new File(bundleLocation, JarFile.MANIFEST_NAME)));
			}
		} catch (IOException e) {
			//ignore
		}
		Dictionary manifest = null;

		//It is not a manifest, but a plugin or a fragment

		if (manifestStream != null) {
			try {
				Manifest m = new Manifest(manifestStream);
				manifest = manifestToProperties(m.getMainAttributes());
			} catch (IOException ioe) {
				return null;
			} finally {
				try {
					manifestStream.close();
				} catch (IOException e1) {
					//Ignore
				}
				try {
					if (jarFile != null)
						jarFile.close();
				} catch (IOException e2) {
					//Ignore
				}
			}
		}
		return manifest;
	}

	private static Properties manifestToProperties(Attributes d) {
		Iterator iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}

	public static URL getUrl(String protocol, String host, String file) throws MalformedURLException {// throws ManipulatorException {
		file = Utils.replaceAll(file, File.separator, "/");
		return new URL(protocol, host, file);
	}

	public static URL getUrlInFull(String path, URL from) throws MalformedURLException {//throws ManipulatorException {
		Utils.checkFullUrl(from, "from");
		path = Utils.replaceAll(path, File.separator, "/");
		String fromSt = Utils.removeLastCh(from.toExternalForm(), '/');
		if (path.startsWith("/")) {
			String fileSt = from.getFile();
			return new URL(fromSt.substring(0, fromSt.lastIndexOf(fileSt) - 1) + path);
		}
		return new URL(fromSt + "/" + path);
	}

	public static String removeLastCh(String target, char ch) {
		while (target.charAt(target.length() - 1) == ch) {
			target = target.substring(0, target.length() - 1);
		}
		return target;
	}

	public static String replaceAll(String st, String oldSt, String newSt) {
		int index = -1;
		while ((index = st.indexOf(oldSt)) != -1) {
			st = st.substring(0, index) + newSt + st.substring(index + oldSt.length());
		}
		return st;
	}

	public static void log(int level, Object obj, String method, String message, Throwable e) {
		String msg = "";
		if (method == null) {
			if (obj != null)
				msg = "(" + obj.getClass().getName() + ")";
		} else if (obj == null)
			msg = "[" + method + "]" + message;
		else
			msg = "[" + method + "](" + obj.getClass().getName() + ")";
		msg += message;

//		if (LogService logService = Activator.getLogService();
//		if (logService != null) {
//			logService.log(level, msg, e);
//		} else {
		String levelSt = null;
		if (level == 1)
			levelSt = "DEBUG";
		else if (level == 2)
			levelSt = "INFO";
		else if (level == 3)
			levelSt = "WARNING";
		else if (level == 4) {
			levelSt = "ERROR";
//				useLog = true;
		}
//			if (useLog) {
		System.err.println("[" + levelSt + "]" + msg);
		if (e != null)
			e.printStackTrace();
//			}
	}
}