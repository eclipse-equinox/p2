/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.equinox.internal.p2.metadata.generator.Activator;
import org.eclipse.equinox.internal.p2.metadata.generator.LocalizationHelper;
import org.eclipse.equinox.internal.p2.metadata.generator.features.SiteCategory;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitPatchDescription;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * @deprecated this class has been renamed to PublisherHelper and the vast majority
 * of the function has been deprecated and moved elsewhere.  See the deprecation notices there
 * for more information.
 */
public class MetadataGeneratorHelper {
	/**
	 * A capability namespace representing the type of Eclipse resource (bundle, feature, source bundle, etc)
	 * @see IProvidedCapability#getNamespace()
	 */
	public static final String NAMESPACE_ECLIPSE_TYPE = "org.eclipse.equinox.p2.eclipse.type"; //$NON-NLS-1$

	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace
	 * representing and OSGi bundle resource
	 * @see IProvidedCapability#getName()
	 */
	public static final String TYPE_ECLIPSE_BUNDLE = "bundle"; //$NON-NLS-1$
	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace
	 * representing a feature
	 */
	public static final String TYPE_ECLIPSE_FEATURE = "feature"; //$NON-NLS-1$

	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace
	 * representing a source bundle
	 */
	public static final String TYPE_ECLIPSE_SOURCE = "source"; //$NON-NLS-1$

	/**
	 * A capability namespace representing the localization (translation)
	 * of strings from a specified IU in a specified locale
	 * @see IProvidedCapability#getNamespace()
	 * TODO: this should be in API, probably in IInstallableUnit
	 */
	public static final String NAMESPACE_IU_LOCALIZATION = "org.eclipse.equinox.p2.localization"; //$NON-NLS-1$

	// Only certain properties in the bundle manifest are assumed to be localized.
	public static final String[] BUNDLE_LOCALIZED_PROPERTIES = {Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_VENDOR, Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_DOCURL, Constants.BUNDLE_UPDATELOCATION};
	public static final int BUNDLE_LOCALIZATION_INDEX = BUNDLE_LOCALIZED_PROPERTIES.length;

	private static final String[] BUNDLE_IU_PROPERTY_MAP = {Constants.BUNDLE_NAME, IInstallableUnit.PROP_NAME, Constants.BUNDLE_DESCRIPTION, IInstallableUnit.PROP_DESCRIPTION, Constants.BUNDLE_VENDOR, IInstallableUnit.PROP_PROVIDER, Constants.BUNDLE_CONTACTADDRESS, IInstallableUnit.PROP_CONTACT, Constants.BUNDLE_DOCURL, IInstallableUnit.PROP_DOC_URL};

	private static final String CAPABILITY_NS_JAVA_PACKAGE = "java.package"; //$NON-NLS-1$
	private static final String CAPABILITY_NS_OSGI_BUNDLE = "osgi.bundle"; //$NON-NLS-1$
	private static final String CAPABILITY_NS_OSGI_FRAGMENT = "osgi.fragment"; //$NON-NLS-1$

	private static final String CAPABILITY_NS_UPDATE_FEATURE = "org.eclipse.update.feature"; //$NON-NLS-1$

	private static final Version DEFAULT_JRE_VERSION = Version.create("1.6"); //$NON-NLS-1$

	public static final String ECLIPSE_FEATURE_CLASSIFIER = "org.eclipse.update.feature"; //$NON-NLS-1$
	public static final String OSGI_BUNDLE_CLASSIFIER = "osgi.bundle"; //$NON-NLS-1$
	public static final String BINARY_ARTIFACT_CLASSIFIER = "binary"; //$NON-NLS-1$

	public static final IMatchExpression INSTALL_FEATURES_FILTER = InstallableUnit.parseFilter("(org.eclipse.update.install.features=true)"); //$NON-NLS-1$

	private static final String IU_NAMESPACE = IInstallableUnit.NAMESPACE_IU_ID;

	private static final String LAUNCHER_ID_PREFIX = "org.eclipse.launcher"; //$NON-NLS-1$

	private static final String ECLIPSE_INSTALL_HANDLER_PROP = "org.eclipse.update.installHandler"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_APPLICATION_PROP = "org.eclipse.update.feature.application"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_PLUGIN_PROP = "org.eclipse.update.feature.plugin"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_EXCLUSIVE_PROP = "org.eclipse.update.feature.exclusive"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_PRIMARY_PROP = "org.eclipse.update.feature.primary"; //$NON-NLS-1$

	//TODO - need to come up with a way to infer launcher version
	private static final Version LAUNCHER_VERSION = Version.createOSGi(1, 0, 0);

	public static final ITouchpointType TOUCHPOINT_NATIVE = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.native", Version.createOSGi(1, 0, 0)); //$NON-NLS-1$
	public static final ITouchpointType TOUCHPOINT_OSGI = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", Version.createOSGi(1, 0, 0)); //$NON-NLS-1$

