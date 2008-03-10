/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.metadata.generator.Activator;
import org.eclipse.equinox.internal.p2.metadata.generator.features.SiteCategory;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

public class MetadataGeneratorHelper {
	/**
	 * A capability namespace representing the type of Eclipse resource (bundle, feature, source bundle, etc)
	 * @see RequiredCapability#getNamespace()
	 */
	public static final String NAMESPACE_ECLIPSE_TYPE = "org.eclipse.equinox.p2.eclipse.type"; //$NON-NLS-1$

	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace 
	 * representing and OSGi bundle resource
	 * @see RequiredCapability#getName()
	 */
	public static final String TYPE_ECLIPSE_BUNDLE = "bundle"; //$NON-NLS-1$
	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace 
	 * representing a feature
	 * @see RequiredCapability#getName()
	 */
	public static final String TYPE_ECLIPSE_FEATURE = "feature"; //$NON-NLS-1$

	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace 
	 * representing a source bundle
	 * @see RequiredCapability#getName()
	 */
	public static final String TYPE_ECLIPSE_SOURCE = "source"; //$NON-NLS-1$

	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace 
	 * representing localized manifest properties
	 * @see RequiredCapability#getName()
	 */
	public static final String TYPE_ECLIPSE_MANIFEST_LOCALIZATION = "manifest.localization"; //$NON-NLS-1$

	// Only certain properties in the bundle manifest are assumed to be localized.
	public static final String[] BUNDLE_LOCALIZED_PROPERTIES = {Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_VENDOR, Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_DOCURL, Constants.BUNDLE_UPDATELOCATION};
	public static final int BUNDLE_LOCALIZATION_INDEX = BUNDLE_LOCALIZED_PROPERTIES.length;

	private static final String[] BUNDLE_IU_PROPERTY_MAP = {Constants.BUNDLE_NAME, IInstallableUnit.PROP_NAME, Constants.BUNDLE_DESCRIPTION, IInstallableUnit.PROP_DESCRIPTION, Constants.BUNDLE_VENDOR, IInstallableUnit.PROP_PROVIDER, Constants.BUNDLE_CONTACTADDRESS, IInstallableUnit.PROP_CONTACT, Constants.BUNDLE_DOCURL, IInstallableUnit.PROP_DOC_URL};

	private static final String CAPABILITY_NS_JAVA_PACKAGE = "java.package"; //$NON-NLS-1$
	private static final String CAPABILITY_NS_OSGI_BUNDLE = "osgi.bundle"; //$NON-NLS-1$
	private static final String CAPABILITY_NS_OSGI_FRAGMENT = "osgi.fragment"; //$NON-NLS-1$

	private static final String CAPABILITY_NS_UPDATE_FEATURE = "org.eclipse.update.feature"; //$NON-NLS-1$

	private static final Version DEFAULT_JRE_VERSION = new Version("1.6"); //$NON-NLS-1$

	public static final String ECLIPSE_FEATURE_CLASSIFIER = "org.eclipse.update.feature"; //$NON-NLS-1$
	public static final String OSGI_BUNDLE_CLASSIFIER = "osgi.bundle"; //$NON-NLS-1$
	public static final String BINARY_ARTIFACT_CLASSIFIER = "binary"; //$NON-NLS-1$

	public static final String INSTALL_FEATURES_FILTER = "(org.eclipse.update.install.features=true)"; //$NON-NLS-1$

	private static final String IU_NAMESPACE = IInstallableUnit.NAMESPACE_IU_ID;

	private static final String LAUNCHER_ID_PREFIX = "org.eclipse.launcher"; //$NON-NLS-1$

	private static final String ECLIPSE_INSTALL_HANDLER_PROP = "org.eclipse.update.installHandler"; //$NON-NLS-1$

	//TODO - need to come up with a way to infer launcher version
	private static final Version LAUNCHER_VERSION = new Version(1, 0, 0);

	private static final Version versionMax = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	public static final TouchpointType TOUCHPOINT_NATIVE = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.native", new Version(1, 0, 0)); //$NON-NLS-1$
	public static final TouchpointType TOUCHPOINT_OSGI = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", new Version(1, 0, 0)); //$NON-NLS-1$

