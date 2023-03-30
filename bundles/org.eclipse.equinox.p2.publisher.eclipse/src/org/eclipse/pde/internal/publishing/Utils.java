/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *      SAP AG - consolidation of publishers for PDE formats
 *******************************************************************************/
package org.eclipse.pde.internal.publishing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Enumeration;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

public final class Utils {

	static public void copy(File source, File destination) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(source));
			out = new BufferedOutputStream(new FileOutputStream(destination));
			final byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				bytesRead = in.read(buffer);
				if (bytesRead == -1)
					break;
				out.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				if (in != null)
					in.close();
			} finally {
				if (out != null)
					out.close();
			}
		}
	}

	public static boolean guessUnpack(BundleDescription bundle, String[] classpath) {
		if (bundle == null)
			return true;

		@SuppressWarnings("unchecked")
		Dictionary<String, String> properties = (Dictionary<String, String>) bundle.getUserObject();
		String shape = null;
		if (properties != null && (shape = properties.get(Constants.ECLIPSE_BUNDLE_SHAPE)) != null) {
			return shape.equals("dir"); //$NON-NLS-1$
		}

		// launcher fragments are a special case, they have no bundle-classpath
		if (bundle.getHost() != null && bundle.getName().startsWith(Constants.BUNDLE_EQUINOX_LAUNCHER))
			return true;

		if (new File(bundle.getLocation()).isFile())
			return false;

		if (classpath.length == 0)
			return false;

		for (String classpath1 : classpath) {
			if (classpath1.equals(".")) {
				return false;
			}
		}
		return true;
	}

	public static String[] getBundleClasspath(Dictionary<String, String> manifest) {
		String fullClasspath = getBundleManifestHeader(manifest, Constants.BUNDLE_CLASSPATH);
		String[] result = new String[0];
		try {
			if (fullClasspath != null) {
				ManifestElement[] classpathEntries;
				classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, fullClasspath);
				result = new String[classpathEntries.length];
				for (int i = 0; i < classpathEntries.length; i++) {
					result[i] = classpathEntries[i].getValue();
				}
			}
		} catch (BundleException e) {
			//Ignore
		}
		return result;
	}

	public static String getBundleManifestHeader(Dictionary<String, String> manifest, String header) {
		String value = manifest.get(header);
		if (value != null)
			return value;

		Enumeration<String> keys = manifest.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key.equalsIgnoreCase(header))
				return manifest.get(key);
		}
		return null;
	}
}
