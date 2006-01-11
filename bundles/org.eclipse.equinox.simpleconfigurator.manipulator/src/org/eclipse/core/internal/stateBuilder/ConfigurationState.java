/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.stateBuilder;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.internal.utils.BundleHelper;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;

// This class provides a higher level API on the state
public class ConfigurationState {
	private final static String OSGI_WS = "osgi.ws";  //$NON-NLS-1$
	private final static String OSGI_OS = "osgi.os";  //$NON-NLS-1$
	private final static String OSGI_ARCH = "osgi.arch";  //$NON-NLS-1$
	private final static String OSGI_NL = "osgi.nl";  //$NON-NLS-1$
	
	private static final String PROFILE_EXTENSION = ".profile"; //$NON-NLS-1$
	private static final String SYSTEM_PACKAGES = "org.osgi.framework.system.packages"; //$NON-NLS-1$

	private StateObjectFactory factory;
	protected State state;
	private long id;

	private String javaProfile;
	private String[] javaProfiles;

	protected long getNextId() {
		return ++id;
	}

	public ConfigurationState() {
		PlatformAdmin platformAdminService = (PlatformAdmin) BundleHelper.getDefault().acquireService(PlatformAdmin.class.getName());
		factory = platformAdminService.getFactory();
		state = factory.createState();
		state.setResolver(platformAdminService.getResolver());
		id = 0;
	}

	public void addBundleDescription(BundleDescription toAdd) {
		state.addBundle(toAdd);
	}

	private PluginConverter acquirePluginConverter() throws Exception {
		return (PluginConverter) BundleHelper.getDefault().acquireService(PluginConverter.class.getName());
	}

	//Add a bundle to the state, updating the version number 
	public boolean addBundle(Dictionary enhancedManifest, File bundleLocation) {
		try {
			BundleDescription descriptor = factory.createBundleDescription(state, enhancedManifest, bundleLocation.getAbsolutePath(), getNextId());
			state.addBundle(descriptor);
		} catch (BundleException e) {
			e.printStackTrace();	//TODO log the error
			return false;
		}
		return true;
	}

	public boolean addBundle(File bundleLocation) {
		Dictionary manifest;
		manifest = loadManifest(bundleLocation);
		if (manifest == null)
			return false;
		return addBundle(manifest, bundleLocation);
	}

	//Return a dictionary representing a manifest. The data may result from plugin.xml conversion  
	private Dictionary basicLoadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		ZipFile jarFile = null;
		try {
			//TODO Using Path make us require runtime.... this could be improved
			if ("jar".equalsIgnoreCase(new Path(bundleLocation.getName()).getFileExtension()) && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				manifestStream = new FileInputStream(new File(bundleLocation, JarFile.MANIFEST_NAME));
			}
		} catch (IOException e) {
			//ignore
		}

		//It is not a manifest, but a plugin or a fragment
		Dictionary manifest = null;
		if (manifestStream == null) {
			manifest = convertPluginManifest(bundleLocation, true);
			if (manifest == null)
				return null;
		}

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

