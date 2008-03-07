/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.generator.Messages;
import org.eclipse.equinox.internal.p2.metadata.repository.Activator;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;

public class BundleDescriptionFactory {
	static final String DIR = "dir"; //$NON-NLS-1$
	static final String JAR = "jar"; //$NON-NLS-1$
	private static final String FEATURE_FILENAME_DESCRIPTOR = "feature.xml"; //$NON-NLS-1$
	private static final String PLUGIN_FILENAME_DESCRIPTOR = "plugin.xml"; //$NON-NLS-1$
	private static final String FRAGMENT_FILENAME_DESCRIPTOR = "fragment.xml"; //$NON-NLS-1$

	static String BUNDLE_FILE_KEY = "eclipse.p2.bundle.format"; //$NON-NLS-1$

	//	static final String DEFAULT_BUNDLE_LOCALIZATION = "plugin"; //$NON-NLS-1$	
	//	static final String PROPERTIES_FILE_EXTENSION = ".properties"; //$NON-NLS-1$
	//	static final String MANIFEST_LOCALIZATIONS = "eclipse.p2.manifest.localizations"; //$NON-NLS-1$
	//
	//	static final Locale DEFAULT_LOCALE = new Locale("df", "LT"); //$NON-NLS-1$//$NON-NLS-2$
	//	static final Locale PSEUDO_LOCALE = new Locale("zz", "ZZ"); //$NON-NLS-1$//$NON-NLS-2$

	StateObjectFactory factory;
	State state;

	public BundleDescriptionFactory(StateObjectFactory factory, State state) {
		this.factory = factory;
		this.state = state;
		//TODO find a state and a factory when not provided
	}

	private PluginConverter acquirePluginConverter() {
		return (PluginConverter) ServiceHelper.getService(Activator.getContext(), PluginConverter.class.getName());
	}

	private Dictionary convertPluginManifest(File bundleLocation, boolean logConversionException) {
		PluginConverter converter;
		try {
			converter = acquirePluginConverter();
			if (converter == null)
				return null;
			return converter.convertManifest(bundleLocation, false, null, true, null);
		} catch (PluginConversionException convertException) {
			if (bundleLocation.getName().equals(FEATURE_FILENAME_DESCRIPTOR))
				return null;
			if (!new File(bundleLocation, PLUGIN_FILENAME_DESCRIPTOR).exists() && !new File(bundleLocation, FRAGMENT_FILENAME_DESCRIPTOR).exists())
				return null;
			if (logConversionException) {
				IStatus status = new Status(IStatus.WARNING, Activator.ID, 0, NLS.bind(Messages.exception_errorConverting, bundleLocation.getAbsolutePath()), convertException);
				System.out.println(status);
				//TODO Need to find a way to get a logging service to log
			}
			return null;
		}
	}

	public BundleDescription getBundleDescription(Dictionary enhancedManifest, File bundleLocation) {
		try {
			BundleDescription descriptor = factory.createBundleDescription(state, enhancedManifest, bundleLocation != null ? bundleLocation.getAbsolutePath() : null, 1); //TODO Do we need to have a real bundle id
			descriptor.setUserObject(enhancedManifest);
			return descriptor;
		} catch (BundleException e) {
			//			IStatus status = new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_STATE_PROBLEM, NLS.bind(Messages.exception_stateAddition, enhancedManifest.get(Constants.BUNDLE_NAME)), e);
			//			BundleHelper.getDefault().getLog().log(status);
			System.err.println(NLS.bind(Messages.exception_stateAddition, bundleLocation != null ? bundleLocation.getAbsoluteFile() : null));
			return null;
		}
	}

	public BundleDescription getBundleDescription(File bundleLocation) {
		Dictionary manifest = loadManifest(bundleLocation);
		if (manifest == null)
			return null;
		return getBundleDescription(manifest, bundleLocation);
	}

