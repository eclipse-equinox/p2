/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/*
 * This class was copied from org.eclipse.equinox.internal.frameworkadmin.utils
 * package of org.eclipse.equinox.frameworkadmin plugin on March 3 2007.
 * 
 * The reason why it was copied is to make simpleconfigurator dependent on any
 * bundles(org.eclipse.equinox.framework).
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

	public static BundleInfo[] getBundleInfosFromList(List<BundleInfo> list) {
		if (list == null)
			return new BundleInfo[0];
		BundleInfo[] ret = new BundleInfo[list.size()];
		list.toArray(ret);
		return ret;
	}

	public static URL getUrl(String protocol, String host, String file) throws MalformedURLException {// throws ManipulatorException {
		file = Utils.replaceAll(file, File.separator, "/");
		return new URL(protocol, host, file);
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

	public static URL buildURL(String spec) throws MalformedURLException {
		if (spec == null)
			throw new NullPointerException("URL spec is null."); //$NON-NLS-1$
		// Construct the URL carefully so as to preserve UNC paths etc.
		if (spec.startsWith("file:")) { //$NON-NLS-1$
			// need to do this for UNC paths
			File file = new File(spec.substring(5));
			if (file.isAbsolute())
				return file.toURL();
		}
		return new URL(spec);
	}
}
