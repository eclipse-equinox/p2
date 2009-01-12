/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.manipulator;

import org.eclipse.equinox.internal.provisional.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.BundleInfo;
import org.osgi.framework.Version;

public class SimpleConfiguratorManipulatorUtils {

	private static final String VERSION_PREFIX = "#version=";
	private static final String VERSION_1 = "1";
	private static final Version OLD_STYLE_SIMPLE_CONFIGURATOR_VERSION = new Version("1.0.100.v20081206");

	public static void writeConfiguration(BundleInfo[] simpleInfos, File outputFile) throws IOException {

		// if empty remove the configuration file
		if (simpleInfos == null || simpleInfos.length == 0) {
			if (outputFile.exists()) {
				outputFile.delete();
			}
			File parentDir = outputFile.getParentFile();
			if (parentDir.exists()) {
				parentDir.delete();
			}
			return;
		}

		// sort by symbolic name
		Arrays.sort(simpleInfos, new Comparator() {
			public int compare(Object o1, Object o2) {
				if (o1 instanceof BundleInfo && o2 instanceof BundleInfo) {
					return ((BundleInfo) o1).getSymbolicName().compareTo(((BundleInfo) o2).getSymbolicName());
				}
				return 0;
			}
		});

		Utils.createParentDir(outputFile);
		BufferedWriter writer = null;
		IOException caughtException = null;
		boolean oldStyle = false;
		for (int i = 0; i < simpleInfos.length; i++) {
			if (SimpleConfiguratorManipulator.SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME.equals(simpleInfos[i].getSymbolicName())) {
				Version version = new Version(simpleInfos[i].getVersion());
				if (version.compareTo(OLD_STYLE_SIMPLE_CONFIGURATOR_VERSION) < 0)
					oldStyle = true;
				break;
			}
		}

		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			// version line
			writer.write(createVersionLine());
			writer.newLine();

			// bundle info lines
			for (int i = 0; i < simpleInfos.length; i++) {
				writer.write(createBundleInfoLine(simpleInfos[i], oldStyle));
				writer.newLine();
			}
		} catch (IOException e) {
			caughtException = e;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// we want to avoid over-writing the original exception
					if (caughtException != null)
						caughtException = e;
				}
			}
		}
		if (caughtException != null)
			throw caughtException;
	}

	public static String createVersionLine() {
		return VERSION_PREFIX + VERSION_1;
	}

	public static String createBundleInfoLine(BundleInfo bundleInfo, boolean oldStyle) throws IOException {
		// symbolicName,version,location,startLevel,markedAsStarted
		StringBuffer buffer = new StringBuffer();
		buffer.append(bundleInfo.getSymbolicName());
		buffer.append(',');
		buffer.append(bundleInfo.getVersion());
		buffer.append(',');
		buffer.append(createBundleLocation(bundleInfo.getLocation(), oldStyle));
		buffer.append(',');
		buffer.append(bundleInfo.getStartLevel());
		buffer.append(',');
		buffer.append(bundleInfo.isMarkedAsStarted());
		return buffer.toString();
	}

	public static String createBundleLocation(URI location, boolean oldStyle) {
		if (oldStyle) {
			String scheme = location.getScheme();
			if (scheme == null)
				scheme = "file";
			return scheme + ":" + location.getSchemeSpecificPart();
		}

		String result = location.toString();
		int commaIndex = result.indexOf(',');
		while (commaIndex != -1) {
			result = result.substring(0, commaIndex) + "%2C" + result.substring(commaIndex + 1);
			commaIndex = result.indexOf(',');
		}
		return result;
	}

}