	public static final ProvidedCapability BUNDLE_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, new Version(1, 0, 0));
	public static final ProvidedCapability FEATURE_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_FEATURE, new Version(1, 0, 0));
	public static final ProvidedCapability SOURCE_BUNDLE_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_SOURCE, new Version(1, 0, 0));
	public static final ProvidedCapability MANIFEST_LOCALIZATION_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_MANIFEST_LOCALIZATION, new Version(1, 0, 0));

	static final String DEFAULT_BUNDLE_LOCALIZATION = "plugin"; //$NON-NLS-1$	
	static final String PROPERTIES_FILE_EXTENSION = ".properties"; //$NON-NLS-1$
	static final String MANIFEST_LOCALIZATIONS = "eclipse.p2.manifest.localizations"; //$NON-NLS-1$

	static final Locale DEFAULT_LOCALE = new Locale("df", "LT"); //$NON-NLS-1$//$NON-NLS-2$
	static final Locale PSEUDO_LOCALE = new Locale("zz", "ZZ"); //$NON-NLS-1$//$NON-NLS-2$

	public static IArtifactDescriptor createArtifactDescriptor(IArtifactKey key, File pathOnDisk, boolean asIs, boolean recur) {
		//TODO this size calculation is bogus
		ArtifactDescriptor result = new ArtifactDescriptor(key);
		if (pathOnDisk != null) {
			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, Long.toString(pathOnDisk.length()));
			// TODO - this is wrong but I'm testing a work-around for bug 205842
			result.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(pathOnDisk.length()));
		}
		return result;
	}

	public static IArtifactDescriptor createPack200ArtifactDescriptor(IArtifactKey key, File pathOnDisk, String installSize) {
		final String PACKED_FORMAT = "packed"; //$NON-NLS-1$
		//TODO this size calculation is bogus
		ArtifactDescriptor result = new ArtifactDescriptor(key);
		if (pathOnDisk != null) {
			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, installSize);
			// TODO - this is wrong but I'm testing a work-around for bug 205842
			result.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(pathOnDisk.length()));
		}
		ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
		result.setProcessingSteps(steps);
		result.setProperty(IArtifactDescriptor.FORMAT, PACKED_FORMAT);
		return result;
	}

	public static IArtifactKey createBundleArtifactKey(String bsn, String version) {
		return new ArtifactKey(OSGI_BUNDLE_CLASSIFIER, bsn, new Version(version));
	}

	public static IInstallableUnit createBundleConfigurationUnit(String iuId, Version iuVersion, boolean isBundleFragment, GeneratorBundleInfo configInfo, String configurationFlavor, String filter) {
		if (configInfo == null)
			return null;

		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = configurationFlavor + iuId;
		cu.setId(configUnitId);
		cu.setVersion(iuVersion);

		//Indicate the IU to which this CU apply
		cu.setHost(new RequiredCapability[] { //
				MetadataFactory.createRequiredCapability(CAPABILITY_NS_OSGI_BUNDLE, iuId, new VersionRange(iuVersion, true, versionMax, true), null, false, false), // 
						MetadataFactory.createRequiredCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, new VersionRange(new Version(1, 0, 0), true, new Version(2, 0, 0), false), null, false, false)});

		//Adds capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new ProvidedCapability[] {createSelfCapability(configUnitId, iuVersion), MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_FLAVOR, configurationFlavor, new Version(1, 0, 0))});

		Map touchpointData = new HashMap();
		touchpointData.put("install", "installBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("uninstall", "uninstallBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("configure", createConfigScript(configInfo, isBundleFragment)); //$NON-NLS-1$
		touchpointData.put("unconfigure", createUnconfigScript(configInfo, isBundleFragment)); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		cu.setFilter(filter);
		return MetadataFactory.createInstallableUnit(cu);
	}

	public static IInstallableUnit createBundleIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key, Set localizationIUs) {
		IInstallableUnit bundleIU = createBundleIU(bd, manifest, isFolderPlugin, key);

		if (manifest != null && bd.getLocation() != null) {
			String bundleLocalization = (String) manifest.get(Constants.BUNDLE_LOCALIZATION);
			if (bundleLocalization == null) {
				bundleLocalization = DEFAULT_BUNDLE_LOCALIZATION;
			}
			Map manifestLocalizations = getManifestLocalizations(manifest, new File(bd.getLocation()));
			if (manifestLocalizations != null) {
				localizationIUs.addAll(createLocalizationFragmentsForBundle(bd, manifestLocalizations));
			}
		}
		return bundleIU;
	}

	public static IInstallableUnit createBundleIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key) {
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
			reqsDeps.add(MetadataFactory.createRequiredCapability(CAPABILITY_NS_JAVA_PACKAGE, importPackageName, versionRange, null, isOptional(importSpec), false));
		}
		iu.setRequiredCapabilities((RequiredCapability[]) reqsDeps.toArray(new RequiredCapability[reqsDeps.size()]));

		// Create set of provided capabilities
		ArrayList providedCapabilities = new ArrayList();
		providedCapabilities.add(createSelfCapability(bd.getSymbolicName(), bd.getVersion()));
		providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_OSGI_BUNDLE, bd.getSymbolicName(), bd.getVersion()));

		// Process the export package
		ExportPackageDescription exports[] = bd.getExportPackages();
		for (int i = 0; i < exports.length; i++) {
			//TODO make sure that we support all the refinement on the exports
			providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_JAVA_PACKAGE, exports[i].getName(), exports[i].getVersion() == Version.emptyVersion ? null : exports[i].getVersion()));
		}
		// Here we add a bundle capability to identify bundles
		if (isBinaryBundle)
			providedCapabilities.add(BUNDLE_CAPABILITY);
		else
			providedCapabilities.add(SOURCE_BUNDLE_CAPABILITY);

		if (isFragment)
			providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_OSGI_FRAGMENT, bd.getHost().getName(), bd.getVersion()));
		iu.setCapabilities((ProvidedCapability[]) providedCapabilities.toArray(new ProvidedCapability[providedCapabilities.size()]));

		iu.setArtifacts(new IArtifactKey[] {key});

		iu.setTouchpointType(TOUCHPOINT_OSGI);

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
		iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		return MetadataFactory.createInstallableUnit(iu);
	}

	private static List createLocalizationFragmentsForBundle(BundleDescription bd, Map manifestLocalizations) {
		List localizationFragments = new ArrayList(manifestLocalizations.size());
		for (Iterator iter = manifestLocalizations.keySet().iterator(); iter.hasNext();) {
			Locale locale = (Locale) iter.next();
			Properties localizedStrings = (Properties) manifestLocalizations.get(locale);
			IInstallableUnitFragment nextLocaleFragment = createLocalizationFragmentOfBundle(bd, locale, localizedStrings);
			localizationFragments.add(nextLocaleFragment);
		}
		return localizationFragments;
	}

	/*
	 * @param bd
	 * @param locale
	 * @param localizedStrings
	 * @return installableUnitFragment
	 */
	private static IInstallableUnitFragment createLocalizationFragmentOfBundle(BundleDescription bd, Locale locale, Properties localizedStrings) {
		InstallableUnitFragmentDescription fragment = new MetadataFactory.InstallableUnitFragmentDescription();
		String fragmentId = makeLocalizationFragmentId(bd.getSymbolicName(), locale);
		fragment.setId(fragmentId);
		fragment.setVersion(bd.getVersion());

		RequiredCapability[] hostReqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(NAMESPACE_ECLIPSE_TYPE, bd.getSymbolicName(), new VersionRange(bd.getVersion(), true, bd.getVersion(), true), null, false, false)};
		fragment.setHost(hostReqs);

		fragment.setSingleton(true);

		Enumeration propertyKeys = localizedStrings.propertyNames();
		while (propertyKeys.hasMoreElements()) {
			String nextKey = (String) propertyKeys.nextElement();
			fragment.setProperty(nextKey, localizedStrings.getProperty(nextKey));
		}
		// TODO: do we need any capabilities?
		// Create set of provided capabilities It's just a tag indicating a localization fragment.
		ArrayList providedCapabilities = new ArrayList(1);
		providedCapabilities.add(MANIFEST_LOCALIZATION_CAPABILITY);
		fragment.setCapabilities((ProvidedCapability[]) providedCapabilities.toArray(new ProvidedCapability[providedCapabilities.size()]));

		return MetadataFactory.createInstallableUnitFragment(fragment);
	}

	public static void createHostLocalizationFragments(BundleDescription bd, String hostId, String[] hostBundleManifestValues, Set localizationIUs) {
		Map hostLocalizations = getHostLocalizations(new File(bd.getLocation()), hostBundleManifestValues);
		if (hostLocalizations != null) {
			for (Iterator iter = hostLocalizations.keySet().iterator(); iter.hasNext();) {
				Locale locale = (Locale) iter.next();
				Properties localizedStrings = (Properties) hostLocalizations.get(locale);
				IInstallableUnitFragment nextLocaleFragment = createLocalizationFragmentOfHost(hostId, hostBundleManifestValues, bd, locale, localizedStrings);
				localizationIUs.add(nextLocaleFragment);
			}
		}
	}

	/*
	 * @param hostId
	 * @param bd
	 * @param locale
	 * @param localizedStrings
	 * @return installableUnitFragment
	 */
	private static IInstallableUnitFragment createLocalizationFragmentOfHost(String hostId, String[] hostManifestValues, BundleDescription bd, Locale locale, Properties localizedStrings) {
		InstallableUnitFragmentDescription fragment = new MetadataFactory.InstallableUnitFragmentDescription();
		HostSpecification hostSpec = bd.getHost();
		String fragmentId = makeLocalizationFragmentId(hostId, locale);
		fragment.setId(fragmentId);
		fragment.setVersion(bd.getVersion()); // TODO: is this a meaningful version?

		RequiredCapability[] hostReqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(NAMESPACE_ECLIPSE_TYPE, hostSpec.getName(), hostSpec.getVersionRange(), null, false, false)};
		fragment.setHost(hostReqs);

		fragment.setSingleton(true);

		for (int i = 0; i < BUNDLE_LOCALIZED_PROPERTIES.length; i++) {
			String nextKey = hostManifestValues[i];
			String localizedValue = null;
			if (nextKey != null && (localizedValue = localizedStrings.getProperty(nextKey)) != null) {
				fragment.setProperty(nextKey, localizedValue);
			}
		}
		// TODO: do we need any capabilities? It's just a tag indicating a localization fragment.
		// Create set of provided capabilities
		ArrayList providedCapabilities = new ArrayList(1);
		providedCapabilities.add(MANIFEST_LOCALIZATION_CAPABILITY);
		fragment.setCapabilities((ProvidedCapability[]) providedCapabilities.toArray(new ProvidedCapability[providedCapabilities.size()]));

		return MetadataFactory.createInstallableUnitFragment(fragment);
	}

	/**
	 * @param id
	 * @param locale
	 * @return the id for the fragment contain the localized properties
	 * 		   for the manifest of the bundle with the given id
	 * 		   in the given locale.
	 */
	private static String makeLocalizationFragmentId(String id, Locale locale) {
		String localeString = (!DEFAULT_LOCALE.equals(locale) ? '_' + locale.toString() : ""); //$NON-NLS-1$
		return id + "_manifest" + localeString + "_properties"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Creates an IU corresponding to an update site category
	 * @param category The category descriptor
	 * @param featureIUs The IUs of the features that belong to the category
	 * @param parentCategory The parent category, or <code>null</code>
	 * @return an IU representing the category
	 */
	public static IInstallableUnit createCategoryIU(SiteCategory category, Set featureIUs, IInstallableUnit parentCategory) {
		InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
		cat.setSingleton(true);
		cat.setId(category.getName());
		cat.setVersion(Version.emptyVersion);
		cat.setProperty(IInstallableUnit.PROP_NAME, category.getLabel());
		cat.setProperty(IInstallableUnit.PROP_DESCRIPTION, category.getDescription());

		ArrayList reqsConfigurationUnits = new ArrayList(featureIUs.size());
		for (Iterator iterator = featureIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			reqsConfigurationUnits.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
		}
		//note that update sites don't currently support nested categories, but it may be useful to add in the future
		if (parentCategory != null) {
			reqsConfigurationUnits.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, parentCategory.getId(), VersionRange.emptyRange, parentCategory.getFilter(), false, false));
		}
		cat.setRequiredCapabilities((RequiredCapability[]) reqsConfigurationUnits.toArray(new RequiredCapability[reqsConfigurationUnits.size()]));
		cat.setCapabilities(new ProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, category.getName(), Version.emptyVersion)});
		cat.setArtifacts(new IArtifactKey[0]);
		cat.setProperty(IInstallableUnit.PROP_TYPE_CATEGORY, "true"); //$NON-NLS-1$
		return MetadataFactory.createInstallableUnit(cat);
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
		String configUnitId = createDefaultConfigUnitId(OSGI_BUNDLE_CLASSIFIER, configurationFlavor);
		cu.setId(configUnitId);
		Version configUnitVersion = new Version(1, 0, 0);
		cu.setVersion(configUnitVersion);

		// Add capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new ProvidedCapability[] {createSelfCapability(configUnitId, configUnitVersion), MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_FLAVOR, configurationFlavor, new Version(1, 0, 0))});

		// Create a required capability on bundles
		RequiredCapability[] reqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, VersionRange.emptyRange, null, false, true)};
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

	public static String createDefaultConfigUnitId(String classifier, String configurationFlavor) {
		return configurationFlavor + "." + classifier + ".default"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static IInstallableUnit createDefaultFeatureConfigurationUnit(String configurationFlavor) {
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = createDefaultConfigUnitId(ECLIPSE_FEATURE_CLASSIFIER, configurationFlavor);
		cu.setId(configUnitId);
		Version configUnitVersion = new Version(1, 0, 0);
		cu.setVersion(configUnitVersion);

		// Add capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new ProvidedCapability[] {createSelfCapability(configUnitId, configUnitVersion), MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_FLAVOR, configurationFlavor, new Version(1, 0, 0))});

		// Create a required capability on features
		RequiredCapability[] reqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_FEATURE, VersionRange.emptyRange, null, false, true)};
		cu.setHost(reqs);

		cu.setFilter(INSTALL_FEATURES_FILTER);
		Map touchpointData = new HashMap();
		touchpointData.put("install", "installFeature(feature:${artifact},featureId:default,featureVersion:default)"); //$NON-NLS-1$//$NON-NLS-2$
		touchpointData.put("uninstall", "uninstallFeature(feature:${artifact},featureId:default,featureVersion:default)"); //$NON-NLS-1$//$NON-NLS-2$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		return MetadataFactory.createInstallableUnit(cu);
	}

	public static IInstallableUnit createDefaultConfigurationUnitForSourceBundles(String configurationFlavor) {
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = createDefaultConfigUnitId("source", configurationFlavor); //$NON-NLS-1$
		cu.setId(configUnitId);
		Version configUnitVersion = new Version(1, 0, 0);
		cu.setVersion(configUnitVersion);

		// Add capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new ProvidedCapability[] {createSelfCapability(configUnitId, configUnitVersion), MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_FLAVOR, configurationFlavor, new Version(1, 0, 0))});

		// Create a required capability on source providers
		RequiredCapability[] reqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_SOURCE, VersionRange.emptyRange, null, false, true)};
		cu.setHost(reqs);
		Map touchpointData = new HashMap();

		touchpointData.put("install", "addSourceBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("uninstall", "removeSourceBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		return MetadataFactory.createInstallableUnit(cu);
	}

	private static void addExtraProperties(IInstallableUnit iiu, Properties extraProperties) {
		if (iiu instanceof InstallableUnit) {
			InstallableUnit iu = (InstallableUnit) iiu;

			for (Enumeration e = extraProperties.propertyNames(); e.hasMoreElements();) {
				String name = (String) e.nextElement();
				iu.setProperty(name, extraProperties.getProperty(name));
			}
		}
	}

	public static IInstallableUnit[] createEclipseIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key, Properties extraProperties) {
		ArrayList iusCreated = new ArrayList(4);

		IInstallableUnit iu = createBundleIU(bd, manifest, isFolderPlugin, key);
		addExtraProperties(iu, extraProperties);
		iusCreated.add(iu);

		if (manifest != null) {
			String bundleLocalization = null;
			if (bd.getHost() == null) // not a fragment
				bundleLocalization = (String) manifest.get(Constants.BUNDLE_LOCALIZATION);
			if (bundleLocalization == null)
				bundleLocalization = DEFAULT_BUNDLE_LOCALIZATION;

			Map manifestLocalizations = getManifestLocalizations(manifest, new File(bd.getLocation()));

			if (manifestLocalizations != null) {
				List localizationFragments = createLocalizationFragmentsForBundle(bd, manifestLocalizations);
				for (Iterator iter = localizationFragments.iterator(); iter.hasNext();) {
					addExtraProperties((IInstallableUnit) iter.next(), extraProperties);
				}
				iusCreated.addAll(localizationFragments);
			}
		}

		return (IInstallableUnit[]) (iusCreated.toArray(new IInstallableUnit[iusCreated.size()]));
	}

	public static IArtifactKey createFeatureArtifactKey(String fsn, String version) {
		return new ArtifactKey(ECLIPSE_FEATURE_CLASSIFIER, fsn, new Version(version));
	}

	public static IInstallableUnit createFeatureJarIU(Feature feature, boolean isExploded) {
		return createFeatureJarIU(feature, isExploded, null);
	}

	public static IInstallableUnit createFeatureJarIU(Feature feature, boolean isExploded, Properties extraProperties) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		String id = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/false);
		iu.setId(id);
		Version version = new Version(feature.getVersion());
		iu.setVersion(version);
		if (feature.getLicense() != null)
			iu.setLicense(new License(feature.getLicenseURL(), feature.getLicense()));
		if (feature.getCopyright() != null)
			iu.setCopyright(new Copyright(feature.getCopyrightURL(), feature.getCopyright()));

		// The required capabilities are not specified at this level because we don't want the feature jar to be attractive to install.

		iu.setTouchpointType(TOUCHPOINT_OSGI);
		iu.setFilter(INSTALL_FEATURES_FILTER);
		iu.setSingleton(true);

		if (feature.getInstallHandler() != null) {
			String installHandlerProperty = "handler=" + feature.getInstallHandler(); //$NON-NLS-1$

			if (feature.getInstallHandlerLibrary() != null)
				installHandlerProperty += ", library=" + feature.getInstallHandlerLibrary(); //$NON-NLS-1$

			if (feature.getInstallHandlerURL() != null)
				installHandlerProperty += ", url=" + feature.getInstallHandlerURL(); //$NON-NLS-1$

			iu.setProperty(ECLIPSE_INSTALL_HANDLER_PROP, installHandlerProperty);
		}

		iu.setCapabilities(new ProvidedCapability[] {createSelfCapability(id, version), FEATURE_CAPABILITY, MetadataFactory.createProvidedCapability(CAPABILITY_NS_UPDATE_FEATURE, feature.getId(), version)});
		iu.setArtifacts(new IArtifactKey[] {createFeatureArtifactKey(feature.getId(), version.toString())});

		if (isExploded) {
			// Define the immutable metadata for this IU. In this case immutable means
			// that this is something that will not impact the configuration.
			Map touchpointData = new HashMap();
			touchpointData.put("zipped", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		}

		if (extraProperties != null) {
			Enumeration e = extraProperties.propertyNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				iu.setProperty(name, extraProperties.getProperty(name));
			}
		}

		return MetadataFactory.createInstallableUnit(iu);
	}

	public static IInstallableUnit createGroupIU(Feature feature, IInstallableUnit featureIU) {
		return createGroupIU(feature, featureIU, null);
	}

	public static IInstallableUnit createGroupIU(Feature feature, IInstallableUnit featureIU, Properties extraProperties) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		String id = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/true);
		iu.setId(id);
		Version version = new Version(feature.getVersion());
		iu.setVersion(version);
		iu.setProperty(IInstallableUnit.PROP_NAME, feature.getLabel());
		if (feature.getLicense() != null)
			iu.setLicense(new License(feature.getLicenseURL(), feature.getLicense()));
		if (feature.getCopyright() != null)
			iu.setCopyright(new Copyright(feature.getCopyrightURL(), feature.getCopyright()));
		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, new VersionRange(new Version(0, 0, 0), true, new Version(feature.getVersion()), false), IUpdateDescriptor.NORMAL, null));

		FeatureEntry entries[] = feature.getEntries();
		RequiredCapability[] required = new RequiredCapability[entries.length + 1];
		for (int i = 0; i < entries.length; i++) {
			VersionRange range = getVersionRange(entries[i]);
			required[i] = MetadataFactory.createRequiredCapability(IU_NAMESPACE, getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true), range, getFilter(entries[i]), entries[i].isOptional(), false);
		}
		required[entries.length] = MetadataFactory.createRequiredCapability(IU_NAMESPACE, featureIU.getId(), new VersionRange(featureIU.getVersion(), true, featureIU.getVersion(), true), INSTALL_FEATURES_FILTER, false, false);
		iu.setRequiredCapabilities(required);
		iu.setTouchpointType(TouchpointType.NONE);
		iu.setProperty(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		// TODO: shouldn't the filter for the group be constructed from os, ws, arch, nl
		// 		 of the feature?
		// iu.setFilter(filter);
		iu.setCapabilities(new ProvidedCapability[] {createSelfCapability(id, version)});

		if (extraProperties != null) {
			Enumeration e = extraProperties.propertyNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				iu.setProperty(name, extraProperties.getProperty(name));
			}
		}

		return MetadataFactory.createInstallableUnit(iu);
	}

	/**
	 * Creates IUs and artifact descriptors for the JRE.  The resulting IUs are added
	 * to the given set, and the resulting artifact descriptor, if any, is returned.
	 * If the jreLocation is <code>null</code>, default information is generated.
	 */
	public static IArtifactDescriptor createJREData(File jreLocation, Set resultantIUs) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(false);
		String id = "a.jre"; //$NON-NLS-1$
		Version version = DEFAULT_JRE_VERSION;
		iu.setId(id);
		iu.setVersion(version);
		iu.setTouchpointType(TOUCHPOINT_NATIVE);

		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configId = "config." + id;//$NON-NLS-1$
		cu.setId(configId);
		cu.setVersion(version);
		cu.setHost(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, id, new VersionRange(version, true, versionMax, true), null, false, false)});
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new ProvidedCapability[] {createSelfCapability(configId, version)});
		cu.setTouchpointType(TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();

		if (jreLocation == null || !jreLocation.exists()) {
			//set some reasonable defaults
			iu.setVersion(version);
			iu.setCapabilities(generateJRECapability(id, version, null));
			resultantIUs.add(MetadataFactory.createInstallableUnit(iu));

			touchpointData.put("install", ""); //$NON-NLS-1$ //$NON-NLS-2$
			cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
			resultantIUs.add(MetadataFactory.createInstallableUnit(cu));
			return null;
		}
		generateJREIUData(iu, id, version, jreLocation);

		//Generate artifact for JRE
		IArtifactKey key = new ArtifactKey(BINARY_ARTIFACT_CLASSIFIER, id, version);
		iu.setArtifacts(new IArtifactKey[] {key});
		resultantIUs.add(MetadataFactory.createInstallableUnit(iu));

		//Create config info for the CU
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		resultantIUs.add(MetadataFactory.createInstallableUnit(cu));

		//Create the artifact descriptor
		return createArtifactDescriptor(key, jreLocation, false, true);
	}

	public static ArtifactKey createLauncherArtifactKey(String id, Version version) {
		return new ArtifactKey(BINARY_ARTIFACT_CLASSIFIER, id, version);
	}

	/**
	 * Creates IUs and artifacts for the Launcher executable. The resulting IUs are added
	 * to the given set, and the resulting artifact descriptor is returned.
	 */
	public static IArtifactDescriptor createLauncherIU(File launcher, String configurationFlavor, Set resultantIUs) {
		if (launcher == null || !launcher.exists())
			return null;

		//Create the IU
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String launcherId = LAUNCHER_ID_PREFIX + '_' + launcher.getName();
		iu.setId(launcherId);
		iu.setVersion(LAUNCHER_VERSION);

		IArtifactKey key = createLauncherArtifactKey(launcherId, LAUNCHER_VERSION);
		iu.setArtifacts(new IArtifactKey[] {key});
		iu.setCapabilities(new ProvidedCapability[] {createSelfCapability(launcherId, LAUNCHER_VERSION)});
		iu.setTouchpointType(TOUCHPOINT_NATIVE);
		resultantIUs.add(MetadataFactory.createInstallableUnit(iu));

		//Create the CU
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = configurationFlavor + launcherId;
		cu.setId(configUnitId);
		cu.setVersion(LAUNCHER_VERSION);
		cu.setHost(new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, launcherId, new VersionRange(LAUNCHER_VERSION, true, versionMax, true), null, false, false)});
		cu.setProperty(IInstallableUnit.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new ProvidedCapability[] {createSelfCapability(configUnitId, LAUNCHER_VERSION)});
		cu.setTouchpointType(TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
		if (!info.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32)) {
			if (info.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_MACOSX)) {
				configurationData += " chmod(targetDir:${installFolder}/Eclipse.app/Contents/MacOS, targetFile:eclipse, permissions:755);"; //$NON-NLS-1$
				generateLauncherSetter("Eclipse", launcherId, LAUNCHER_VERSION, "macosx", null, null, resultantIUs);
			} else
				configurationData += " chmod(targetDir:${installFolder}, targetFile:" + launcher.getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			generateLauncherSetter("eclipse", launcherId, LAUNCHER_VERSION, "win32", null, null, resultantIUs);
		}
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		resultantIUs.add(MetadataFactory.createInstallableUnitFragment(cu));

		//Create the artifact descriptor
		return createArtifactDescriptor(key, launcher, false, true);
	}

	public static void generateLauncherSetter(String launcherName, String iuId, Version version, String os, String ws, String arch, Set result) {
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		iud.setId(iuId + '.' + launcherName);
		iud.setVersion(version);
		iud.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);

		if (os != null || ws != null || arch != null) {
			String filterOs = os != null ? "(os=" + os + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterWs = ws != null ? "(ws=" + ws + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterArch = arch != null ? "(arch=" + arch + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			iud.setFilter("(& " + filterOs + filterWs + filterArch + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Map touchpointData = new HashMap();
		touchpointData.put("configure", "setLauncherName(name:" + launcherName + ")");
		touchpointData.put("unconfigure", "setLauncherName()");
		iud.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		result.add(MetadataFactory.createInstallableUnit(iud));
	}

	public static ProvidedCapability createSelfCapability(String installableUnitId, Version installableUnitVersion) {
		return MetadataFactory.createProvidedCapability(IU_NAMESPACE, installableUnitId, installableUnitVersion);
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

	private static ProvidedCapability[] generateJRECapability(String installableUnitId, Version installableUnitVersion, InputStream profileStream) {
		if (profileStream == null) {
			//use the 1.6 profile stored in the generator bundle
			try {
				profileStream = Activator.getContext().getBundle().getEntry("JavaSE-1.6.profile").openStream(); //$NON-NLS-1$
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		Properties p = new Properties();
		try {
			p.load(profileStream);
			ManifestElement[] jrePackages = ManifestElement.parseHeader("org.osgi.framework.system.packages", (String) p.get("org.osgi.framework.system.packages")); //$NON-NLS-1$ //$NON-NLS-2$
			ProvidedCapability[] exportedPackageAsCapabilities = new ProvidedCapability[jrePackages.length + 1];
			exportedPackageAsCapabilities[0] = createSelfCapability(installableUnitId, installableUnitVersion);
			for (int i = 1; i <= jrePackages.length; i++) {
				exportedPackageAsCapabilities[i] = MetadataFactory.createProvidedCapability(CAPABILITY_NS_JAVA_PACKAGE, jrePackages[i - 1].getValue(), null);
			}
			return exportedPackageAsCapabilities;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (profileStream != null) {
				try {
					profileStream.close();
				} catch (IOException e) {
					//ignore secondary failure
				}
			}
		}
		return new ProvidedCapability[0];
	}

	private static void generateJREIUData(InstallableUnitDescription iu, String installableUnitId, Version installableUnitVersion, File jreLocation) {
		//Look for a JRE profile file to set version and capabilities
		File[] profiles = jreLocation.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().endsWith(".profile"); //$NON-NLS-1$
			}
		});
		if (profiles.length != 1) {
			iu.setVersion(DEFAULT_JRE_VERSION);
			iu.setCapabilities(generateJRECapability(installableUnitId, installableUnitVersion, null));
			return;
		}
		String profileName = profiles[0].getAbsolutePath().substring(profiles[0].getAbsolutePath().lastIndexOf('/'));
		Version version = DEFAULT_JRE_VERSION;
		//TODO Find a better way to determine JRE version
		if (profileName.indexOf("1.6") > 0) { //$NON-NLS-1$
			version = new Version("1.6"); //$NON-NLS-1$
		} else if (profileName.indexOf("1.5") > 0) { //$NON-NLS-1$
			version = new Version("1.5"); //$NON-NLS-1$
		} else if (profileName.indexOf("1.4") > 0) { //$NON-NLS-1$
			version = new Version("1.4"); //$NON-NLS-1$
		}
		iu.setVersion(version);
		try {
			iu.setCapabilities(generateJRECapability(installableUnitId, installableUnitVersion, new FileInputStream(profiles[0])));
		} catch (FileNotFoundException e) {
			//Shouldn't happen, but ignore and fall through to use default
		}
	}

	public static String getFilter(FeatureEntry entry) {
		StringBuffer result = new StringBuffer();
		result.append("(&"); //$NON-NLS-1$
		if (entry.getFilter() != null)
			result.append(entry.getFilter());
		if (entry.getOS() != null)
			result.append("(osgi.os=" + entry.getOS() + ')');//$NON-NLS-1$
		if (entry.getWS() != null)
			result.append("(osgi.ws=" + entry.getWS() + ')');//$NON-NLS-1$
		if (entry.getArch() != null)
			result.append("(osgi.arch=" + entry.getArch() + ')');//$NON-NLS-1$
		if (entry.getNL() != null)
			result.append("(osgi.nl=" + entry.getNL() + ')');//$NON-NLS-1$
		if (result.length() == 2)
			return null;
		result.append(')');
		return result.toString();
	}

	public static String getTransformedId(String original, boolean isPlugin, boolean isGroup) {
		return (isPlugin ? original : original + (isGroup ? ".feature.group" : ".feature.jar")); //$NON-NLS-1$//$NON-NLS-2$
	}

	public static VersionRange getVersionRange(FeatureEntry entry) {
		String versionSpec = entry.getVersion();
		if (versionSpec == null)
			// TODO should really be returning VersionRange.emptyRange here...
			return null;
		Version version = new Version(versionSpec);
		if (!entry.isRequires())
			return new VersionRange(version, true, version, true);
		String match = entry.getMatch();
		if (match == null)
			// TODO should really be returning VersionRange.emptyRange here...
			return null;
		if (match.equals("perfect")) //$NON-NLS-1$
			return new VersionRange(version, true, version, true);
		if (match.equals("equivalent")) { //$NON-NLS-1$
			Version upper = new Version(version.getMajor(), version.getMinor() + 1, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("compatible")) { //$NON-NLS-1$
			Version upper = new Version(version.getMajor() + 1, 0, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("greaterOrEqual")) //$NON-NLS-1$
			return new VersionRange(version, true, new VersionRange(null).getMaximum(), true);
		return null;
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
			if (aProperty.getKey().equals(BundleDescriptionFactory.BUNDLE_FILE_KEY))
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
			localizations = getJarManifestLocalization(bundleLocation, bundleLocalization, defaultLocale, bundleManifestValues);
		} else {
			localizations = getDirManifestLocalization(bundleLocation, bundleLocalization, defaultLocale, bundleManifestValues);
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
			localizations = getJarManifestLocalization(bundleLocation, hostBundleLocalization, defaultLocale, hostBundleManifestValues);
		} else {
			localizations = getDirManifestLocalization(bundleLocation, hostBundleLocalization, defaultLocale, hostBundleManifestValues);
		}

		return localizations;
	}

	private static Map getJarManifestLocalization(File bundleLocation, String bundleLocalization, Locale defaultLocale, String[] bundleManifestValues) {
		ZipFile jarFile = null;
		Map localizations = new HashMap(4);
		try {
			jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
			for (Enumeration entries = jarFile.entries(); entries.hasMoreElements();) {
				ZipEntry nextEntry = (ZipEntry) entries.nextElement();
				String nextName = nextEntry.getName();
				String localeString = getLocaleString(nextName, bundleLocalization);

				if (!nextEntry.isDirectory() && localeString != null) {
					Locale nextLocale = getLocale(localeString);
					InputStream stream = null;
					try {
						stream = jarFile.getInputStream(nextEntry);
						Properties properties = new Properties();
						properties.load(stream);
						Properties localizedStrings = getLocalizedProperties(bundleManifestValues, properties);
						if (localizedStrings.size() > 0) {
							localizations.put(nextLocale, localizedStrings);
							if (DEFAULT_LOCALE.equals(nextLocale) && defaultLocale != null) {
								localizations.put(nextLocale, localizedStrings);
							}
						}
					} finally {
						if (stream != null)
							stream.close();
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (IOException ioe) {
					// do nothing
				}
			}
		}

		return localizations;
	}

	private static Map getDirManifestLocalization(File bundleLocation, String bundleLocalization, Locale defaultLocale, String[] hostBundleManifestValues) {
		File localizationPath = new File(bundleLocation, bundleLocalization);
		File localizationDir = localizationPath.getParentFile();
		final String localizationFile = localizationPath.getName();
		MetadataGeneratorHelper foo = new MetadataGeneratorHelper();
		String[] localizationFiles = localizationDir.list(foo.new LocalizationFileFilter() {
			public boolean accept(File directory, String filename) {
				return (getLocaleString(filename, localizationFile) != null ? true : false);
			}
		});

		HashMap localizations = null;

		if (localizationFiles != null) {
			localizations = new HashMap(localizationFiles.length);
			for (int i = 0; i < localizationFiles.length; i++) {
				String nextFile = localizationFiles[i];
				Locale nextLocale = getLocale(getLocaleString(nextFile, localizationFile));

				try {
					Properties properties = loadProperties(bundleLocation, nextFile);
					Properties localizedStrings = getLocalizedProperties(hostBundleManifestValues, properties);
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

	private abstract class LocalizationFileFilter implements FilenameFilter {

		public LocalizationFileFilter() {
			// Nothing to do
		}

		/* (non-Javadoc)
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		public abstract boolean accept(File directory, String filename);
	}

	static public String getLocaleString(String filename, String filenamePrefix) {
		String localeString = null;
		if (filename.startsWith(filenamePrefix) && filename.endsWith(PROPERTIES_FILE_EXTENSION)) {
			if (filename.length() > filenamePrefix.length() + PROPERTIES_FILE_EXTENSION.length()) {
				localeString = filename.substring(filenamePrefix.length() + 1, filename.length() - PROPERTIES_FILE_EXTENSION.length());
			} else {
				localeString = ""; //$NON-NLS-1$
			}
		}
		return localeString;
	}

	private static Properties loadProperties(File bundleLocation, String localizationFile) throws IOException {
		Properties result = new Properties();
		InputStream propertyStream = null;
		try {
			try {
				if (bundleLocation.isDirectory())
					propertyStream = new FileInputStream(new File(bundleLocation, localizationFile));
				else {
					URLConnection connection = new URL("jar:" + bundleLocation.toURL().toExternalForm() + "!/" + localizationFile).openConnection(); //$NON-NLS-1$ //$NON-NLS-2$
					connection.setUseCaches(false);
					propertyStream = connection.getInputStream();
				}
			} catch (FileNotFoundException e) {
				// if there is no messages file then just return;
				return result;
			}
			result.load(propertyStream);
		} finally {
			if (propertyStream != null)
				propertyStream.close();
		}
		return result;
	}

	static private Locale getLocale(String localeString) {
		Locale locale = DEFAULT_LOCALE;
		if (localeString.length() == 5 && localeString.indexOf('_') == 2) {
			locale = new Locale(localeString.substring(0, 2), localeString.substring(3, 5));
		} else if (localeString.length() == 2) {
			locale = new Locale(localeString.substring(0, 2));
		}
		return locale;
	}

	static private Properties getLocalizedProperties(String[] bundleManifestKeys, Properties properties) {
		Properties localizedProperties = new Properties();
		for (int i = 0; i < BUNDLE_LOCALIZED_PROPERTIES.length; i++) {
			String key = bundleManifestKeys[i];
			if (key != null) {
				String localizedValue = properties.getProperty(key);
				if (localizedValue != null)
					localizedProperties.put(key, localizedValue);
			}
		}
		return localizedProperties;
	}

}