	public BundleDescription getBundleDescription(InputStream manifestStream, File bundleLocation) {
		Hashtable entries = new Hashtable();
		try {
			ManifestElement.parseBundleManifest(manifestStream, entries);
			return getBundleDescription(entries, bundleLocation);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Dictionary loadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		ZipFile jarFile = null;
		try {
			if ("jar".equalsIgnoreCase(new Path(bundleLocation.getName()).getFileExtension()) && bundleLocation.isFile()) { //$NON-NLS-1$
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
		if (manifestStream != null) {
			try {
				manifest = manifestToProperties(new Manifest(manifestStream).getMainAttributes());
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
		} else {
			manifest = convertPluginManifest(bundleLocation, true);
		}

		//Deal with the pre-3.0 plug-in shape who have a default jar manifest.mf
		if (manifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME) == null)
			manifest = convertPluginManifest(bundleLocation, true);

		if (manifest == null)
			return null;

		manifest.put(BUNDLE_FILE_KEY, bundleLocation.isDirectory() ? DIR : JAR);
		getManifestLocalizations(manifest, bundleLocation);
		// localizeManifest(manifest, bundleLocation);
		return manifest;
	}

	//	private Properties loadProperties(File bundleLocation, String localizationFile) throws IOException {
	//		Properties result = new Properties();
	//		InputStream propertyStream = null;
	//		try {
	//			try {
	//				if (bundleLocation.isDirectory())
	//					propertyStream = new FileInputStream(new File(bundleLocation, localizationFile));
	//				else {
	//					URLConnection connection = new URL("jar:" + bundleLocation.toURL().toExternalForm() + "!/" + localizationFile).openConnection(); //$NON-NLS-1$ //$NON-NLS-2$
	//					connection.setUseCaches(false);
	//					propertyStream = connection.getInputStream();
	//				}
	//			} catch (FileNotFoundException e) {
	//				// if there is no messages file then just return;
	//				return result;
	//			}
	//			result.load(propertyStream);
	//		} finally {
	//			if (propertyStream != null)
	//				propertyStream.close();
	//		}
	//		return result;
	//	}

	// Collect the manifest localizations from the bundle directory
	// and store them in the manifest.
	private void getManifestLocalizations(Dictionary manifest, File bundleLocation) {
		//		Map localizations;
		//		Locale defaultLocale = null; // = Locale.ENGLISH; // TODO: get this from GeneratorInfo
		//		String bundleLocalization = (String) manifest.get(Constants.BUNDLE_LOCALIZATION);
		//		if (bundleLocalization == null || bundleLocalization.trim().length() == 0)
		//			bundleLocalization = DEFAULT_BUNDLE_LOCALIZATION;
		//
		//		if ("jar".equalsIgnoreCase(new Path(bundleLocation.getName()).getFileExtension()) && //$NON-NLS-1$
		//				bundleLocation.isFile()) {
		//			localizations = getJarManifestLocalization(bundleLocation, bundleLocalization, manifest, defaultLocale);
		//		} else {
		//			localizations = getDirManifestLocalization(bundleLocation, bundleLocalization, manifest, defaultLocale);
		//		}
		//
		//		if (localizations.size() > 0) {
		//			manifest.put(MANIFEST_LOCALIZATIONS, localizations);
		//		}
	}

	//	private Map getJarManifestLocalization(File bundleLocation, String bundleLocalization, Dictionary manifest, Locale defaultLocale) {
	//		ZipFile jarFile = null;
	//		Map localizations = new HashMap(4);
	//		try {
	//			jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
	//			for (Enumeration entries = jarFile.entries(); entries.hasMoreElements();) {
	//				ZipEntry nextEntry = (ZipEntry) entries.nextElement();
	//				String nextName = nextEntry.getName();
	//				String localeString = getLocaleString(nextName, bundleLocalization);
	//
	//				if (!nextEntry.isDirectory() && localeString != null) {
	//					Locale nextLocale = getLocale(localeString);
	//					InputStream stream = null;
	//					try {
	//						stream = jarFile.getInputStream(nextEntry);
	//						Properties properties = new Properties();
	//						properties.load(stream);
	//						Properties localizedStrings = getLocalizedProperties(manifest, properties);
	//						if (localizedStrings.size() > 0) {
	//							localizations.put(nextLocale, localizedStrings);
	//							if (DEFAULT_LOCALE.equals(nextLocale) && defaultLocale != null) {
	//								localizations.put(nextLocale, localizedStrings);
	//							}
	//						}
	//					} finally {
	//						if (stream != null)
	//							stream.close();
	//					}
	//				}
	//			}
	//		} catch (IOException ioe) {
	//			ioe.printStackTrace();
	//		} finally {
	//			if (jarFile != null) {
	//				try {
	//					jarFile.close();
	//				} catch (IOException ioe) {
	//					// do nothing
	//				}
	//			}
	//		}
	//
	//		return localizations;
	//	}
	//
	//	private Map getDirManifestLocalization(File bundleLocation, String bundleLocalization, Dictionary manifest, Locale defaultLocale) {
	//		File localizationPath = new File(bundleLocation, bundleLocalization);
	//		File localizationDir = localizationPath.getParentFile();
	//		String localizationFile = localizationPath.getName();
	//		String[] localizationFiles = localizationDir.list(new LocalizationFileFilter(localizationFile));
	//
	//		HashMap localizations = null;
	//
	//		if (localizationFiles != null) {
	//			localizations = new HashMap(localizationFiles.length);
	//			for (int i = 0; i < localizationFiles.length; i++) {
	//				String nextFile = localizationFiles[i];
	//				Locale nextLocale = getLocale(getLocaleString(nextFile, localizationFile));
	//
	//				try {
	//					Properties properties = loadProperties(bundleLocation, nextFile);
	//					Properties localizedStrings = getLocalizedProperties(manifest, properties);
	//					if (localizedStrings.size() > 0) {
	//						localizations.put(nextLocale, localizedStrings);
	//						if (DEFAULT_LOCALE.equals(nextLocale) && defaultLocale != null) {
	//							localizations.put(nextLocale, localizedStrings);
	//						}
	//					}
	//				} catch (IOException ioe) {
	//					ioe.printStackTrace();
	//				}
	//			}
	//		}
	//
	//		return localizations;
	//	}

	//	private class LocalizationFileFilter implements FilenameFilter {
	//
	//		String filenamePrefix;
	//
	//		public LocalizationFileFilter(String filenamePrefix) {
	//			this.filenamePrefix = filenamePrefix;
	//		}
	//
	//		/* (non-Javadoc)
	//		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
	//		 */
	//		public boolean accept(File directory, String filename) {
	//			return (getLocaleString(filename, filenamePrefix) != null ? true : false);
	//		}
	//	}

	//	static public String getLocaleString(String filename, String filenamePrefix) {
	//		String localeString = null;
	//		if (filename.startsWith(filenamePrefix) && filename.endsWith(PROPERTIES_FILE_EXTENSION)) {
	//			if (filename.length() > filenamePrefix.length() + PROPERTIES_FILE_EXTENSION.length()) {
	//				localeString = filename.substring(filenamePrefix.length() + 1, filename.length() - PROPERTIES_FILE_EXTENSION.length());
	//			} else {
	//				localeString = ""; //$NON-NLS-1$
	//			}
	//		}
	//		return localeString;
	//	}

	//	static private Locale getLocale(String localeString) {
	//		Locale locale = DEFAULT_LOCALE;
	//		if (localeString.length() == 5 && localeString.indexOf('_') == 2) {
	//			locale = new Locale(localeString.substring(0, 2), localeString.substring(3, 5));
	//		} else if (localeString.length() == 2) {
	//			locale = new Locale(localeString.substring(0, 2));
	//		}
	//		return locale;
	//	}
	//
	//	static private Properties getLocalizedProperties(Dictionary manifest, Properties properties) {
	//		// Walk over the manifest and find all %xxx with the string value
	//		// in the properties file and copy them to the localized properties.
	//		Properties localizedProperties = new Properties();
	//		for (Enumeration e = manifest.keys(); e.hasMoreElements();) {
	//			String key = (String) e.nextElement();
	//			Object value = manifest.get(key);
	//			if (value instanceof String) {
	//				String stringValue = (String) value;
	//				if (stringValue.startsWith("%")) { //$NON-NLS-1$
	//					String newValue = properties.getProperty(stringValue.substring(1));
	//					if (newValue != null)
	//						localizedProperties.put(key, newValue);
	//				}
	//			}
	//		}
	//		return localizedProperties;
	//	}

	private Properties manifestToProperties(Attributes attributes) {
		Properties result = new Properties();
		for (Iterator i = attributes.keySet().iterator(); i.hasNext();) {
			Attributes.Name key = (Attributes.Name) i.next();
			result.put(key.toString(), attributes.get(key));
		}
		return result;
	}
}
