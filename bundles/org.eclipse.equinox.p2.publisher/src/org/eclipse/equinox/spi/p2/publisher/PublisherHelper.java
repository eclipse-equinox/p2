/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genuitec, LLC - added license support
 *     Code 9 - Ongoing development
 *     SAP AG - consolidation of publishers for PDE formats
 *     Mykola Nikishov - multiple artifact checksums
 *******************************************************************************/
package org.eclipse.equinox.spi.p2.publisher;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumUtilities;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.BasicVersion;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.osgi.framework.Constants;

/**
 * This class was originally the MetadataGeneratorHelper from the Generator.
 * Much of the code has been deprecated and will be removed.
 */
public class PublisherHelper {
	/**
	 * A capability namespace representing the type of Eclipse resource (bundle, feature, source bundle, etc)
	 * @see IProvidedCapability#getNamespace()
	 */
	public static final String NAMESPACE_ECLIPSE_TYPE = "org.eclipse.equinox.p2.eclipse.type"; //$NON-NLS-1$

	public static final String NAMESPACE_FLAVOR = "org.eclipse.equinox.p2.flavor"; //$NON-NLS-1$"
	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace
	 * representing a feature
	 * @see IProvidedCapability#getName()
	 */
	public static final String TYPE_ECLIPSE_FEATURE = "feature"; //$NON-NLS-1$

	/**
	 * A capability name in the {@link #NAMESPACE_ECLIPSE_TYPE} namespace
	 * representing a source bundle
	 * @see IProvidedCapability#getName()
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
	public static final String[] BUNDLE_LOCALIZED_PROPERTIES = {Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_VENDOR, Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_DOCURL, Constants.BUNDLE_UPDATELOCATION, Constants.BUNDLE_LOCALIZATION};

	public static final String CAPABILITY_NS_JAVA_PACKAGE = "java.package"; //$NON-NLS-1$

	public static final String CAPABILITY_NS_UPDATE_FEATURE = "org.eclipse.update.feature"; //$NON-NLS-1$

	public static final String ECLIPSE_FEATURE_CLASSIFIER = "org.eclipse.update.feature"; //$NON-NLS-1$
	public static final String OSGI_BUNDLE_CLASSIFIER = "osgi.bundle"; //$NON-NLS-1$
	public static final String BINARY_ARTIFACT_CLASSIFIER = "binary"; //$NON-NLS-1$

	public static final String INSTALL_FEATURES_FILTER = "(org.eclipse.update.install.features=true)"; //$NON-NLS-1$

	public static final String IU_NAMESPACE = IInstallableUnit.NAMESPACE_IU_ID;

	public static final String ECLIPSE_INSTALL_HANDLER_PROP = "org.eclipse.update.installHandler"; //$NON-NLS-1$

	public static final ITouchpointType TOUCHPOINT_NATIVE = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.native", Version.createOSGi(1, 0, 0)); //$NON-NLS-1$
	public static final ITouchpointType TOUCHPOINT_OSGI = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", Version.createOSGi(1, 0, 0)); //$NON-NLS-1$

	public static final IProvidedCapability FEATURE_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_FEATURE, Version.createOSGi(1, 0, 0));

	public static IArtifactDescriptor createArtifactDescriptor(IArtifactKey key, File pathOnDisk) {
		return createArtifactDescriptor(null, null, key, pathOnDisk);
	}

	/**
	 * Creates an artifact descriptor for the given key and path.
	 * @param info the publisher info
	 * @param key the key of the artifact to publish
	 * @param pathOnDisk the path of the artifact on disk
	 * @return a new artifact descriptor
	 */
	public static IArtifactDescriptor createArtifactDescriptor(IPublisherInfo info, IArtifactKey key, File pathOnDisk) {
		return createArtifactDescriptor(info, info.getArtifactRepository(), key, pathOnDisk);
	}

