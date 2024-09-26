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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

public final class Utils {

	static public void copy(File source, File destination) throws IOException {
		Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	public static boolean guessUnpack(BundleDescription bundle, List<String> classpath) {
		if (bundle == null) {
			return true;
		}
		@SuppressWarnings("unchecked")
		Dictionary<String, String> properties = (Dictionary<String, String>) bundle.getUserObject();
		String shape = null;
		if (properties != null && (shape = properties.get(Constants.ECLIPSE_BUNDLE_SHAPE)) != null) {
			return shape.equals("dir"); //$NON-NLS-1$
		}

		// launcher fragments are a special case, they have no bundle-classpath
		if (bundle.getHost() != null && bundle.getName().startsWith(Constants.BUNDLE_EQUINOX_LAUNCHER)) {
			return true;
		}
		if (new File(bundle.getLocation()).isFile()) {
			return false;
		}
		return !classpath.isEmpty() && classpath.stream().noneMatch("."::equals); //$NON-NLS-1$
	}

	public static List<String> getBundleClasspath(Dictionary<String, String> manifest) {
		String fullClasspath = getBundleManifestHeader(manifest, Constants.BUNDLE_CLASSPATH);
		try {
			if (fullClasspath != null) {
				return Arrays.stream(ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH,
						fullClasspath)).map(ManifestElement::getValue).toList();
			}
		} catch (BundleException e) {
			//Ignore
		}
		return List.of();
	}

	/**
	 * @param manifest manifest entries, or {@code null}
	 * @return requested header value, or {@code null}
	 */
	public static String getBundleManifestHeader(Dictionary<String, String> manifest, String header) {
		if (manifest == null) {
			return null;
		}
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
