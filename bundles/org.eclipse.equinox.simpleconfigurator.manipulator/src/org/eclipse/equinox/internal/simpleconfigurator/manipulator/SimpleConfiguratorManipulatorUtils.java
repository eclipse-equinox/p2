/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.manipulator;

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import org.eclipse.equinox.internal.frameworkadmin.equinox.Messages;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.BundleInfo;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.osgi.framework.Version;

public class SimpleConfiguratorManipulatorUtils {

	private static final String VERSION_PREFIX = "#version="; //$NON-NLS-1$
	private static final String VERSION_1 = "1"; //$NON-NLS-1$
	private static final Version OLD_STYLE_SIMPLE_CONFIGURATOR_VERSION = new Version("1.0.100.v20081206"); //$NON-NLS-1$
	private static final Version DEFAULT_ENCODING_CONFIGURATOR_VERSION = new Version("1.0.200.v20100503"); //$NON-NLS-1$ 3.6, after bug 289644

	public static void writeConfiguration(BundleInfo[] simpleInfos, File outputFile) throws IOException {
		if (!Utils.createParentDir(outputFile)) {
			throw new IllegalStateException(Messages.exception_failedToCreateDir);
		}

		IOException caughtException = null;
		OutputStream stream = null;
		try {
			stream = new FileOutputStream(outputFile);
			writeConfiguration(simpleInfos, stream);
		} catch (IOException e) {
			caughtException = e;
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				// we want to avoid over-writing the original exception
				if (caughtException != null)
					caughtException = e;
			}
		}
		if (caughtException != null)
			throw caughtException;
	}

	/**
	 * The output stream is left open
	 * @param simpleInfos
	 * @param stream
	 * @throws IOException
	 */
	public static void writeConfiguration(BundleInfo[] simpleInfos, OutputStream stream) throws IOException {
		// sort by symbolic name
		Arrays.sort(simpleInfos, new Comparator<BundleInfo>() {
			public int compare(BundleInfo b1, BundleInfo b2) {
				int bsnComparison = b1.getSymbolicName().compareTo(b2.getSymbolicName());
				if (bsnComparison != 0)
					return bsnComparison;
				// prefer latest version, see https://bugs.eclipse.org/363590
				return new Version(b2.getVersion()).compareTo(new Version(b1.getVersion()));
			}
		});

		BufferedWriter writer = null;
		boolean oldStyle = false;
		boolean utf8 = true;
		for (int i = 0; i < simpleInfos.length; i++) {
			if (SimpleConfiguratorManipulator.SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME.equals(simpleInfos[i].getSymbolicName())) {
				Version version = new Version(simpleInfos[i].getVersion());
				if (version.compareTo(OLD_STYLE_SIMPLE_CONFIGURATOR_VERSION) < 0)
					oldStyle = true;
				if (version.compareTo(DEFAULT_ENCODING_CONFIGURATOR_VERSION) < 0)
					utf8 = false;
				break;
			}
		}

		if (utf8) {
			writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8")); //$NON-NLS-1$
			//encoding is expected to be the first line
			writer.write(SimpleConfiguratorUtils.ENCODING_UTF8);
			writer.newLine();
		} else {
			writer = new BufferedWriter(new OutputStreamWriter(stream));
		}
		writer.write(createVersionLine());
		writer.newLine();

		// bundle info lines
		for (int i = 0; i < simpleInfos.length; i++) {
			writer.write(createBundleInfoLine(simpleInfos[i], oldStyle));
			writer.newLine();
		}
		writer.flush();
	}

	public static String createVersionLine() {
		return VERSION_PREFIX + VERSION_1;
	}

	public static String createBundleInfoLine(BundleInfo bundleInfo, boolean oldStyle) {
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
				scheme = "file"; //$NON-NLS-1$
			return scheme + ':' + location.getSchemeSpecificPart();
		}

		//encode comma characters because it is used as the segment delimiter in the bundle info file
		String result = location.toString();
		int commaIndex = result.indexOf(',');
		while (commaIndex != -1) {
			result = result.substring(0, commaIndex) + "%2C" + result.substring(commaIndex + 1); //$NON-NLS-1$
			commaIndex = result.indexOf(',');
		}
		return result;
	}

}
