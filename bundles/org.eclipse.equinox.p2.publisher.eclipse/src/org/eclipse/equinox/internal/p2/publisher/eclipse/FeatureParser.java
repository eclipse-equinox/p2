/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - split into FeatureParser and FeatureManifestParser
 *     SAP AG - consolidation of publishers for PDE formats
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.spi.p2.publisher.LocalizationHelper;
import org.eclipse.pde.internal.publishing.Activator;
import org.xml.sax.SAXException;

/**
 * The publisher feature parser. This class parses a feature either in jar or folder
 * form. Feature localization data (feature.properties) is also processed here.
 */
public class FeatureParser {

	private final FeatureManifestParser parser = new FeatureManifestParser();

	/**
	 * Parses the specified location and constructs a feature. The given location 
	 * should be either the location of the feature JAR or the directory containing
	 * the feature.
	 * 
	 * @param location the location of the feature to parse.  
	 */
	public Feature parse(File location) {
		if (!location.exists())
			return null;

		Feature feature = null;
		if (location.isDirectory()) {
			//skip directories that don't contain a feature.xml file
			File file = new File(location, "feature.xml"); //$NON-NLS-1$
			InputStream input = null;
			try {
				input = new BufferedInputStream(new FileInputStream(file));
				feature = parser.parse(input, toURL(location));
				if (feature != null) {
					List<String> messageKeys = parser.getMessageKeys();
					String[] keyStrings = messageKeys.toArray(new String[messageKeys.size()]);
					feature.setLocalizations(LocalizationHelper.getDirPropertyLocalizations(location, "feature", null, keyStrings)); //$NON-NLS-1$
				}
			} catch (FileNotFoundException e) {
				return null;
			} catch (SAXException e) {
				logWarning(location, e);
			} catch (IOException e) {
				logWarning(location, e);
			} finally {
				if (input != null)
					try {
						input.close();
					} catch (IOException e) {
						//
					}
			}
		} else if (location.getName().endsWith(".jar")) { //$NON-NLS-1$
			JarFile jar = null;
			try {
				jar = new JarFile(location);
				JarEntry entry = jar.getJarEntry("feature.xml"); //$NON-NLS-1$
				if (entry == null)
					return null;

				InputStream input = new BufferedInputStream(jar.getInputStream(entry));
				feature = parser.parse(input, toURL(location));
				if (feature != null) {
					List<String> messageKeys = parser.getMessageKeys();
					String[] keyStrings = messageKeys.toArray(new String[messageKeys.size()]);
					feature.setLocalizations(LocalizationHelper.getJarPropertyLocalizations(location, "feature", null, keyStrings)); //$NON-NLS-1$
				}
			} catch (SAXException e) {
				logWarning(location, e);
			} catch (IOException e) {
				logWarning(location, e);
			} catch (SecurityException e) {
				logWarning(location, e);
			} finally {
				try {
					if (jar != null)
						jar.close();
				} catch (IOException e) {
					//
				}
			}
		}
		return feature;
	}

	private static void logWarning(File location, Exception exception) {
		LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Exception parsing feature: " + location.getAbsolutePath(), exception)); //$NON-NLS-1$
	}

	private static URL toURL(File location) {
		try {
			return location.toURI().toURL();
		} catch (MalformedURLException e) {
			// not known to happen
			throw new RuntimeException(e);
		}
	}
}
