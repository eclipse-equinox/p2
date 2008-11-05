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
 *     Code 9 - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.spi.p2.publisher;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitFragmentDescription;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * This class was originally the MetadataGeneratorHelper from the Generator.
 * Much of the code has been deprecated and will be removed.
 *
 */
// TODO remove the deprecated code and see if there is much left
public class PublisherHelper {
	/**
	 * A capability namespace representing the type of Eclipse resource (bundle, feature, source bundle, etc)
	 * @see RequiredCapability#getNamespace()
	 * @see ProvidedCapability#getNamespace()
	 */
	public static final String NAMESPACE_ECLIPSE_TYPE = "org.eclipse.equinox.p2.eclipse.type"; //$NON-NLS-1$

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
	 * A capability namespace representing the localization (translation)
	 * of strings from a specified IU in a specified locale
	 * @see RequiredCapability#getNamespace()
	 * @see ProvidedCapability#getNamespace()
	 * TODO: this should be in API, probably in IInstallableUnit
	 */
	public static final String NAMESPACE_IU_LOCALIZATION = "org.eclipse.equinox.p2.localization"; //$NON-NLS-1$

	// Only certain properties in the bundle manifest are assumed to be localized.
	public static final String[] BUNDLE_LOCALIZED_PROPERTIES = {Constants.BUNDLE_NAME, Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_VENDOR, Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_DOCURL, Constants.BUNDLE_UPDATELOCATION};

	public static final String CAPABILITY_NS_JAVA_PACKAGE = "java.package"; //$NON-NLS-1$

	public static final String CAPABILITY_NS_UPDATE_FEATURE = "org.eclipse.update.feature"; //$NON-NLS-1$

	public static final String ECLIPSE_FEATURE_CLASSIFIER = "org.eclipse.update.feature"; //$NON-NLS-1$
	public static final String OSGI_BUNDLE_CLASSIFIER = "osgi.bundle"; //$NON-NLS-1$
	public static final String BINARY_ARTIFACT_CLASSIFIER = "binary"; //$NON-NLS-1$

	public static final String INSTALL_FEATURES_FILTER = "(org.eclipse.update.install.features=true)"; //$NON-NLS-1$

	public static final String IU_NAMESPACE = IInstallableUnit.NAMESPACE_IU_ID;

	public static final String ECLIPSE_INSTALL_HANDLER_PROP = "org.eclipse.update.installHandler"; //$NON-NLS-1$

	public static final Version versionMax = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	public static final TouchpointType TOUCHPOINT_NATIVE = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.native", new Version(1, 0, 0)); //$NON-NLS-1$
	public static final TouchpointType TOUCHPOINT_OSGI = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", new Version(1, 0, 0)); //$NON-NLS-1$

	public static final ProvidedCapability FEATURE_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_FEATURE, new Version(1, 0, 0));

	public static IArtifactDescriptor createArtifactDescriptor(IArtifactKey key, File pathOnDisk) {
		//TODO this size calculation is bogus
		ArtifactDescriptor result = new ArtifactDescriptor(key);
		if (pathOnDisk != null) {
			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, Long.toString(pathOnDisk.length()));
			// TODO - this is wrong but I'm testing a work-around for bug 205842
			result.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(pathOnDisk.length()));
		}
		String md5 = computeMD5(pathOnDisk);
		if (md5 != null)
			result.setProperty(IArtifactDescriptor.DOWNLOAD_MD5, md5);
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

	public static ProvidedCapability makeTranslationCapability(String hostId, Locale locale) {
		return MetadataFactory.createProvidedCapability(NAMESPACE_IU_LOCALIZATION, locale.toString(), new Version(1, 0, 0));
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
		RequiredCapability[] reqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_FEATURE, VersionRange.emptyRange, null, true, true, false)};
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
		RequiredCapability[] reqs = new RequiredCapability[] {MetadataFactory.createRequiredCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_SOURCE, VersionRange.emptyRange, null, true, true, false)};
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
		ArrayList iusCreated = new ArrayList(1);
		IInstallableUnit iu = BundlesAction.createBundleIU(bd, manifest, isFolderPlugin, key, new PublisherInfo());
		addExtraProperties(iu, extraProperties);
		iusCreated.add(iu);
		return (IInstallableUnit[]) (iusCreated.toArray(new IInstallableUnit[iusCreated.size()]));
	}

	public static ArtifactKey createBinaryArtifactKey(String id, Version version) {
		return new ArtifactKey(BINARY_ARTIFACT_CLASSIFIER, id, version);
	}

	public static ProvidedCapability createSelfCapability(String installableUnitId, Version installableUnitVersion) {
		return MetadataFactory.createProvidedCapability(IU_NAMESPACE, installableUnitId, installableUnitVersion);
	}

}
