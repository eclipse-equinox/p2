/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.actions;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Publish IUs for all of the bundles in the given set of locations.  The locations can 
 * be actual locations of the bundles or folders of bundles.
 */
public class BundlesAction extends AbstractPublishingAction {

	// TODO reconsider the references to these specific ids in the action.  The action should be generic
	protected static final String ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	protected static final String ORG_ECLIPSE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$
	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace 
	 * representing and OSGi bundle resource
	 * @see RequiredCapability#getName()
	 * @see ProvidedCapability#getName()
	 */
	public static final String TYPE_ECLIPSE_BUNDLE = "bundle"; //$NON-NLS-1$

	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace 
	 * representing a source bundle
	 * @see RequiredCapability#getName()
	 */
	public static final String TYPE_ECLIPSE_SOURCE = "source"; //$NON-NLS-1$

	public static final String OSGI_BUNDLE_CLASSIFIER = "osgi.bundle"; //$NON-NLS-1$
	private static final String CAPABILITY_NS_OSGI_BUNDLE = "osgi.bundle"; //$NON-NLS-1$
	private static final String CAPABILITY_NS_OSGI_FRAGMENT = "osgi.fragment"; //$NON-NLS-1$

	public static final ProvidedCapability BUNDLE_CAPABILITY = MetadataFactory.createProvidedCapability(MetadataGeneratorHelper.NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, new Version(1, 0, 0));
	public static final ProvidedCapability SOURCE_BUNDLE_CAPABILITY = MetadataFactory.createProvidedCapability(MetadataGeneratorHelper.NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_SOURCE, new Version(1, 0, 0));

	static final String DEFAULT_BUNDLE_LOCALIZATION = "plugin"; //$NON-NLS-1$	
	static final String PROPERTIES_FILE_EXTENSION = ".properties"; //$NON-NLS-1$

	static final String BUNDLE_ADVICE_FILE = "META-INF/p2.inf"; //$NON-NLS-1$
	static final String ADVICE_INSTRUCTIONS_PREFIX = "instructions."; //$NON-NLS-1$
	private static final String[] BUNDLE_IU_PROPERTY_MAP = {Constants.BUNDLE_NAME, IInstallableUnit.PROP_NAME, Constants.BUNDLE_DESCRIPTION, IInstallableUnit.PROP_DESCRIPTION, Constants.BUNDLE_VENDOR, IInstallableUnit.PROP_PROVIDER, Constants.BUNDLE_CONTACTADDRESS, IInstallableUnit.PROP_CONTACT, Constants.BUNDLE_DOCURL, IInstallableUnit.PROP_DOC_URL};
	public static final String[] BUNDLE_LOCALIZED_PROPERTIES = {Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_VENDOR, Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_DOCURL, Constants.BUNDLE_UPDATELOCATION};
	public static final int BUNDLE_LOCALIZATION_INDEX = BUNDLE_LOCALIZED_PROPERTIES.length;

	private File[] locations;
	private StateObjectFactory stateObjectFactory;

	public static IArtifactKey createBundleArtifactKey(String bsn, String version) {
		return new ArtifactKey(OSGI_BUNDLE_CLASSIFIER, bsn, new Version(version));
	}

	public static IInstallableUnit createBundleConfigurationUnit(String hostId, Version hostVersion, boolean isBundleFragment, GeneratorBundleInfo configInfo, String configurationFlavor, String filter) {
		if (configInfo == null)
			return null;

		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = configurationFlavor + hostId;
		cu.setId(configUnitId);
		cu.setVersion(hostVersion);

		//Indicate the IU to which this CU apply
		cu.setHost(new RequiredCapability[] { //
				MetadataFactory.createRequiredCapability(CAPABILITY_NS_OSGI_BUNDLE, hostId, new VersionRange(hostVersion, true, MetadataGeneratorHelper.versionMax, true), null, false, false, true), // 
						MetadataFactory.createRequiredCapability(MetadataGeneratorHelper.NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, new VersionRange(new Version(1, 0, 0), true, new Version(2, 0, 0), false), null, false, false, false)});

		//Adds capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configUnitId, hostVersion), MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_FLAVOR, configurationFlavor, new Version(1, 0, 0))});

		Map touchpointData = new HashMap();
		touchpointData.put("install", "installBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("uninstall", "uninstallBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("configure", createConfigScript(configInfo, isBundleFragment)); //$NON-NLS-1$
		touchpointData.put("unconfigure", createUnconfigScript(configInfo, isBundleFragment)); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		cu.setFilter(filter);
		return MetadataFactory.createInstallableUnit(cu);
	}

	public static IInstallableUnit createBundleIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key) {
		return createBundleIU(bd, manifest, isFolderPlugin, key, false);
	}

