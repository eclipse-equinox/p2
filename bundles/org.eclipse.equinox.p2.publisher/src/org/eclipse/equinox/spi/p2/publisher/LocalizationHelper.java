/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.spi.p2.publisher;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;

/**
 * Helper functions supporting the processing of localized property files.
 */
public final class LocalizationHelper {

	private static final String PROPERTIES_FILE_EXTENSION = ".properties"; //$NON-NLS-1$
	private static final Locale DEFAULT_LOCALE = new Locale("df", "LT"); //$NON-NLS-1$//$NON-NLS-2$

	// Extract the locale string from the properties file with the given filename
	// where the locale string follows the given prefix. For example, return "zh_HK"
	// from filename == "plugin_zh_HK.properties" and prefix == "plugin".
	static public String getLocaleString(String filename, String prefix) {
		String localeString = null;
		if (filename.startsWith(prefix) && filename.endsWith(PROPERTIES_FILE_EXTENSION)) {
			if (filename.length() > prefix.length() + PROPERTIES_FILE_EXTENSION.length()) {
				localeString = filename.substring(prefix.length() + 1,
						filename.length() - PROPERTIES_FILE_EXTENSION.length());
			} else {
				localeString = ""; //$NON-NLS-1$
			}
		}
		return localeString;
	}

	// Get the locale corresponding to the given locale string
	static public Locale getLocale(String localeString) {
		Locale locale = DEFAULT_LOCALE;
		if (localeString.length() == 5 && localeString.indexOf('_') == 2) {
			locale = new Locale(localeString.substring(0, 2), localeString.substring(3, 5));
		} else if (localeString.length() == 2) {
			locale = new Locale(localeString.substring(0, 2));
		}
		return locale;
	}

	// For the given root directory and path to localization files within that
	// directory
	// get a map from locale to property set for the localization property files.
	public static Map<Locale, Map<String, String>> getDirPropertyLocalizations(File root, String localizationPath,
			Locale defaultLocale, String[] propertyKeys) {
		File fullPath = new File(root, localizationPath);
		File localizationDir = fullPath.getParentFile();
		final String localizationFile = fullPath.getName();
		String[] localizationFiles = LocalizationHelper.getLocalizationFiles(localizationDir, localizationFile);

		HashMap<Locale, Map<String, String>> localizations = null;

		if (localizationFiles != null && localizationFiles.length > 0) {
			localizations = new HashMap<>(localizationFiles.length);
			for (String nextFile : localizationFiles) {
				Locale nextLocale = getLocale(LocalizationHelper.getLocaleString(nextFile, localizationFile));

				try {
					Map<String, String> properties = loadProperties(localizationDir, nextFile);
					Map<String, String> localizedStrings = getLocalizedProperties(propertyKeys, properties);
					if (localizedStrings.size() > 0) {
						localizations.put(nextLocale, localizedStrings);
						if (DEFAULT_LOCALE.equals(nextLocale) && defaultLocale != null) {
							localizations.put(nextLocale, localizedStrings);
						}
					}
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}

		return localizations;
	}

	public static Map<Locale, Map<String, String>> getJarPropertyLocalizations(File root, String localizationPath,
			Locale defaultLocale, String[] propertyKeys) {
		Map<Locale, Map<String, String>> localizations = new HashMap<>(4);
		try (ZipFile jarFile = new ZipFile(root, ZipFile.OPEN_READ)) {
			for (Enumeration<? extends ZipEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				ZipEntry nextEntry = entries.nextElement();
				String nextName = nextEntry.getName();
				String localeString = LocalizationHelper.getLocaleString(nextName, localizationPath);

				if (!nextEntry.isDirectory() && localeString != null) {
					Locale nextLocale = LocalizationHelper.getLocale(localeString);
					try (InputStream stream = jarFile.getInputStream(nextEntry)) {
						Map<String, String> properties = CollectionUtils.loadProperties(stream);
						Map<String, String> localizedStrings = LocalizationHelper.getLocalizedProperties(propertyKeys,
								properties);
						if (localizedStrings.size() > 0) {
							localizations.put(nextLocale, localizedStrings);
							if (DEFAULT_LOCALE.equals(nextLocale) && defaultLocale != null) {
								localizations.put(nextLocale, localizedStrings);
							}
						}
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return localizations;
	}

	// Load a property set from given root and file with the given name
	private static Map<String, String> loadProperties(File root, String propertyFilename) throws IOException {
		InputStream propertyStream = null;
		try {
			try {
				if (root.isDirectory())
					propertyStream = new FileInputStream(new File(root, propertyFilename));
				else {
					URLConnection connection = new URL("jar:" + root.toURL().toExternalForm() + "!/" + propertyFilename) //$NON-NLS-1$ //$NON-NLS-2$
							.openConnection();
					connection.setUseCaches(false);
					propertyStream = connection.getInputStream();
				}
			} catch (FileNotFoundException e) {
				// if there is no messages file then just return;
				return Collections.emptyMap();
			}
			return CollectionUtils.loadProperties(propertyStream);
		} finally {
			if (propertyStream != null)
				propertyStream.close();
		}
	}

	// Given a list of keys and the corresponding localized property set,
	// return a new property set with those keys and the localized values.
	static public Map<String, String> getLocalizedProperties(String[] propertyKeys, Map<String, String> properties) {
		Map<String, String> localizedProperties = new HashMap<>();
		for (String key : propertyKeys) {
			if (key != null) {
				String localizedValue = properties.get(key);
				if (localizedValue != null)
					localizedProperties.put(key, localizedValue);
			}
		}
		return localizedProperties;
	}

	public static String[] getLocalizationFiles(File localizationDir, final String filenamePrefix) {
		return localizationDir
				.list((directory, filename) -> (getLocaleString(filename, filenamePrefix) != null ? true : false));
	}

	private LocalizationHelper() {
		//
	}

}
