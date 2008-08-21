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
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.generator.Activator;
import org.eclipse.equinox.internal.p2.metadata.generator.Messages;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;

/**
 * @deprecated All of the function here has moved to BundlesAction static methods
 */
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
			if (converter == null) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Unable to aquire PluginConverter service during generation for: " + bundleLocation));
				return null;
			}
			return converter.convertManifest(bundleLocation, false, null, true, null);
		} catch (PluginConversionException convertException) {
			// only log the exception if we had a plugin.xml or fragment.xml and we failed conversion
			if (bundleLocation.getName().equals(FEATURE_FILENAME_DESCRIPTOR))
				return null;
			if (!new File(bundleLocation, PLUGIN_FILENAME_DESCRIPTOR).exists() && !new File(bundleLocation, FRAGMENT_FILENAME_DESCRIPTOR).exists())
				return null;
			if (logConversionException) {
				IStatus status = new Status(IStatus.WARNING, Activator.ID, 0, NLS.bind(Messages.exception_errorConverting, bundleLocation.getAbsolutePath()), convertException);
				LogHelper.log(status);
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
			String message = NLS.bind(Messages.exception_stateAddition, bundleLocation == null ? null : bundleLocation.getAbsoluteFile());
			IStatus status = new Status(IStatus.WARNING, Activator.ID, message, e);
			LogHelper.log(status);
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
			String message = "An error occurred while reading the bundle description " + (bundleLocation == null ? "" : bundleLocation.getAbsolutePath() + '.'); //$NON-NLS-1$ //$NON-NLS-2$
			IStatus status = new Status(IStatus.ERROR, Activator.ID, message, e);
			LogHelper.log(status);
		} catch (BundleException e) {
			String message = "An error occurred while reading the bundle description " + (bundleLocation == null ? "" : bundleLocation.getAbsolutePath() + '.'); //$NON-NLS-1$ //$NON-NLS-2$
			IStatus status = new Status(IStatus.ERROR, Activator.ID, message, e);
			LogHelper.log(status);
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
				File manifestFile = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (manifestFile.exists())
					manifestStream = new BufferedInputStream(new FileInputStream(manifestFile));
			}
		} catch (IOException e) {
			//ignore but log
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "An error occurred while loading the bundle manifest " + bundleLocation, e)); //$NON-NLS-1$
		}

		Dictionary manifest = null;
		if (manifestStream != null) {
			try {
				Map manifestMap = ManifestElement.parseBundleManifest(manifestStream, null);
				// TODO temporary hack.  We are reading a Map but everyone wants a Dictionary so convert.
				// real answer is to have people expect a Map but that is a wider change.
				manifest = new Hashtable(manifestMap);
			} catch (IOException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "An error occurred while loading the bundle manifest " + bundleLocation, e)); //$NON-NLS-1$
				return null;
			} catch (BundleException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "An error occurred while loading the bundle manifest " + bundleLocation, e)); //$NON-NLS-1$
				return null;
			} finally {
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

		if (manifest == null)
			return null;

		//Deal with the pre-3.0 plug-in shape who have a default jar manifest.mf
		if (manifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME) == null)
			manifest = convertPluginManifest(bundleLocation, true);

		if (manifest == null)
			return null;

		manifest.put(BUNDLE_FILE_KEY, bundleLocation.isDirectory() ? DIR : JAR);
		return manifest;
	}
}