	private void mergeExistingManifest(File bundleLocation, Dictionary initialManifest) {
		if (initialManifest.get(Constants.BUNDLE_SYMBOLICNAME) != null)
			return;

		Dictionary generatedManifest = convertPluginManifest(bundleLocation, false);
		if (generatedManifest == null)
			return;

		//merge manifests. The values from the generated manifest are added to the initial one. Values from the initial one are not deleted 
		Enumeration enumeration = generatedManifest.keys();
		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();
			if (initialManifest.get(key) == null)
				initialManifest.put(key, generatedManifest.get(key));
		}
	}

	private Dictionary loadManifest(File bundleLocation) {
		Dictionary manifest = basicLoadManifest(bundleLocation);
		if (manifest == null)
			return null;

		mergeExistingManifest(bundleLocation, manifest);
		return manifest;
	}

	private Dictionary convertPluginManifest(File bundleLocation, boolean logConversionException) {
		PluginConverter converter;
		try {
			converter = acquirePluginConverter();
			return converter.convertManifest(bundleLocation, false, null, false, null); //$NON-NLS-1$
		} catch (PluginConversionException convertException) {
			if (logConversionException) {
				convertException.printStackTrace(); //TODO to fix
//				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, 0, NLS.bind(Messages.exception_errorConverting, bundleLocation.getAbsolutePath()), convertException);
//				BundleHelper.getDefault().getLog().log(status);
			}
			return null;
		} catch (Exception serviceException) {
			serviceException.printStackTrace(); //TODO to fix 
//			IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, 0, NLS.bind(Messages.exception_cannotAcquireService, "Plugin converter"), serviceException); //$NON-NLS-1$
//			BundleHelper.getDefault().getLog().log(status);
			return null;
		}
	}

	private Properties manifestToProperties(Attributes d) {
		Iterator iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}

	public void addBundles(Collection bundles) {
		for (Iterator iter = bundles.iterator(); iter.hasNext();) {
			File bundle = (File) iter.next();
			addBundle(bundle);
		}
	}

	public void resolveState(String os, String ws, String arch, String nl, String jre) {
		//TODO the JRE argument is never user
		Hashtable properties = new Hashtable(3);
		if (ws == null) {
			properties.put(OSGI_WS, CatchAllValue.singleton);
		} else {
			properties.put(OSGI_WS, ws);
		}

		if (os == null) {
			properties.put(OSGI_OS, CatchAllValue.singleton);
		} else {
			properties.put(OSGI_OS, os);
		}

		if (arch == null) {
			properties.put(OSGI_ARCH, CatchAllValue.singleton);
		} else {
			properties.put(OSGI_ARCH, arch);
		}

		//Set the JRE profile
		if (javaProfile == null) {
			javaProfile = getDefaultJavaProfile();
		}
		String profile = getJavaProfilePackages();
		if (profile != null)
			properties.put(SYSTEM_PACKAGES, profile);

		state.setPlatformProperties(properties);
		state.resolve(false);
	}

	private String getDefaultJavaProfile() {
		if (javaProfiles == null)
			setJavaProfiles(getOSGiLocation());
		if (javaProfiles != null && javaProfiles.length > 0)
			return javaProfiles[0];
		return null;
	}

	public State getState() {
		return state;
	}


	/**
	 * This methods return the bundleDescriptions to which imports have been
	 * bound to.
	 * 
	 * @param root
	 */
	public static BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList resolvedImports = new ArrayList(packages.length);
		for (int i = 0; i < packages.length; i++)
			if (!root.getLocation().equals(packages[i].getExporter().getLocation()) && !resolvedImports.contains(packages[i].getExporter()))
				resolvedImports.add(packages[i].getExporter());
		return (BundleDescription[]) resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
	}

	/**
	 * This methods return the bundleDescriptions to which required bundles
	 * have been bound to.
	 * 
	 * @param root
	 */
	public static BundleDescription[] getRequiredBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		return root.getResolvedRequires();
	}

	public BundleDescription getResolvedBundle(String bundleId, String version) {
		if (version == null) {
			return getResolvedBundle(bundleId);
		}
		BundleDescription description = getState().getBundle(bundleId, Version.parseVersion(version));
		if (description != null && description.isResolved())
			return description;

		return null;
	}

	public BundleDescription getResolvedBundle(String bundleId) {
		BundleDescription[] description = getState().getBundles(bundleId);
		if (description == null)
			return null;
		for (int i = 0; i < description.length; i++) {
			if (description[i].isResolved())
				return description[i];
		}
		return null;
	}

	private File getOSGiLocation() {
		BundleDescription osgiBundle = state.getBundle("org.eclipse.osgi", null); //$NON-NLS-1$
		if (osgiBundle == null)
			return null;
		return new File(osgiBundle.getLocation());
	}

	private void setJavaProfiles(File bundleLocation) {
		if (bundleLocation == null)
			return;
		if (bundleLocation.isDirectory())
			javaProfiles = getDirJavaProfiles(bundleLocation);
		else
			javaProfiles = getJarJavaProfiles(bundleLocation);
		if (javaProfiles == null)
			return;
		// sort the javaProfiles in descending order
		Arrays.sort(javaProfiles, new Comparator() {
			public int compare(Object profile1, Object profile2) {
				return -((String) profile1).compareTo((String) profile2);
			}
		});
	}

	private String[] getDirJavaProfiles(File bundleLocation) {
		String[] profiles = bundleLocation.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(PROFILE_EXTENSION);
			}
		});
		return profiles;
	}

	private String[] getJarJavaProfiles(File bundleLocation) {
		ZipFile zipFile = null;
		ArrayList results = new ArrayList(6);
		try {
			zipFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
			Enumeration entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				String entryName = ((ZipEntry) entries.nextElement()).getName();
				if (entryName.indexOf('/') < 0 && entryName.endsWith(PROFILE_EXTENSION))
					results.add(entryName);
			}
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
		}
		return (String[]) results.toArray(new String[results.size()]);
	}

	private String getJavaProfilePackages() {
		if (javaProfile == null)
			return null;
		File location = getOSGiLocation();
		InputStream is = null;
		ZipFile zipFile = null;
		try {
			if (location.isDirectory()) {
				is = new FileInputStream(new File(location, javaProfile));
			} else {
				zipFile = null;
				try {
					zipFile = new ZipFile(location, ZipFile.OPEN_READ);
					ZipEntry entry = zipFile.getEntry(javaProfile);
					if (entry != null)
						is = zipFile.getInputStream(entry);
				} catch (IOException e) {
					// nothing to do
				}
			}
			Properties profile = new Properties();
			profile.load(is);
			return profile.getProperty(SYSTEM_PACKAGES);
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					// nothing to do
				}
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
		}
		return null;
	}
}