	public static IInstallableUnit createBundleIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key, boolean useNestedAdvice) {
		Map manifestLocalizations = null;
		if (manifest != null && bd.getLocation() != null) {
			manifestLocalizations = getManifestLocalizations(manifest, new File(bd.getLocation()));
		}

		return createBundleIU(bd, manifest, isFolderPlugin, key, manifestLocalizations, useNestedAdvice);
	}

	public static IInstallableUnit createBundleIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key, Map manifestLocalizations) {
		return createBundleIU(bd, manifest, isFolderPlugin, key, manifestLocalizations, false);
	}

	public static IInstallableUnit createBundleIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key, Map manifestLocalizations, boolean useNestedAdvice) {
		boolean isBinaryBundle = true;
		if (manifest != null && manifest.containsKey("Eclipse-SourceBundle")) { //$NON-NLS-1$
			isBinaryBundle = false;
		}
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(bd.isSingleton());
		iu.setId(bd.getSymbolicName());
		iu.setVersion(bd.getVersion());
		iu.setFilter(bd.getPlatformFilter());

		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(bd.getSymbolicName(), new VersionRange(new Version(0, 0, 0), true, bd.getVersion(), false), IUpdateDescriptor.NORMAL, null));

		boolean isFragment = bd.getHost() != null;
		//		boolean requiresAFragment = isFragment ? false : requireAFragment(bd, manifest);

		//Process the required bundles
		BundleSpecification requiredBundles[] = bd.getRequiredBundles();
		ArrayList reqsDeps = new ArrayList();
		//		if (requiresAFragment)
		//			reqsDeps.add(MetadataFactory.createRequiredCapability(CAPABILITY_TYPE_OSGI_FRAGMENTS, bd.getSymbolicName(), VersionRange.emptyRange, null, false, false));
		if (isFragment)
			reqsDeps.add(MetadataFactory.createRequiredCapability(CAPABILITY_NS_OSGI_BUNDLE, bd.getHost().getName(), bd.getHost().getVersionRange(), null, false, false));
		for (int j = 0; j < requiredBundles.length; j++)
			reqsDeps.add(MetadataFactory.createRequiredCapability(CAPABILITY_NS_OSGI_BUNDLE, requiredBundles[j].getName(), requiredBundles[j].getVersionRange() == VersionRange.emptyRange ? null : requiredBundles[j].getVersionRange(), null, requiredBundles[j].isOptional(), false));

		// Process the import packages
		ImportPackageSpecification osgiImports[] = bd.getImportPackages();
		for (int i = 0; i < osgiImports.length; i++) {
			// TODO we need to sort out how we want to handle wild-carded dynamic imports - for now we ignore them
			ImportPackageSpecification importSpec = osgiImports[i];
			String importPackageName = importSpec.getName();
			if (importPackageName.indexOf('*') != -1)
				continue;

			VersionRange versionRange = importSpec.getVersionRange() == VersionRange.emptyRange ? null : importSpec.getVersionRange();

			//TODO this needs to be refined to take into account all the attribute handled by imports
			reqsDeps.add(MetadataFactory.createRequiredCapability(MetadataGeneratorHelper.CAPABILITY_NS_JAVA_PACKAGE, importPackageName, versionRange, null, isOptional(importSpec), false));
		}
		iu.setRequiredCapabilities((RequiredCapability[]) reqsDeps.toArray(new RequiredCapability[reqsDeps.size()]));

		// Create set of provided capabilities
		ArrayList providedCapabilities = new ArrayList();
		providedCapabilities.add(MetadataGeneratorHelper.createSelfCapability(bd.getSymbolicName(), bd.getVersion()));
		providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_OSGI_BUNDLE, bd.getSymbolicName(), bd.getVersion()));

		// Process the export package
		ExportPackageDescription exports[] = bd.getExportPackages();
		for (int i = 0; i < exports.length; i++) {
			//TODO make sure that we support all the refinement on the exports
			providedCapabilities.add(MetadataFactory.createProvidedCapability(MetadataGeneratorHelper.CAPABILITY_NS_JAVA_PACKAGE, exports[i].getName(), exports[i].getVersion() == Version.emptyVersion ? null : exports[i].getVersion()));
		}
		// Here we add a bundle capability to identify bundles
		if (isBinaryBundle)
			providedCapabilities.add(BUNDLE_CAPABILITY);
		else
			providedCapabilities.add(SOURCE_BUNDLE_CAPABILITY);

		if (isFragment)
			providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_OSGI_FRAGMENT, bd.getHost().getName(), bd.getVersion()));

		if (manifestLocalizations != null) {
			for (Iterator iter = manifestLocalizations.keySet().iterator(); iter.hasNext();) {
				Locale locale = (Locale) iter.next();
				Properties translatedStrings = (Properties) manifestLocalizations.get(locale);
				Enumeration propertyKeys = translatedStrings.propertyNames();
				while (propertyKeys.hasMoreElements()) {
					String nextKey = (String) propertyKeys.nextElement();
					iu.setProperty(locale.toString() + '.' + nextKey, translatedStrings.getProperty(nextKey));
				}
				providedCapabilities.add(makeTranslationCapability(bd.getSymbolicName(), locale));
			}
		}

		iu.setCapabilities((ProvidedCapability[]) providedCapabilities.toArray(new ProvidedCapability[providedCapabilities.size()]));

		iu.setArtifacts(new IArtifactKey[] {key});

		iu.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);

		// Set certain properties from the manifest header attributes as IU properties.
		// The values of these attributes may be localized (strings starting with '%')
		// with the translated values appearing in the localization IU fragments
		// associated with the bundle IU.
		if (manifest != null) {
			int i = 0;
			while (i < BUNDLE_IU_PROPERTY_MAP.length) {
				if (manifest.containsKey(BUNDLE_IU_PROPERTY_MAP[i])) {
					String value = (String) manifest.get(BUNDLE_IU_PROPERTY_MAP[i]);
					if (value != null && value.length() > 0) {
						iu.setProperty(BUNDLE_IU_PROPERTY_MAP[i + 1], value);
					}
				}
				i += 2;
			}
		}

		// Define the immutable metadata for this IU. In this case immutable means
		// that this is something that will not impact the configuration.
		Map touchpointData = new HashMap();
		if (isFolderPlugin)
			touchpointData.put("zipped", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("manifest", toManifestString(manifest)); //$NON-NLS-1$

		if (useNestedAdvice)
			mergeInstructionsAdvice(touchpointData, getBundleAdvice(bd.getLocation()));

		iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		return MetadataFactory.createInstallableUnit(iu);
	}

	// TODO need to figure out a mapping of this onto real advice and make this generic
	private static void mergeInstructionsAdvice(Map touchpointData, Map bundleAdvice) {
		if (touchpointData == null || bundleAdvice == null)
			return;

		for (Iterator iterator = bundleAdvice.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			if (key.startsWith(ADVICE_INSTRUCTIONS_PREFIX)) {
				String phase = key.substring(ADVICE_INSTRUCTIONS_PREFIX.length());
				String instructions = touchpointData.containsKey(phase) ? (String) touchpointData.get(phase) : ""; //$NON-NLS-1$
				if (instructions.length() > 0)
					instructions += ";"; //$NON-NLS-1$
				instructions += ((String) bundleAdvice.get(key)).trim();
				touchpointData.put(phase, instructions);
			}
		}
	}

	public static void createHostLocalizationFragment(IInstallableUnit bundleIU, BundleDescription bd, String hostId, String[] hostBundleManifestValues, Set localizationIUs) {
		Map hostLocalizations = getHostLocalizations(new File(bd.getLocation()), hostBundleManifestValues);
		if (hostLocalizations != null) {
			IInstallableUnitFragment localizationFragment = createLocalizationFragmentOfHost(bd, hostId, hostBundleManifestValues, hostLocalizations);
			localizationIUs.add(localizationFragment);
		}
	}

	/*
	 * @param hostId
	 * @param bd
	 * @param locale
	 * @param localizedStrings
	 * @return installableUnitFragment
	 */
	private static IInstallableUnitFragment createLocalizationFragmentOfHost(BundleDescription bd, String hostId, String[] hostManifestValues, Map hostLocalizations) {
		InstallableUnitFragmentDescription fragment = new MetadataFactory.InstallableUnitFragmentDescription();
		String fragmentId = makeHostLocalizationFragmentId(bd.getSymbolicName());
		fragment.setId(fragmentId);
		fragment.setVersion(bd.getVersion()); // TODO: is this a meaningful version?

		HostSpecification hostSpec = bd.getHost();
		RequiredCapability[] hostReqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, hostSpec.getName(), hostSpec.getVersionRange(), null, false, false, false)};
		fragment.setHost(hostReqs);

		fragment.setSingleton(true);
		fragment.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());

		// Create a provided capability for each locale and add the translated properties.
		ArrayList providedCapabilities = new ArrayList(hostLocalizations.keySet().size());
		for (Iterator iter = hostLocalizations.keySet().iterator(); iter.hasNext();) {
			Locale locale = (Locale) iter.next();
			Properties translatedStrings = (Properties) hostLocalizations.get(locale);

			Enumeration propertyKeys = translatedStrings.propertyNames();
			while (propertyKeys.hasMoreElements()) {
				String nextKey = (String) propertyKeys.nextElement();
				fragment.setProperty(locale.toString() + '.' + nextKey, translatedStrings.getProperty(nextKey));
			}
			providedCapabilities.add(makeTranslationCapability(hostId, locale));
		}
		fragment.setCapabilities((ProvidedCapability[]) providedCapabilities.toArray(new ProvidedCapability[providedCapabilities.size()]));

		return MetadataFactory.createInstallableUnitFragment(fragment);
	}

	/**
	 * @param id
	 * @return the id for the iu fragment containing localized properties
	 * 		   for the fragment with the given id.
	 */
	private static String makeHostLocalizationFragmentId(String id) {
		return id + ".translated_host_properties"; //$NON-NLS-1$
	}

	public static ProvidedCapability makeTranslationCapability(String hostId, Locale locale) {
		return MetadataFactory.createProvidedCapability(MetadataGeneratorHelper.NAMESPACE_IU_LOCALIZATION, locale.toString(), new Version(1, 0, 0));
	}

	private static String createConfigScript(GeneratorBundleInfo configInfo, boolean isBundleFragment) {
		if (configInfo == null)
			return ""; //$NON-NLS-1$

		String configScript = "";//$NON-NLS-1$
		if (!isBundleFragment && configInfo.getStartLevel() != BundleInfo.NO_LEVEL) {
			configScript += "setStartLevel(startLevel:" + configInfo.getStartLevel() + ");"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!isBundleFragment && configInfo.isMarkedAsStarted()) {
			configScript += "markStarted(started: true);"; //$NON-NLS-1$
		}

		if (configInfo.getSpecialConfigCommands() != null) {
			configScript += configInfo.getSpecialConfigCommands();
		}

		return configScript;
	}

	private static String createDefaultBundleConfigScript(GeneratorBundleInfo configInfo) {
		return createConfigScript(configInfo, false);
	}

	public static IInstallableUnit createDefaultBundleConfigurationUnit(GeneratorBundleInfo configInfo, GeneratorBundleInfo unconfigInfo, String configurationFlavor) {
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = MetadataGeneratorHelper.createDefaultConfigUnitId(OSGI_BUNDLE_CLASSIFIER, configurationFlavor);
		cu.setId(configUnitId);
		Version configUnitVersion = new Version(1, 0, 0);
		cu.setVersion(configUnitVersion);

		// Add capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(configUnitId, configUnitVersion), MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_FLAVOR, configurationFlavor, new Version(1, 0, 0))});

		// Create a required capability on bundles
		RequiredCapability[] reqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(MetadataGeneratorHelper.NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, VersionRange.emptyRange, null, false, true, false)};
		cu.setHost(reqs);
		Map touchpointData = new HashMap();

		touchpointData.put("install", "installBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("uninstall", "uninstallBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("configure", createDefaultBundleConfigScript(configInfo)); //$NON-NLS-1$
		touchpointData.put("unconfigure", createDefaultBundleUnconfigScript(unconfigInfo)); //$NON-NLS-1$

		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		return MetadataFactory.createInstallableUnit(cu);
	}

	private static String createDefaultBundleUnconfigScript(GeneratorBundleInfo unconfigInfo) {
		return createUnconfigScript(unconfigInfo, false);
	}

	private static String createUnconfigScript(GeneratorBundleInfo unconfigInfo, boolean isBundleFragment) {
		if (unconfigInfo == null)
			return ""; //$NON-NLS-1$
		String unconfigScript = "";//$NON-NLS-1$
		if (!isBundleFragment && unconfigInfo.getStartLevel() != BundleInfo.NO_LEVEL) {
			unconfigScript += "setStartLevel(startLevel:" + BundleInfo.NO_LEVEL + ");"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!isBundleFragment && unconfigInfo.isMarkedAsStarted()) {
			unconfigScript += "markStarted(started: false);"; //$NON-NLS-1$
		}

		if (unconfigInfo.getSpecialUnconfigCommands() != null) {
			unconfigScript += unconfigInfo.getSpecialUnconfigCommands();
		}
		return unconfigScript;
	}

	// TODO not really sure what to do with this method.  Got it from the Generator.  There is
	// an advice file that needs to be read.  This should likely go in another advice object.
	public static Map getBundleAdvice(String bundleLocation) {
		if (bundleLocation == null)
			return Collections.EMPTY_MAP;

		File bundle = new File(bundleLocation);
		if (!bundle.exists())
			return Collections.EMPTY_MAP;

		ZipFile jar = null;
		InputStream stream = null;
		if (bundle.isDirectory()) {
			File adviceFile = new File(bundle, BUNDLE_ADVICE_FILE);
			if (adviceFile.exists()) {
				try {
					stream = new BufferedInputStream(new FileInputStream(adviceFile));
				} catch (IOException e) {
					return Collections.EMPTY_MAP;
				}
			}
		} else if (bundle.isFile()) {
			try {
				jar = new ZipFile(bundle);
				ZipEntry entry = jar.getEntry(BUNDLE_ADVICE_FILE);
				if (entry != null)
					stream = new BufferedInputStream(jar.getInputStream(entry));
			} catch (IOException e) {
				if (jar != null)
					try {
						jar.close();
					} catch (IOException e1) {
						//boo
					}
				return Collections.EMPTY_MAP;
			}
		}

		Properties advice = null;
		if (stream != null) {
			try {
				advice = new Properties();
				advice.load(stream);
			} catch (IOException e) {
				return Collections.EMPTY_MAP;
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
					//boo
				}
			}
		}

		if (jar != null) {
			try {
				jar.close();
			} catch (IOException e) {
				// boo
			}
		}

		return advice != null ? advice : Collections.EMPTY_MAP;
	}

	private static boolean isOptional(ImportPackageSpecification importedPackage) {
		if (importedPackage.getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_DYNAMIC) || importedPackage.getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_OPTIONAL))
			return true;
		return false;
	}

	private static String toManifestString(Map p) {
		if (p == null)
			return null;
		Collection properties = p.entrySet();
		StringBuffer result = new StringBuffer();
		for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
			Map.Entry aProperty = (Map.Entry) iterator.next();
			if (aProperty.getKey().equals(BundleDescriptionFactory.BUNDLE_SHAPE))
				continue;
			result.append(aProperty.getKey()).append(": ").append(aProperty.getValue()).append('\n'); //$NON-NLS-1$
		}
		return result.toString();
	}

	// Return a map from locale to property set for the manifest localizations
	// from the given bundle directory and given bundle localization path/name
	// manifest property value.
	private static Map getManifestLocalizations(Map manifest, File bundleLocation) {
		Map localizations;
		Locale defaultLocale = null; // = Locale.ENGLISH; // TODO: get this from GeneratorInfo
		String[] bundleManifestValues = getManifestCachedValues(manifest);
		String bundleLocalization = bundleManifestValues[BUNDLE_LOCALIZATION_INDEX];

		if ("jar".equalsIgnoreCase(new Path(bundleLocation.getName()).getFileExtension()) && //$NON-NLS-1$
				bundleLocation.isFile()) {
			localizations = LocalizationHelper.getJarPropertyLocalizations(bundleLocation, bundleLocalization, defaultLocale, bundleManifestValues);
			//localizations = getJarManifestLocalization(bundleLocation, bundleLocalization, defaultLocale, bundleManifestValues);
		} else {
			localizations = LocalizationHelper.getDirPropertyLocalizations(bundleLocation, bundleLocalization, defaultLocale, bundleManifestValues);
			// localizations = getDirManifestLocalization(bundleLocation, bundleLocalization, defaultLocale, bundleManifestValues);
		}

		return localizations;
	}

	public static String[] getManifestCachedValues(Map manifest) {
		String[] cachedValues = new String[BUNDLE_LOCALIZED_PROPERTIES.length + 1];
		for (int j = 0; j < MetadataGeneratorHelper.BUNDLE_LOCALIZED_PROPERTIES.length; j++) {
			String value = (String) manifest.get(BUNDLE_LOCALIZED_PROPERTIES[j]);
			if (value != null && value.length() > 1 && value.charAt(0) == '%') {
				cachedValues[j] = value.substring(1);
			}
		}
		String localizationFile = (String) manifest.get(org.osgi.framework.Constants.BUNDLE_LOCALIZATION);
		cachedValues[BUNDLE_LOCALIZATION_INDEX] = (localizationFile != null ? localizationFile : DEFAULT_BUNDLE_LOCALIZATION);
		return cachedValues;
	}

	// Return a map from locale to property set for the manifest localizations
	// from the given bundle directory and given bundle localization path/name
	// manifest property value.
	public static Map getHostLocalizations(File bundleLocation, String[] hostBundleManifestValues) {
		Map localizations;
		Locale defaultLocale = null; // = Locale.ENGLISH; // TODO: get this from GeneratorInfo
		String hostBundleLocalization = hostBundleManifestValues[BUNDLE_LOCALIZATION_INDEX];

		if ("jar".equalsIgnoreCase(new Path(bundleLocation.getName()).getFileExtension()) && //$NON-NLS-1$
				bundleLocation.isFile()) {
			localizations = LocalizationHelper.getJarPropertyLocalizations(bundleLocation, hostBundleLocalization, defaultLocale, hostBundleManifestValues);
			//localizations = getJarManifestLocalization(bundleLocation, hostBundleLocalization, defaultLocale, hostBundleManifestValues);
		} else {
			localizations = LocalizationHelper.getDirPropertyLocalizations(bundleLocation, hostBundleLocalization, defaultLocale, hostBundleManifestValues);
			// localizations = getDirManifestLocalization(bundleLocation, hostBundleLocalization, defaultLocale, hostBundleManifestValues);
		}

		return localizations;
	}

	public BundlesAction(File[] locations) {
		this.locations = expandLocations(locations);
		// TODO need to figure a better way of configuring the generator...
		PlatformAdmin platformAdmin = (PlatformAdmin) ServiceHelper.getService(Activator.getContext(), PlatformAdmin.class.getName());
		if (platformAdmin != null)
			stateObjectFactory = platformAdmin.getFactory();
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		BundleDescription[] bundles = getBundleDescriptions(locations);
		generateBundleIUs(bundles, results, info);
		return Status.OK_STATUS;
	}

	private File[] expandLocations(File[] list) {
		ArrayList result = new ArrayList();
		expandLocations(list, result);
		return (File[]) result.toArray(new File[result.size()]);
	}

	private void expandLocations(File[] list, ArrayList result) {
		if (list == null)
			return;
		for (int i = 0; i < list.length; i++) {
			File location = list[i];
			if (location.isDirectory()) {
				// if the location is itself a bundle, just add it.  Otherwise r down
				if (!new File(location, "META-INF/MANIFEST.MF").exists()) //$NON-NLS-1$
					expandLocations(location.listFiles(), result);
				else
					result.add(location);
			} else {
				result.add(location);
			}
		}
	}

	protected void generateBundleIUs(BundleDescription[] bundles, IPublisherResult result, IPublisherInfo info) {
		// Computing the path for localized property files in a NL fragment bundle
		// requires the BUNDLE_LOCALIZATION property from the manifest of the host bundle,
		// so a first pass is done over all the bundles to cache this value as well as the tags
		// from the manifest for the localizable properties.
		final int CACHE_PHASE = 0;
		final int GENERATE_PHASE = 1;
		Map bundleLocalizationMap = new HashMap(bundles.length);
		Set localizationIUs = new HashSet(32);
		for (int phase = CACHE_PHASE; phase <= GENERATE_PHASE; phase++) {
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription bd = bundles[i];
				// A bundle may be null if the associated plug-in does not have a manifest file -
				// for example, org.eclipse.jdt.launching.j9
				if (bd != null && bd.getSymbolicName() != null && bd.getVersion() != null) {
					Map bundleManifest = (Map) bd.getUserObject();

					if (phase == CACHE_PHASE) {
						if (bundleManifest != null) {
							String[] cachedValues = MetadataGeneratorHelper.getManifestCachedValues(bundleManifest);
							bundleLocalizationMap.put(makeSimpleKey(bd), cachedValues);
						}
					} else {
						IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bd.getSymbolicName(), bd.getVersion().toString());
						File location = new File(bd.getLocation());
						IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(key, location);
						addProperties((ArtifactDescriptor) ad, location, info);
						// don't consider any advice here as we want to know about the real form on disk
						boolean isDir = isDir(bd, info);
						// if the artifact is a dir and we are not doing "AS_IS", zip it up.
						// TODO this does not cover all the cases.  What if the current artifact is not in the form it should be in the end?
						// for example we have a dir on disk but it should be deployed as a JAR?
						if (isDir && !((info.getArtifactOptions() & IPublisherInfo.A_AS_IS) > 0))
							publishArtifact(ad, new File(bd.getLocation()), new File(bd.getLocation()).listFiles(), info, INCLUDE_ROOT);
						else
							publishArtifact(ad, new File(bd.getLocation()), new File[] {new File(bd.getLocation())}, info, AS_IS | INCLUDE_ROOT);
						// FIXME 1.0 merge - need to consider phase instruction advice here.  See Generator#mergeInstructionsAdvice 
						IInstallableUnit bundleIU = MetadataGeneratorHelper.createBundleIU(bd, bundleManifest, isDir, key);

						if (isFragment(bd)) {
							// TODO: Can NL fragments be multi-host?  What special handling
							//		 is required for multi-host fragments in general?
							String hostId = bd.getHost().getName();
							String hostKey = makeSimpleKey(hostId);
							String[] cachedValues = (String[]) bundleLocalizationMap.get(hostKey);

							if (cachedValues != null) {
								MetadataGeneratorHelper.createHostLocalizationFragment(bundleIU, bd, hostId, cachedValues, localizationIUs);
							}
						}

						result.addIU(bundleIU, IPublisherResult.ROOT);
						result.addIUs(localizationIUs, IPublisherResult.NON_ROOT);
						localizationIUs.clear();
					}
				}
			}
		}
	}

	/**
	 * Add all of the advice for the bundle at the given location to the given descriptor.
	 * @param descriptor the descriptor to decorate
	 * @param location the location of the bundle
	 * @param info the publisher info supplying the advice
	 */
	private void addProperties(ArtifactDescriptor descriptor, File location, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, false, null, null, IBundleAdvice.class);
		for (Iterator i = advice.iterator(); i.hasNext();) {
			IBundleAdvice entry = (IBundleAdvice) i.next();
			Properties props = entry.getProperties(location);
			if (props == null)
				continue;
			for (Iterator j = props.keySet().iterator(); j.hasNext();) {
				String key = (String) j.next();
				descriptor.setRepositoryProperty(key, props.getProperty(key));
			}
		}
	}

	private boolean isDir(BundleDescription bundle, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, true, bundle.getSymbolicName(), bundle.getVersion(), IBundleShapeAdvice.class);
		// if the advice has a shape, use it
		if (advice != null && !advice.isEmpty()) {
			// we know there is some advice but if there is more than one, take the first.
			String shape = ((IBundleShapeAdvice) advice.iterator().next()).getShape();
			if (shape != null)
				return shape.equals(IBundleShapeAdvice.DIR);
		}
		// otherwise go with whatever we figured out from the manifest or the shape on disk
		Map manifest = (Map) bundle.getUserObject();
		String format = (String) manifest.get(BundleDescriptionFactory.BUNDLE_SHAPE);
		return BundleDescriptionFactory.DIR.equals(format);
	}

	private String makeSimpleKey(BundleDescription bd) {
		// TODO: can't use the bundle version in the key for the BundleLocalization
		//		 property map since the host specification for a fragment has a
		//		 version range, not a version. Hence, this mechanism for finding
		// 		 manifest localization property files may break under changes
		//		 to the BundleLocalization property of a bundle.
		return makeSimpleKey(bd.getSymbolicName() /*, bd.getVersion() */);
	}

	private String makeSimpleKey(String id /*, Version version */) {
		return id; // + '_' + version.toString();
	}

	private boolean isFragment(BundleDescription bd) {
		return (bd.getHost() != null ? true : false);
	}

	// TODO reconsider the special cases here for the configurators.  Perhaps these should be in their own actions.
	protected BundleDescription[] getBundleDescriptions(File[] bundleLocations) {
		if (bundleLocations == null)
			return new BundleDescription[0];
		boolean addSimpleConfigurator = false;
		boolean scIn = false;
		for (int i = 0; i < bundleLocations.length; i++) {
			if (!addSimpleConfigurator)
				addSimpleConfigurator = bundleLocations[i].toString().indexOf(ORG_ECLIPSE_UPDATE_CONFIGURATOR) > 0;
			if (!scIn) {
				scIn = bundleLocations[i].toString().indexOf(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR) > 0;
				if (scIn)
					break;
			}
		}
		if (scIn)
			addSimpleConfigurator = false;
		BundleDescription[] result = new BundleDescription[bundleLocations.length + (addSimpleConfigurator ? 1 : 0)];
		BundleDescriptionFactory factory = getBundleFactory();
		for (int i = 0; i < bundleLocations.length; i++) {
			result[i] = factory.getBundleDescription(bundleLocations[i]);
		}
		if (addSimpleConfigurator) {
			// Add simple configurator to the list of bundles
			try {
				URL configuratorURL = FileLocator.toFileURL(Activator.getContext().getBundle().getEntry(ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR + ".jar"));
				if (configuratorURL == null)
					System.out.println("Could not find simpleconfigurator bundle");
				else {
					File location = new File(configuratorURL.getFile()); //$NON-NLS-1$
					result[result.length - 1] = factory.getBundleDescription(location);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	protected BundleDescriptionFactory getBundleFactory() {
		return new BundleDescriptionFactory(stateObjectFactory, null);
	}

}