	private static IArtifactDescriptor createArtifactDescriptor(IPublisherInfo info, IArtifactRepository artifactRepo, IArtifactKey key, File pathOnDisk) {
		IArtifactDescriptor result = artifactRepo != null ? artifactRepo.createArtifactDescriptor(key) : new ArtifactDescriptor(key);
		if (result instanceof ArtifactDescriptor) {
			if (pathOnDisk != null && pathOnDisk.isFile()) {
				ArtifactDescriptor descriptor = (ArtifactDescriptor) result;
				descriptor.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, Long.toString(pathOnDisk.length()));
				descriptor.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(pathOnDisk.length()));

				boolean generateChecksums = info == null || isArtifactGenerateChecksums(info);
				if (generateChecksums) {
					calculateChecksums(pathOnDisk, descriptor);
				}
			}
		}
		return result;
	}

	private static void calculateChecksums(File pathOnDisk, ArtifactDescriptor descriptor) {
		// TODO disable specific algorithms
		List<String> checksumsToSkip = Collections.emptyList();
		Map<String, String> checksums = new HashMap<>();
		IStatus status = ChecksumUtilities.calculateChecksums(pathOnDisk, checksums, checksumsToSkip);
		if (!status.isOK()) {
			// TODO handle errors in some way
			LogHelper.log(status);
		}
		Map<String, String> checksumProperties = ChecksumUtilities.checksumsToProperties(IArtifactDescriptor.DOWNLOAD_CHECKSUM, checksums);
		descriptor.addProperties(checksumProperties);
	}

	public static IProvidedCapability makeTranslationCapability(String hostId, Locale locale) {
		return MetadataFactory.createProvidedCapability(NAMESPACE_IU_LOCALIZATION, locale.toString(), Version.createOSGi(1, 0, 0));
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
		Map<String, String> touchpointData = new HashMap<>();
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
		Map<String, String> touchpointData = new HashMap<>();

		touchpointData.put("install", "addSourceBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		touchpointData.put("uninstall", "removeSourceBundle(bundle:${artifact})"); //$NON-NLS-1$ //$NON-NLS-2$
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		return MetadataFactory.createInstallableUnit(cu);
	}

	public static ArtifactKey createBinaryArtifactKey(String id, Version version) {
		return new ArtifactKey(BINARY_ARTIFACT_CLASSIFIER, id, version);
	}

	public static IProvidedCapability createSelfCapability(String installableUnitId, Version installableUnitVersion) {
		return MetadataFactory.createProvidedCapability(IU_NAMESPACE, installableUnitId, installableUnitVersion);
	}

	/**
	 * Convert <code>version</code> into its OSGi equivalent if possible.
	 *
	 * @param version The version to convert. Can be <code>null</code>
	 * @return The converted version or <code>null</code> if the argument was <code>null</code>
	 * @throws UnsupportedOperationException if the version could not be converted into an OSGi version
	 */
	public static org.osgi.framework.Version toOSGiVersion(Version version) {
		if (version == null) {
			return null;
		}
		if (version == Version.emptyVersion) {
			return org.osgi.framework.Version.emptyVersion;
		}
		if (version == Version.MAX_VERSION) {
			return new org.osgi.framework.Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		}

		BasicVersion bv = (BasicVersion) version;
		return new org.osgi.framework.Version(bv.getMajor(), bv.getMinor(), bv.getMicro(), bv.getQualifier());
	}

	/**
	 * Create an omni version from an OSGi <code>version</code>.
	 * @param version The OSGi version. Can be <code>null</code>.
	 * @return The created omni version
	 */
	public static Version fromOSGiVersion(org.osgi.framework.Version version) {
		if (version == null) {
			return Version.MAX_VERSION;
		}
		if (version.getMajor() == Integer.MAX_VALUE && version.getMinor() == Integer.MAX_VALUE && version.getMicro() == Integer.MAX_VALUE) {
			return Version.MAX_VERSION;
		}
		return Version.createOSGi(version.getMajor(), version.getMinor(), version.getMicro(), version.getQualifier());
	}

	public static org.eclipse.osgi.service.resolver.VersionRange toOSGiVersionRange(VersionRange range) {
		if (range.equals(VersionRange.emptyRange)) {
			return org.eclipse.osgi.service.resolver.VersionRange.emptyRange;
		}
		return new org.eclipse.osgi.service.resolver.VersionRange(toOSGiVersion(range.getMinimum()), range.getIncludeMinimum(), toOSGiVersion(range.getMaximum()), range.getIncludeMinimum());
	}

	public static VersionRange fromOSGiVersionRange(org.eclipse.osgi.service.resolver.VersionRange range) {
		if (range.equals(org.eclipse.osgi.service.resolver.VersionRange.emptyRange)) {
			return VersionRange.emptyRange;
		}

		Version min = fromOSGiVersion(range.getLeft());
		boolean includeMin = range.getIncludeMinimum();

		Version max = fromOSGiVersion(range.getRight());
		// TODO The OSGi open ended range does not include the max value, where as the p2 does
		// Fix the p2 range to not include maximum as well (how will this affect the projector)?.
		boolean includeMax = Version.MAX_VERSION.equals(max) ? true : range.getIncludeMaximum();

		return new VersionRange(min, includeMin, max, includeMax);
	}

	/**
	 *
	 * @return <code>true</code> if md5 sums should be generated <code>false</code>
	 *         otherwise
	 * @since 1.7.0
	 */
	public static boolean isArtifactGenerateChecksums(IPublisherInfo publisherInfo) {
		if ((publisherInfo.getArtifactOptions() & IPublisherInfo.A_NO_MD5) != 0) {
			return false;
		}
		return true;
	}

	/**
	 *
	 * @return <code>true</code> if existing artifacts should be overwritten
	 *         <code>false</code> otherwise
	 * @since 1.7.0
	 */
	public static boolean isArtifactOverwrite(IPublisherInfo publisherInfo) {
		return (publisherInfo.getArtifactOptions() & IPublisherInfo.A_OVERWRITE) != 0;
	}

	/**
	 *
	 * @return <code>true</code> if artifacts should published <code>false</code>
	 *         otherwise
	 * @since 1.7.0
	 */
	public static boolean isArtifactPublish(IPublisherInfo publisherInfo) {
		int artifactOptions = publisherInfo.getArtifactOptions();
		return artifactOptions == 0 || (artifactOptions & IPublisherInfo.A_PUBLISH) != 0;
	}

	/**
	 *
	 * @return <code>true</code> if the artifact index should be updated
	 *         <code>false</code> otherwise
	 * @since 1.7.0
	 */
	public static boolean isArtifactIndex(IPublisherInfo publisherInfo) {
		int artifactOptions = publisherInfo.getArtifactOptions();
		return artifactOptions == 0 || (artifactOptions & IPublisherInfo.A_INDEX) != 0;
	}
}