	public static final IProvidedCapability BUNDLE_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, Version.createOSGi(1, 0, 0));
	public static final IProvidedCapability FEATURE_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_FEATURE, Version.createOSGi(1, 0, 0));
	public static final IProvidedCapability SOURCE_BUNDLE_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_SOURCE, Version.createOSGi(1, 0, 0));

	static final String DEFAULT_BUNDLE_LOCALIZATION = "plugin"; //$NON-NLS-1$

	static final String BUNDLE_ADVICE_FILE = "META-INF/p2.inf"; //$NON-NLS-1$
	static final String ADVICE_INSTRUCTIONS_PREFIX = "instructions."; //$NON-NLS-1$

	static final String NAMESPACE_FLAVOR = "org.eclipse.equinox.p2.flavor"; //$NON-NLS-1$"

	public static IArtifactDescriptor createArtifactDescriptor(IArtifactKey key, File pathOnDisk, boolean asIs, boolean recur) {
		//TODO this size calculation is bogus
		ArtifactDescriptor result = new ArtifactDescriptor(key);
		if (pathOnDisk != null) {
			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, Long.toString(pathOnDisk.length()));
			// TODO - this is wrong but I'm testing a work-around for bug 205842
			result.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(pathOnDisk.length()));
		}
		if (asIs) {
			String md5 = computeMD5(pathOnDisk);
			if (md5 != null)
				result.setProperty(IArtifactDescriptor.DOWNLOAD_MD5, md5);
		}
		return result;
	}

	private static String computeMD5(File file) {
		if (file == null || file.isDirectory() || !file.exists())
			return null;
		MessageDigest md5Checker;
		try {
			md5Checker = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		InputStream fis = null;
		try {
			fis = new BufferedInputStream(new FileInputStream(file));
			int read = -1;
			while ((read = fis.read()) != -1) {
				md5Checker.update((byte) read);
			}
			byte[] digest = md5Checker.digest();
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				if ((digest[i] & 0xFF) < 0x10)
					buf.append('0');
				buf.append(Integer.toHexString(digest[i] & 0xFF));
			}
			return buf.toString();
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	/**
	 * @deprecated moved to AbstractPublishingAction
	 */
	public static IArtifactDescriptor createPack200ArtifactDescriptor(IArtifactKey key, File pathOnDisk, String installSize) {
		//TODO this size calculation is bogus
		ArtifactDescriptor result = new ArtifactDescriptor(key);
		if (pathOnDisk != null) {
			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, installSize);
			// TODO - this is wrong but I'm testing a work-around for bug 205842
			result.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(pathOnDisk.length()));
		}
		IProcessingStepDescriptor[] steps = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
		result.setProcessingSteps(steps);
		result.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
		return result;
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
	public static IInstallableUnit createBundleConfigurationUnit(String iuId, Version iuVersion, boolean isBundleFragment, GeneratorBundleInfo configInfo, String configurationFlavor, String ldapFilter) {
		IMatchExpression filter = ldapFilter == null ? null : InstallableUnit.parseFilter(ldapFilter);
		return createBundleConfigurationUnit(iuId, iuVersion, isBundleFragment, configInfo, configurationFlavor, filter);
	}

	public static IInstallableUnit createBundleConfigurationUnit(String iuId, Version iuVersion, boolean isBundleFragment, GeneratorBundleInfo configInfo, String configurationFlavor, IMatchExpression filter) {
		if (configInfo == null)
			return null;

		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = configurationFlavor + iuId;
		cu.setId(configUnitId);
		cu.setVersion(iuVersion);

		//Indicate the IU to which this CU apply
		cu.setHost(new IRequirement[] { //
				MetadataFactory.createRequirement(CAPABILITY_NS_OSGI_BUNDLE, iuId, new VersionRange(iuVersion, true, Version.MAX_VERSION, true), null, false, false, true), //
						MetadataFactory.createRequirement(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, new VersionRange(Version.createOSGi(1, 0, 0), true, Version.createOSGi(2, 0, 0), false), null, false, false, false)});

		//Adds capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new IProvidedCapability[] {createSelfCapability(configUnitId, iuVersion), MetadataFactory.createProvidedCapability(NAMESPACE_FLAVOR, configurationFlavor, Version.createOSGi(1, 0, 0))});

		Map touchpointData = new HashMap();
		touchpointData.put("install", "installBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("uninstall", "uninstallBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("configure", createConfigScript(configInfo, isBundleFragment)); //$NON-NLS-1$
		touchpointData.put("unconfigure", createUnconfigScript(configInfo, isBundleFragment)); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		cu.setFilter(filter);
		return MetadataFactory.createInstallableUnit(cu);
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
	public static IInstallableUnit createBundleIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key, boolean useNestedAdvice) {
		Map manifestLocalizations = null;
		if (manifest != null && bd.getLocation() != null) {
			manifestLocalizations = getManifestLocalizations(manifest, new File(bd.getLocation()));
		}

		return createBundleIU(bd, manifest, isFolderPlugin, key, manifestLocalizations, useNestedAdvice);
	}

	private static VersionRange computeUpdateRange(org.osgi.framework.Version base) {
		VersionRange updateRange = null;
		if (!base.equals(org.osgi.framework.Version.emptyVersion)) {
			updateRange = new VersionRange(Version.emptyVersion, true, fromOSGiVersion(base), false);
		} else {
			updateRange = VersionRange.emptyRange;
		}
		return updateRange;
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
	public static IInstallableUnit createBundleIU(BundleDescription bd, Map manifest, boolean isFolderPlugin, IArtifactKey key, Map manifestLocalizations, boolean useNestedAdvice) {
		boolean isBinaryBundle = true;
		if (manifest != null && manifest.containsKey("Eclipse-SourceBundle")) { //$NON-NLS-1$
			isBinaryBundle = false;
		}
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(bd.isSingleton());
		iu.setId(bd.getSymbolicName());
		iu.setVersion(fromOSGiVersion(bd.getVersion()));
		iu.setFilter(bd.getPlatformFilter());

		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(bd.getSymbolicName(), computeUpdateRange(bd.getVersion()), IUpdateDescriptor.NORMAL, null));

		boolean isFragment = bd.getHost() != null;
		//		boolean requiresAFragment = isFragment ? false : requireAFragment(bd, manifest);

		//Process the required bundles
		BundleSpecification requiredBundles[] = bd.getRequiredBundles();
		ArrayList reqsDeps = new ArrayList();
		//		if (requiresAFragment)
		//			reqsDeps.add(MetadataFactory.createRequiredCapability(CAPABILITY_TYPE_OSGI_FRAGMENTS, bd.getSymbolicName(), VersionRange.emptyRange, null, false, false));
		if (isFragment)
			reqsDeps.add(MetadataFactory.createRequirement(CAPABILITY_NS_OSGI_BUNDLE, bd.getHost().getName(), fromOSGiVersionRange(bd.getHost().getVersionRange()), null, false, false));
		for (int j = 0; j < requiredBundles.length; j++)
			reqsDeps.add(MetadataFactory.createRequirement(CAPABILITY_NS_OSGI_BUNDLE, requiredBundles[j].getName(), fromOSGiVersionRange(requiredBundles[j].getVersionRange()), null, requiredBundles[j].isOptional(), false));

		// Process the import packages
		ImportPackageSpecification osgiImports[] = bd.getImportPackages();
		for (int i = 0; i < osgiImports.length; i++) {
			// TODO we need to sort out how we want to handle wild-carded dynamic imports - for now we ignore them
			ImportPackageSpecification importSpec = osgiImports[i];
			String importPackageName = importSpec.getName();
			if (importPackageName.indexOf('*') != -1)
				continue;

			VersionRange versionRange = fromOSGiVersionRange(importSpec.getVersionRange());

			//TODO this needs to be refined to take into account all the attribute handled by imports
			reqsDeps.add(MetadataFactory.createRequirement(CAPABILITY_NS_JAVA_PACKAGE, importPackageName, versionRange, null, isOptional(importSpec), false));
		}
		iu.setRequirements((IRequirement[]) reqsDeps.toArray(new IRequirement[reqsDeps.size()]));

		// Create set of provided capabilities
		ArrayList providedCapabilities = new ArrayList();
		providedCapabilities.add(createSelfCapability(bd.getSymbolicName(), fromOSGiVersion(bd.getVersion())));
		providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_OSGI_BUNDLE, bd.getSymbolicName(), fromOSGiVersion(bd.getVersion())));

		// Process the export package
		ExportPackageDescription exports[] = bd.getExportPackages();
		for (int i = 0; i < exports.length; i++) {
			//TODO make sure that we support all the refinement on the exports
			providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_JAVA_PACKAGE, exports[i].getName(), fromOSGiVersion(exports[i].getVersion())));
		}
		// Here we add a bundle capability to identify bundles
		if (isBinaryBundle)
			providedCapabilities.add(BUNDLE_CAPABILITY);
		else
			providedCapabilities.add(SOURCE_BUNDLE_CAPABILITY);

		if (isFragment)
			providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_OSGI_FRAGMENT, bd.getHost().getName(), fromOSGiVersion(bd.getVersion())));

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

		iu.setCapabilities((IProvidedCapability[]) providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));

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

		if (useNestedAdvice)
			mergeInstructionsAdvice(touchpointData, getBundleAdvice(bd.getLocation(), BUNDLE_ADVICE_FILE));

		iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		return MetadataFactory.createInstallableUnit(iu);
	}

	/**
	 * @deprecated moved to AdviceFileAdvice
	 */
	public static void mergeInstructionsAdvice(Map touchpointData, Map bundleAdvice) {
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

	/**
	 * @deprecated moved to BundlesAction
	 */
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
	/**
	 * @deprecated moved to BundlesAction
	 */
	private static IInstallableUnitFragment createLocalizationFragmentOfHost(BundleDescription bd, String hostId, String[] hostManifestValues, Map hostLocalizations) {
		InstallableUnitFragmentDescription fragment = new MetadataFactory.InstallableUnitFragmentDescription();
		String fragmentId = makeHostLocalizationFragmentId(bd.getSymbolicName());
		fragment.setId(fragmentId);
		fragment.setVersion(fromOSGiVersion(bd.getVersion())); // TODO: is this a meaningful version?

		HostSpecification hostSpec = bd.getHost();
		IRequirement[] hostReqs = new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, hostSpec.getName(), fromOSGiVersionRange(hostSpec.getVersionRange()), null, false, false, false)};
		fragment.setHost(hostReqs);

		fragment.setSingleton(true);
		fragment.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());

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
		fragment.setCapabilities((IProvidedCapability[]) providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));

		return MetadataFactory.createInstallableUnitFragment(fragment);
	}

	/**
	 * @param id
	 * @return the id for the iu fragment containing the localized properties
	 * 		   for the bundle with the given id
	 */
	//	private static String makeBundleLocalizationFragmentId(String id) {
	//		return id + ".translated_properties"; //$NON-NLS-1$
	//	}
	/**
	 * @param id
	 * @return the id for the iu fragment containing localized properties
	 * 		   for the fragment with the given id.
	 */
	/**
	 * @deprecated moved to BundlesAction
	 */
	private static String makeHostLocalizationFragmentId(String id) {
		return id + ".translated_host_properties"; //$NON-NLS-1$
	}

	private static IProvidedCapability makeTranslationCapability(String hostId, Locale locale) {
		return MetadataFactory.createProvidedCapability(NAMESPACE_IU_LOCALIZATION, locale.toString(), Version.createOSGi(1, 0, 0));
	}

	/**
	 * Creates an IU corresponding to an update site category
	 * @param category The category descriptor
	 * @param featureIUs The IUs of the features that belong to the category
	 * @param parentCategory The parent category, or <code>null</code>
	 * @return an IU representing the category
	 * @deprecated moved to SiteXMLAction
	 */
	public static IInstallableUnit createCategoryIU(SiteCategory category, Set featureIUs, IInstallableUnit parentCategory) {
		InstallableUnitDescription cat = new MetadataFactory.InstallableUnitDescription();
		cat.setSingleton(true);
		String categoryId = category.getName();
		cat.setId(categoryId);
		cat.setVersion(Version.emptyVersion);
		cat.setProperty(IInstallableUnit.PROP_NAME, category.getLabel());
		cat.setProperty(IInstallableUnit.PROP_DESCRIPTION, category.getDescription());

		ArrayList reqsConfigurationUnits = new ArrayList(featureIUs.size());
		for (Iterator iterator = featureIUs.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			reqsConfigurationUnits.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter() == null ? null : iu.getFilter(), false, false));
		}
		//note that update sites don't currently support nested categories, but it may be useful to add in the future
		if (parentCategory != null) {
			reqsConfigurationUnits.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, parentCategory.getId(), VersionRange.emptyRange, parentCategory.getFilter() == null ? null : parentCategory.getFilter(), false, false));
		}
		cat.setRequirements((IRequirement[]) reqsConfigurationUnits.toArray(new IRequirement[reqsConfigurationUnits.size()]));

		// Create set of provided capabilities
		ArrayList providedCapabilities = new ArrayList();
		providedCapabilities.add(createSelfCapability(categoryId, Version.emptyVersion));

		Map localizations = category.getLocalizations();
		if (localizations != null) {
			for (Iterator iter = localizations.keySet().iterator(); iter.hasNext();) {
				Locale locale = (Locale) iter.next();
				Properties translatedStrings = (Properties) localizations.get(locale);
				Enumeration propertyKeys = translatedStrings.propertyNames();
				while (propertyKeys.hasMoreElements()) {
					String nextKey = (String) propertyKeys.nextElement();
					cat.setProperty(locale.toString() + '.' + nextKey, translatedStrings.getProperty(nextKey));
				}
				providedCapabilities.add(makeTranslationCapability(categoryId, locale));
			}
		}

		cat.setCapabilities((IProvidedCapability[]) providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));

		cat.setArtifacts(new IArtifactKey[0]);
		cat.setProperty(InstallableUnitDescription.PROP_TYPE_CATEGORY, "true"); //$NON-NLS-1$
		return MetadataFactory.createInstallableUnit(cat);
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
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

	/**
	 * @deprecated moved to BundlesAction
	 */
	private static String createDefaultBundleConfigScript(GeneratorBundleInfo configInfo) {
		return createConfigScript(configInfo, false);
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
	public static IInstallableUnit createDefaultBundleConfigurationUnit(GeneratorBundleInfo configInfo, GeneratorBundleInfo unconfigInfo, String configurationFlavor) {
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = createDefaultConfigUnitId(OSGI_BUNDLE_CLASSIFIER, configurationFlavor);
		cu.setId(configUnitId);
		Version configUnitVersion = Version.createOSGi(1, 0, 0);
		cu.setVersion(configUnitVersion);

		// Add capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new IProvidedCapability[] {createSelfCapability(configUnitId, configUnitVersion), MetadataFactory.createProvidedCapability(NAMESPACE_FLAVOR, configurationFlavor, Version.createOSGi(1, 0, 0))});

		// Create a required capability on bundles
		IRequirement[] reqs = new IRequirement[] {MetadataFactory.createRequirement(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, VersionRange.emptyRange, null, false, true, false)};
		cu.setHost(reqs);
		Map touchpointData = new HashMap();

		touchpointData.put("install", "installBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("uninstall", "uninstallBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("configure", createDefaultBundleConfigScript(configInfo)); //$NON-NLS-1$
		touchpointData.put("unconfigure", createDefaultBundleUnconfigScript(unconfigInfo)); //$NON-NLS-1$

		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		return MetadataFactory.createInstallableUnit(cu);
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
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
		Version configUnitVersion = Version.createOSGi(1, 0, 0);
		cu.setVersion(configUnitVersion);

		// Add capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new IProvidedCapability[] {createSelfCapability(configUnitId, configUnitVersion), MetadataFactory.createProvidedCapability(NAMESPACE_FLAVOR, configurationFlavor, Version.createOSGi(1, 0, 0))});

		// Create a required capability on features
		IRequirement[] reqs = new IRequirement[] {MetadataFactory.createRequirement(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_FEATURE, VersionRange.emptyRange, null, true, true, false)};
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
		Version configUnitVersion = Version.createOSGi(1, 0, 0);
		cu.setVersion(configUnitVersion);

		// Add capabilities for fragment, self, and describing the flavor supported
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new IProvidedCapability[] {createSelfCapability(configUnitId, configUnitVersion), MetadataFactory.createProvidedCapability(NAMESPACE_FLAVOR, configurationFlavor, Version.createOSGi(1, 0, 0))});

		// Create a required capability on source providers
		IRequirement[] reqs = new IRequirement[] {MetadataFactory.createRequirement(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_SOURCE, VersionRange.emptyRange, null, true, true, false)};
		cu.setHost(reqs);
		Map touchpointData = new HashMap();

		touchpointData.put("install", "addSourceBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("uninstall", "removeSourceBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		return MetadataFactory.createInstallableUnit(cu);
	}

	/**
	 * @deprecated moved to FeaturesAction
	 */
	public static IArtifactKey createFeatureArtifactKey(String fsn, String version) {
		return new ArtifactKey(ECLIPSE_FEATURE_CLASSIFIER, fsn, Version.create(version));
	}

	/**
	 * @deprecated moved to FeaturesAction
	 */
	public static IInstallableUnit createFeatureJarIU(Feature feature, boolean isExploded) {
		return createFeatureJarIU(feature, isExploded, null);
	}

	/**
	 * @deprecated moved to FeaturesAction
	 */
	public static IInstallableUnit createFeatureJarIU(Feature feature, boolean isExploded, Properties extraProperties) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		String id = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/false);
		iu.setId(id);
		Version version = fromOSGiVersion(new org.osgi.framework.Version(feature.getVersion()));
		iu.setVersion(version);
		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, computeUpdateRange(new org.osgi.framework.Version(feature.getVersion())), IUpdateDescriptor.NORMAL, null));
		iu.setProperty(IInstallableUnit.PROP_NAME, feature.getLabel());
		if (feature.getDescription() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION, feature.getDescription());
		if (feature.getDescriptionURL() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION_URL, feature.getDescriptionURL());
		if (feature.getProviderName() != null)
			iu.setProperty(IInstallableUnit.PROP_PROVIDER, feature.getProviderName());
		if (feature.getLicense() != null)
			iu.setLicenses(new ILicense[] {MetadataFactory.createLicense(toURIOrNull(feature.getLicenseURL()), feature.getLicense())});
		if (feature.getCopyright() != null)
			iu.setCopyright(MetadataFactory.createCopyright(toURIOrNull(feature.getCopyrightURL()), feature.getCopyright()));
		if (feature.getApplication() != null)
			iu.setProperty(UPDATE_FEATURE_APPLICATION_PROP, feature.getApplication());
		if (feature.getPlugin() != null)
			iu.setProperty(UPDATE_FEATURE_PLUGIN_PROP, feature.getPlugin());
		if (feature.isExclusive())
			iu.setProperty(UPDATE_FEATURE_EXCLUSIVE_PROP, Boolean.TRUE.toString());
		if (feature.isPrimary())
			iu.setProperty(UPDATE_FEATURE_PRIMARY_PROP, Boolean.TRUE.toString());

		// The required capabilities are not specified at this level because we don't want the feature jar to be attractive to install.

		iu.setTouchpointType(TOUCHPOINT_OSGI);
		iu.setFilter(INSTALL_FEATURES_FILTER);
		iu.setSingleton(true);

		if (feature.getInstallHandler() != null && feature.getInstallHandler().trim().length() > 0) {
			String installHandlerProperty = "handler=" + feature.getInstallHandler(); //$NON-NLS-1$

			if (feature.getInstallHandlerLibrary() != null)
				installHandlerProperty += ", library=" + feature.getInstallHandlerLibrary(); //$NON-NLS-1$

			if (feature.getInstallHandlerURL() != null)
				installHandlerProperty += ", url=" + feature.getInstallHandlerURL(); //$NON-NLS-1$

			iu.setProperty(ECLIPSE_INSTALL_HANDLER_PROP, installHandlerProperty);
		}

		// Create set of provided capabilities
		ArrayList providedCapabilities = new ArrayList();
		providedCapabilities.add(createSelfCapability(id, version));
		providedCapabilities.add(FEATURE_CAPABILITY);
		providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_UPDATE_FEATURE, feature.getId(), version));

		iu.setArtifacts(new IArtifactKey[] {createFeatureArtifactKey(feature.getId(), version.toString())});

		if (isExploded) {
			// Define the immutable metadata for this IU. In this case immutable means
			// that this is something that will not impact the configuration.
			Map touchpointData = new HashMap();
			touchpointData.put("zipped", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		}

		Map localizations = feature.getLocalizations();
		if (localizations != null) {
			for (Iterator iter = localizations.keySet().iterator(); iter.hasNext();) {
				Locale locale = (Locale) iter.next();
				Properties translatedStrings = (Properties) localizations.get(locale);
				Enumeration propertyKeys = translatedStrings.propertyNames();
				while (propertyKeys.hasMoreElements()) {
					String nextKey = (String) propertyKeys.nextElement();
					iu.setProperty(locale.toString() + '.' + nextKey, translatedStrings.getProperty(nextKey));
				}
				providedCapabilities.add(makeTranslationCapability(id, locale));
			}
		}

		iu.setCapabilities((IProvidedCapability[]) providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));

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
	 * @deprecated moved to FeaturesAction
	 */
	public static IInstallableUnit createGroupIU(Feature feature, IInstallableUnit featureIU) {
		return createGroupIU(feature, featureIU, null, true);
	}

	public static IInstallableUnit createGroupIU(Feature feature, IInstallableUnit featureIU, Properties extraProperties, boolean transformIds) {
		if (isPatch(feature))
			return createPatchIU(feature, featureIU, extraProperties);
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		String id = feature.getId();
		if (transformIds)
			id = getTransformedId(id, /*isPlugin*/false, /*isGroup*/true);
		iu.setId(id);
		Version version = fromOSGiVersion(new org.osgi.framework.Version(feature.getVersion()));
		iu.setVersion(version);
		iu.setProperty(IInstallableUnit.PROP_NAME, feature.getLabel());
		if (feature.getDescription() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION, feature.getDescription());
		if (feature.getDescriptionURL() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION_URL, feature.getDescriptionURL());
		if (feature.getProviderName() != null)
			iu.setProperty(IInstallableUnit.PROP_PROVIDER, feature.getProviderName());
		if (feature.getLicense() != null)
			iu.setLicenses(new ILicense[] {MetadataFactory.createLicense(toURIOrNull(feature.getLicenseURL()), feature.getLicense())});
		if (feature.getCopyright() != null)
			iu.setCopyright(MetadataFactory.createCopyright(toURIOrNull(feature.getCopyrightURL()), feature.getCopyright()));
		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, computeUpdateRange(new org.osgi.framework.Version(feature.getVersion())), IUpdateDescriptor.NORMAL, null));

		FeatureEntry entries[] = feature.getEntries();
		IRequirement[] required = new IRequirement[entries.length + (featureIU == null ? 0 : 1)];
		for (int i = 0; i < entries.length; i++) {
			VersionRange range = getVersionRange(entries[i]);
			String requiredId = entries[i].getId();
			if (transformIds)
				requiredId = getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true);
			required[i] = MetadataFactory.createRequirement(IU_NAMESPACE, requiredId, range, getFilter(entries[i]), entries[i].isOptional(), false);
		}
		// the feature IU could be null if we are just generating a feature structure rather than
		// actual features.
		if (featureIU != null)
			required[entries.length] = MetadataFactory.createRequirement(IU_NAMESPACE, featureIU.getId(), new VersionRange(featureIU.getVersion(), true, featureIU.getVersion(), true), INSTALL_FEATURES_FILTER, false, false);
		iu.setRequirements(required);
		iu.setTouchpointType(ITouchpointType.NONE);
		iu.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		// TODO: shouldn't the filter for the group be constructed from os, ws, arch, nl
		// 		 of the feature?
		// iu.setFilter(filter);

		// Create set of provided capabilities
		ArrayList providedCapabilities = new ArrayList();
		providedCapabilities.add(createSelfCapability(id, version));

		Map localizations = feature.getLocalizations();
		if (localizations != null) {
			for (Iterator iter = localizations.keySet().iterator(); iter.hasNext();) {
				Locale locale = (Locale) iter.next();
				Properties translatedStrings = (Properties) localizations.get(locale);
				Enumeration propertyKeys = translatedStrings.propertyNames();
				while (propertyKeys.hasMoreElements()) {
					String nextKey = (String) propertyKeys.nextElement();
					iu.setProperty(locale.toString() + '.' + nextKey, translatedStrings.getProperty(nextKey));
				}
				providedCapabilities.add(makeTranslationCapability(id, locale));
			}
		}

		iu.setCapabilities((IProvidedCapability[]) providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));

		if (extraProperties != null) {
			Enumeration e = extraProperties.propertyNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				iu.setProperty(name, extraProperties.getProperty(name));
			}
		}

		return MetadataFactory.createInstallableUnit(iu);
	}

	public static IInstallableUnit createPatchIU(Feature feature, IInstallableUnit featureIU, Properties extraProperties) {
		InstallableUnitPatchDescription iu = new MetadataFactory.InstallableUnitPatchDescription();
		String id = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/true);
		iu.setId(id);
		Version version = fromOSGiVersion(new org.osgi.framework.Version(feature.getVersion()));
		iu.setVersion(version);
		iu.setProperty(IInstallableUnit.PROP_NAME, feature.getLabel());
		if (feature.getDescription() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION, feature.getDescription());
		if (feature.getDescriptionURL() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION_URL, feature.getDescriptionURL());
		if (feature.getProviderName() != null)
			iu.setProperty(IInstallableUnit.PROP_PROVIDER, feature.getProviderName());
		if (feature.getLicense() != null)
			iu.setLicenses(new ILicense[] {MetadataFactory.createLicense(toURIOrNull(feature.getLicenseURL()), feature.getLicense())});
		if (feature.getCopyright() != null)
			iu.setCopyright(MetadataFactory.createCopyright(toURIOrNull(feature.getCopyrightURL()), feature.getCopyright()));
		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, computeUpdateRange(new org.osgi.framework.Version(feature.getVersion())), IUpdateDescriptor.NORMAL, null));

		FeatureEntry entries[] = feature.getEntries();
		ArrayList applicabilityScope = new ArrayList();
		ArrayList patchRequirements = new ArrayList();
		ArrayList requirementChanges = new ArrayList();
		for (int i = 0; i < entries.length; i++) {
			VersionRange range = getVersionRange(entries[i]);
			IRequirement req = MetadataFactory.createRequirement(IU_NAMESPACE, getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true), range, getFilter(entries[i]), entries[i].isOptional(), false);
			if (entries[i].isRequires()) {
				applicabilityScope.add(req);
				if (applicabilityScope.size() == 1) {
					iu.setLifeCycle(MetadataFactory.createRequirement(IU_NAMESPACE, getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true), range, null, false, false, false));
				}
				continue;
			}
			if (entries[i].isPlugin()) {
				IRequirement from = MetadataFactory.createRequirement(IU_NAMESPACE, getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true), VersionRange.emptyRange, getFilter(entries[i]), entries[i].isOptional(), false);
				requirementChanges.add(MetadataFactory.createRequirementChange(from, req));
				continue;
			}
			patchRequirements.add(req);
		}
		//Always add a requirement on the IU containing the feature jar
		patchRequirements.add(MetadataFactory.createRequirement(IU_NAMESPACE, featureIU.getId(), new VersionRange(featureIU.getVersion(), true, featureIU.getVersion(), true), INSTALL_FEATURES_FILTER, false, false));
		iu.setRequirements((IRequirement[]) patchRequirements.toArray(new IRequirement[patchRequirements.size()]));
		iu.setApplicabilityScope(new IRequirement[][] {(IRequirement[]) applicabilityScope.toArray(new IRequirement[applicabilityScope.size()])});
		iu.setRequirementChanges((IRequirementChange[]) requirementChanges.toArray(new IRequirementChange[requirementChanges.size()]));

		//Generate lifecycle
		//		IRequirement lifeCycle = null;
		//		if (applicabilityScope.size() > 0) {
		//			IRequirement req = (IRequirement) applicabilityScope.get(0);
		//		}

		iu.setTouchpointType(ITouchpointType.NONE);
		iu.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		iu.setProperty(InstallableUnitDescription.PROP_TYPE_PATCH, Boolean.TRUE.toString());
		// TODO: shouldn't the filter for the group be constructed from os, ws, arch, nl
		// 		 of the feature?
		// iu.setFilter(filter);

		// Create set of provided capabilities
		ArrayList providedCapabilities = new ArrayList();
		providedCapabilities.add(createSelfCapability(id, version));

		Map localizations = feature.getLocalizations();
		if (localizations != null) {
			for (Iterator iter = localizations.keySet().iterator(); iter.hasNext();) {
				Locale locale = (Locale) iter.next();
				Properties translatedStrings = (Properties) localizations.get(locale);
				Enumeration propertyKeys = translatedStrings.propertyNames();
				while (propertyKeys.hasMoreElements()) {
					String nextKey = (String) propertyKeys.nextElement();
					iu.setProperty(locale.toString() + '.' + nextKey, translatedStrings.getProperty(nextKey));
				}
				providedCapabilities.add(makeTranslationCapability(id, locale));
			}
		}

		iu.setCapabilities((IProvidedCapability[]) providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));

		if (extraProperties != null) {
			Enumeration e = extraProperties.propertyNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				iu.setProperty(name, extraProperties.getProperty(name));
			}
		}

		return MetadataFactory.createInstallableUnitPatch(iu);
	}

	private static boolean isPatch(Feature feature) {
		FeatureEntry[] entries = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isPatch())
				return true;
		}
		return false;
	}

	/**
	 * Creates IUs and artifact descriptors for the JRE.  The resulting IUs are added
	 * to the given set, and the resulting artifact descriptor, if any, is returned.
	 * If the jreLocation is <code>null</code>, default information is generated.
	 */
	/**
	 * @deprecated moved to JREAction
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
		cu.setHost(new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, new VersionRange(version, true, Version.MAX_VERSION, true), null, false, false)});
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new IProvidedCapability[] {createSelfCapability(configId, version)});
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
	 * @deprecated use the EquinoxExecutablesAction instead
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
		iu.setCapabilities(new IProvidedCapability[] {createSelfCapability(launcherId, LAUNCHER_VERSION)});
		iu.setTouchpointType(TOUCHPOINT_NATIVE);
		resultantIUs.add(MetadataFactory.createInstallableUnit(iu));

		//Create the CU
		InstallableUnitFragmentDescription cu = new InstallableUnitFragmentDescription();
		String configUnitId = configurationFlavor + launcherId;
		cu.setId(configUnitId);
		cu.setVersion(LAUNCHER_VERSION);
		cu.setHost(new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, launcherId, new VersionRange(LAUNCHER_VERSION, true, Version.MAX_VERSION, true), null, false, false)});
		cu.setProperty(InstallableUnitDescription.PROP_TYPE_FRAGMENT, Boolean.TRUE.toString());
		cu.setCapabilities(new IProvidedCapability[] {createSelfCapability(configUnitId, LAUNCHER_VERSION)});
		cu.setTouchpointType(TOUCHPOINT_NATIVE);
		Map touchpointData = new HashMap();
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
		if (!info.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_WIN32)) {
			if (info.getOS().equals(org.eclipse.osgi.service.environment.Constants.OS_MACOSX)) {
				configurationData += " chmod(targetDir:${installFolder}/Eclipse.app/Contents/MacOS, targetFile:eclipse, permissions:755);"; //$NON-NLS-1$
				generateLauncherSetter("Eclipse", launcherId, LAUNCHER_VERSION, "macosx", null, null, resultantIUs); //$NON-NLS-1$//$NON-NLS-2$
			} else
				configurationData += " chmod(targetDir:${installFolder}, targetFile:" + launcher.getName() + ", permissions:755);"; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			generateLauncherSetter("eclipse", launcherId, LAUNCHER_VERSION, "win32", null, null, resultantIUs); //$NON-NLS-1$ //$NON-NLS-2$
		}
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		resultantIUs.add(MetadataFactory.createInstallableUnitFragment(cu));

		//Create the artifact descriptor
		return createArtifactDescriptor(key, launcher, false, true);
	}

	/**
	 * @deprecated moved to EquinoxExecutablesAction
	 */
	public static IInstallableUnit generateLauncherSetter(String launcherName, String iuId, Version version, String os, String ws, String arch, Set result) {
		InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
		iud.setId(iuId + '.' + launcherName);
		iud.setVersion(version);
		iud.setTouchpointType(MetadataGeneratorHelper.TOUCHPOINT_OSGI);
		iud.setCapabilities(new IProvidedCapability[] {createSelfCapability(iuId + '.' + launcherName, version)});

		if (os != null || ws != null || arch != null) {
			String filterOs = os != null ? "(osgi.os=" + os + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterWs = ws != null ? "(osgi.ws=" + ws + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterArch = arch != null ? "(osgi.arch=" + arch + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			iud.setFilter("(& " + filterOs + filterWs + filterArch + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Map touchpointData = new HashMap();
		touchpointData.put("configure", "setLauncherName(name:" + launcherName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		touchpointData.put("unconfigure", "setLauncherName()"); //$NON-NLS-1$ //$NON-NLS-2$
		iud.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		IInstallableUnit iu = MetadataFactory.createInstallableUnit(iud);
		result.add(iu);
		return iu;
	}

	public static IProvidedCapability createSelfCapability(String installableUnitId, Version installableUnitVersion) {
		return MetadataFactory.createProvidedCapability(IU_NAMESPACE, installableUnitId, installableUnitVersion);
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
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

	/**
	 * @deprecated moved to JREAction
	 */
	private static IProvidedCapability[] generateJRECapability(String installableUnitId, Version installableUnitVersion, InputStream profileStream) {
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
			IProvidedCapability[] exportedPackageAsCapabilities = new IProvidedCapability[jrePackages.length + 1];
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
		return new IProvidedCapability[0];
	}

	/**
	 * @deprecated moved to JREAction
	 */
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
			version = Version.create("1.6"); //$NON-NLS-1$
		} else if (profileName.indexOf("1.5") > 0) { //$NON-NLS-1$
			version = Version.create("1.5"); //$NON-NLS-1$
		} else if (profileName.indexOf("1.4") > 0) { //$NON-NLS-1$
			version = Version.create("1.4"); //$NON-NLS-1$
		}
		iu.setVersion(version);
		try {
			iu.setCapabilities(generateJRECapability(installableUnitId, installableUnitVersion, new FileInputStream(profiles[0])));
		} catch (FileNotFoundException e) {
			//Shouldn't happen, but ignore and fall through to use default
		}
	}

	/**
	 * @deprecated moved to FeaturesAction
	 */
	public static IMatchExpression getFilter(FeatureEntry entry) {
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
		return InstallableUnit.parseFilter(result.toString());
	}

	/**
	 * @deprecated moved to FeaturesAction
	 */
	public static String getTransformedId(String original, boolean isPlugin, boolean isGroup) {
		return (isPlugin ? original : original + (isGroup ? ".feature.group" : ".feature.jar")); //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * @deprecated moved to FeaturesAction
	 */
	public static VersionRange getVersionRange(FeatureEntry entry) {
		String versionSpec = entry.getVersion();
		if (versionSpec == null)
			return VersionRange.emptyRange;
		Version version = Version.create(versionSpec);
		if (version.equals(Version.emptyVersion))
			return VersionRange.emptyRange;
		if (!entry.isRequires())
			return new VersionRange(version, true, version, true);
		String match = entry.getMatch();

		org.osgi.framework.Version osgiVersion = toOSGiVersion(version);
		if (match == null || match.equals("compatible")) { //$NON-NLS-1$
			Version upper = Version.createOSGi(osgiVersion.getMajor() + 1, 0, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("perfect")) //$NON-NLS-1$
			return new VersionRange(version, true, version, true);
		if (match.equals("equivalent")) { //$NON-NLS-1$
			Version upper = Version.createOSGi(osgiVersion.getMajor(), osgiVersion.getMinor() + 1, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("greaterOrEqual")) //$NON-NLS-1$
			return new VersionRange(version, true, new VersionRange(null).getMaximum(), true);
		return null;
	}

	/**
	 * @deprecated moved to AdviceFileAdvice
	 */
	public static Map getBundleAdvice(String bundleLocation, String suffixLocation) {
		if (bundleLocation == null)
			return Collections.EMPTY_MAP;

		File bundle = new File(bundleLocation);
		if (!bundle.exists())
			return Collections.EMPTY_MAP;

		ZipFile jar = null;
		InputStream stream = null;
		if (bundle.isDirectory()) {
			File adviceFile = new File(bundle, suffixLocation);
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
				ZipEntry entry = jar.getEntry(suffixLocation);
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

	/**
	 * @deprecated moved to BundlesAction
	 */
	private static boolean isOptional(ImportPackageSpecification importedPackage) {
		if (importedPackage.getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_DYNAMIC) || importedPackage.getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_OPTIONAL))
			return true;
		return false;
	}

	/**
	 * @deprecated moved to BundlesAction
	 */
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

	/**
	 * Returns a URI corresponding to the given URL in string form, or null
	 * if a well formed URI could not be created.
	 */
	private static URI toURIOrNull(String url) {
		if (url == null)
			return null;
		try {
			return URIUtil.fromString(url);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	// Return a map from locale to property set for the manifest localizations
	// from the given bundle directory and given bundle localization path/name
	// manifest property value.
	/**
	 * @deprecated moved to BundlesAction
	 */
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

	/**
	 * @deprecated moved to BundlesAction
	 */
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
	/**
	 * @deprecated moved to BundlesAction
	 */
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

	/**
	 * Convert <code>version</code> into its OSGi equivalent if possible.
	 *
	 * @param version The version to convert. Can be <code>null</code>
	 * @return The converted version or <code>null</code> if the argument was <code>null</code>
	 * @throws UnsupportedOperationException if the version could not be converted into an OSGi version
	 */
	public static org.osgi.framework.Version toOSGiVersion(Version version) {
		if (version == null)
			return null;
		if (version == Version.emptyVersion)
			return org.osgi.framework.Version.emptyVersion;
		if (version == Version.MAX_VERSION)
			return new org.osgi.framework.Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

		BasicVersion bv = (BasicVersion) version;
		return new org.osgi.framework.Version(bv.getMajor(), bv.getMinor(), bv.getMicro(), bv.getQualifier());
	}

	/**
	 * Create an omni version from an OSGi <code>version</code>.
	 * @param version The OSGi version. Can be <code>null</code>.
	 * @return The created omni version
	 */
	public static Version fromOSGiVersion(org.osgi.framework.Version version) {
		if (version == null)
			return null;
		if (version.getMajor() == Integer.MAX_VALUE && version.getMicro() == Integer.MAX_VALUE && version.getMicro() == Integer.MAX_VALUE)
			return Version.MAX_VERSION;
		return Version.createOSGi(version.getMajor(), version.getMinor(), version.getMicro(), version.getQualifier());
	}

	public static org.eclipse.osgi.service.resolver.VersionRange toOSGiVersionRange(VersionRange range) {
		if (range.equals(VersionRange.emptyRange))
			return org.eclipse.osgi.service.resolver.VersionRange.emptyRange;
		return new org.eclipse.osgi.service.resolver.VersionRange(toOSGiVersion(range.getMinimum()), range.getIncludeMinimum(), toOSGiVersion(range.getMaximum()), range.getIncludeMinimum());
	}

	public static VersionRange fromOSGiVersionRange(org.eclipse.osgi.service.resolver.VersionRange range) {
		if (range.equals(org.eclipse.osgi.service.resolver.VersionRange.emptyRange))
			return VersionRange.emptyRange;
		return new VersionRange(fromOSGiVersion(range.getMinimum()), range.getIncludeMinimum(), fromOSGiVersion(range.getMaximum()), range.getIncludeMaximum());
	}
};
