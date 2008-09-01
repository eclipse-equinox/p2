/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Publish IUs for all of the features in the given set of locations.  The locations can 
 * be actual locations of the features or folders of features.
 */
public class FeaturesAction extends AbstractPublisherAction {
	public static final String INSTALL_FEATURES_FILTER = "(org.eclipse.update.install.features=true)"; //$NON-NLS-1$

	private File[] locations;
	protected Feature[] features;

	public static String getTransformedId(String original, boolean isPlugin, boolean isGroup) {
		return (isPlugin ? original : original + (isGroup ? ".feature.group" : ".feature.jar")); //$NON-NLS-1$//$NON-NLS-2$
	}

	public static IArtifactKey createFeatureArtifactKey(String id, String version) {
		return new ArtifactKey(PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER, id, new Version(version));
	}

	public static Object[] createFeatureRootFileIU(String featureId, String featureVersion, File location, FileSetDescriptor descriptor) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String id = featureId + '_' + descriptor.getKey();
		iu.setId(id);
		Version version = new Version(featureVersion);
		iu.setVersion(version);
		iu.setCapabilities(new ProvidedCapability[] {PublisherHelper.createSelfCapability(id, version)});
		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		String configSpec = descriptor.getConfigSpec();
		if (configSpec != null)
			iu.setFilter(AbstractPublisherAction.createFilterSpec(configSpec));
		File[] fileResult = attachFiles(iu, descriptor, location);
		setupLinks(iu, descriptor);
		setupPermissions(iu, descriptor);

		IInstallableUnit iuResult = MetadataFactory.createInstallableUnit(iu);
		// need to return both the iu and any files.
		return new Object[] {iuResult, fileResult};
	}

	// attach the described files from the given location to the given iu description.  Return
	// the list of files identified.
	private static File[] attachFiles(InstallableUnitDescription iu, FileSetDescriptor descriptor, File location) {
		String fileList = descriptor.getFiles();
		String[] fileSpecs = getArrayFromString(fileList, ","); //$NON-NLS-1$
		File[] files = new File[fileSpecs.length];
		if (fileSpecs.length > 0) {
			for (int i = 0; i < fileSpecs.length; i++) {
				String spec = fileSpecs[i];
				if (spec.startsWith("file:"))
					spec = spec.substring(5);
				files[i] = new File(location, spec);
			}
		}
		// add touchpoint actions to unzip and cleanup as needed
		// TODO need to support fancy root file location specs
		Map touchpointData = new HashMap(2);
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		// prime the IU with an artifact key that will correspond to the zipped up root files.
		IArtifactKey key = new ArtifactKey(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, iu.getId(), iu.getVersion());
		iu.setArtifacts(new IArtifactKey[] {key});
		return files;
	}

	private static void setupPermissions(InstallableUnitDescription iu, FileSetDescriptor descriptor) {
		Map touchpointData = new HashMap();
		String[][] permsList = descriptor.getPermissions();
		for (int i = 0; i < permsList.length; i++) {
			String[] permSpec = permsList[i];
			String configurationData = " chmod(targetDir:${installFolder}, targetFile:" + permSpec[1] + ", permissions:" + permSpec[0] + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			touchpointData.put("install", configurationData); //$NON-NLS-1$
			iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		}
	}

	private static void setupLinks(InstallableUnitDescription iu, FileSetDescriptor descriptor) {
		// TODO setup the link support.
	}

	public FeaturesAction(File[] locations) {
		this.locations = locations;
	}

	public FeaturesAction(Feature[] features) {
		this.features = features;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		if (features == null && locations == null)
			throw new IllegalStateException("No features or locations provided");
		if (features == null)
			features = getFeatures(expandLocations(locations));
		generateFeatureIUs(features, results, info);
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
				// if the location is itself a feature, just add it.  Otherwise r down
				if (new File(location, "feature.xml").exists())
					result.add(location);
				else
					expandLocations(location.listFiles(), result);
			} else {
				result.add(location);
			}
		}
	}

	protected void generateFeatureIUs(Feature[] features, IPublisherResult result, IPublisherInfo info) {
		// Build Feature IUs, and add them to any corresponding categories
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];
			ArrayList childIUs = generateRootFileIUs(feature, result, info);

			Properties props = getFeatureAdvice(feature, info);
			IInstallableUnit featureIU = createFeatureJarIU(feature, childIUs, props);
			publishFeatureArtifacts(feature, featureIU, info);
			result.addIU(featureIU, IPublisherResult.ROOT);
			generateSiteReferences(feature, result, info);

			gatherBundleShapeAdvice(feature, info);

			IInstallableUnit groupIU = createGroupIU(feature, featureIU, props);
			result.addIU(groupIU, IPublisherResult.ROOT);
		}
	}

	private void generateSiteReferences(Feature feature, IPublisherResult result, IPublisherInfo info) {
		//publish feature site references
		String updateURL = feature.getUpdateSiteURL();
		//don't enable feature update sites by default since this results in too many
		//extra sites being loaded and searched (Bug 234177)
		if (updateURL != null)
			generateSiteReference(updateURL, feature.getId(), info.getMetadataRepository());
		URLEntry[] discoverySites = feature.getDiscoverySites();
		for (int j = 0; j < discoverySites.length; j++)
			generateSiteReference(discoverySites[j].getURL(), feature.getId(), info.getMetadataRepository());
	}

	/**
	 * Generates and publishes a reference to an update site location
	 * @param location The update site location
	 * @param featureId the identifier of the feature where the error occurred, or null
	 * @param metadataRepo The repo into which the references are added
	 */
	private void generateSiteReference(String location, String featureId, IMetadataRepository metadataRepo) {
		try {
			URL associateLocation = new URL(location);
			metadataRepo.addReference(associateLocation, IRepository.TYPE_METADATA, IRepository.NONE);
			metadataRepo.addReference(associateLocation, IRepository.TYPE_ARTIFACT, IRepository.NONE);
		} catch (MalformedURLException e) {
			String message = "Invalid site reference: " + location; //$NON-NLS-1$
			if (featureId != null)
				message = message + " in feature: " + featureId; //$NON-NLS-1$
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message));
		}
	}

	protected void publishFeatureArtifacts(Feature feature, IInstallableUnit featureIU, IPublisherInfo info) {
		// add all the artifacts associated with the feature
		// TODO this is a little strange.  If there are several artifacts, how do we know which files go with
		// which artifacts when we publish them?  For now it would be surprising to have more than one
		// artifact per feature IU.
		IArtifactKey[] artifacts = featureIU.getArtifacts();
		for (int j = 0; j < artifacts.length; j++) {
			File file = new File(feature.getLocation());
			IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(artifacts[j], file);
			addProperties((ArtifactDescriptor) ad, feature, info);
			((ArtifactDescriptor) ad).setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, IArtifactDescriptor.TYPE_ZIP);
			// if the artifact is a dir then zip it up.
			if (file.isDirectory())
				publishArtifact(ad, new File[] {file}, null, info, createRootPrefixComputer(file));
			else
				publishArtifact(ad, file, info);
		}
	}

	/**
	 * Add all of the advice for the feature at the given location to the given descriptor.
	 * @param descriptor the descriptor to decorate
	 * @param feature the feature we are getting advice for
	 * @param info the publisher info supplying the advice
	 */
	private void addProperties(ArtifactDescriptor descriptor, Feature feature, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, false, null, null, IFeatureAdvice.class);
		for (Iterator i = advice.iterator(); i.hasNext();) {
			IFeatureAdvice entry = (IFeatureAdvice) i.next();
			Properties props = entry.getArtifactProperties(feature);
			if (props == null)
				continue;
			for (Iterator j = props.keySet().iterator(); j.hasNext();) {
				String key = (String) j.next();
				descriptor.setRepositoryProperty(key, props.getProperty(key));
			}
		}
	}

	private Properties getFeatureAdvice(Feature feature, IPublisherInfo info) {
		Properties result = new Properties();
		Collection advice = info.getAdvice(null, false, null, null, IFeatureAdvice.class);
		for (Iterator i = advice.iterator(); i.hasNext();) {
			IFeatureAdvice entry = (IFeatureAdvice) i.next();
			Properties props = entry.getIUProperties(feature);
			if (props != null)
				result.putAll(props);
		}
		return result;
	}

	protected ArrayList generateRootFileIUs(Feature feature, IPublisherResult result, IPublisherInfo info) {
		File location = new File(feature.getLocation());
		Properties props = loadProperties(location, "build.properties");
		return generateRootFileIUs(feature.getId(), feature.getVersion(), props, location, result, info);
	}

	private ArrayList generateRootFileIUs(String featureId, String featureVersion, Properties props, File location, IPublisherResult result, IPublisherInfo info) {
		ArrayList ius = new ArrayList();
		FileSetDescriptor[] rootFileDescriptors = getRootFileDescriptors(props);
		for (int i = 0; i < rootFileDescriptors.length; i++) {
			IInstallableUnit iu = generateRootFileIU(featureId, featureVersion, location, rootFileDescriptors[i], result, info);
			ius.add(iu);
		}
		return ius;
	}

	private Properties loadProperties(File location, String file) {
		Properties props = new Properties();
		File tempFile = null;
		try {
			// if the feature is a dir then just return the location
			if (!location.isDirectory()) {
				// otherwise extract the JAR into a temp location and return that location
				tempFile = File.createTempFile("p2.generator", ""); //$NON-NLS-1$ //$NON-NLS-2$
				FileUtils.unzipFile(location, tempFile);
				location = tempFile;
			}
			try {
				InputStream in = null;
				try {
					in = new BufferedInputStream(new FileInputStream(new File(location, file))); //$NON-NLS-1$
					props.load(in);
				} finally {
					if (in != null)
						in.close();
				}
			} catch (FileNotFoundException e) {
				// ignore if it is just a file not found.
			} catch (IOException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error parsing " + file, e)); //$NON-NLS-1$
			}
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
		} finally {
			if (tempFile != null)
				tempFile.delete();
		}
		return props;
	}

	private IInstallableUnit generateRootFileIU(String featureId, String featureVersion, File location, FileSetDescriptor rootFile, IPublisherResult result, IPublisherInfo info) {
		Object[] iuAndFiles = createFeatureRootFileIU(featureId, featureVersion, location, rootFile);
		IInstallableUnit iuResult = (IInstallableUnit) iuAndFiles[0];
		File[] fileResult = (File[]) iuAndFiles[1];
		if (fileResult != null && fileResult.length > 0) {
			IArtifactKey artifact = iuResult.getArtifacts()[0];
			ArtifactDescriptor descriptor = new ArtifactDescriptor(artifact);
			publishArtifact(descriptor, fileResult, null, info, FileUtils.createDynamicPathComputer(1));
		}
		result.addIU(iuResult, IPublisherResult.NON_ROOT);
		return iuResult;
	}

	protected FileSetDescriptor[] getRootFileDescriptors(Properties props) {
		HashMap result = new HashMap();
		for (Iterator i = props.keySet().iterator(); i.hasNext();) {
			String property = (String) i.next();
			// we only care about root properties
			if (!property.startsWith("root.")) //$NON-NLS-1$
				continue;
			String[] spec = getArrayFromString(property, "."); //$NON-NLS-1$
			String descriptorKey = spec[0];
			String configSpec = null;
			// if the spec is 4 or more then there must be a config involved to get it
			if (spec.length > 3) {
				configSpec = createConfigSpec(spec[2], spec[1], spec[3]);
				descriptorKey += "." + createIdString(configSpec); //$NON-NLS-1$
			}

			FileSetDescriptor descriptor = (FileSetDescriptor) result.get(descriptorKey);
			if (descriptor == null) {
				descriptor = new FileSetDescriptor(descriptorKey, configSpec);
				result.put(descriptorKey, descriptor);
			}
			// if the last segment in the spec is "link" 
			if (spec[spec.length - 1] == "link") //$NON-NLS-1$
				descriptor.setLinks(props.getProperty(property));
			else {
				// if the second last segment is "permissions" 
				if (spec[spec.length - 2].equals("permissions")) //$NON-NLS-1$
					descriptor.addPermissions(new String[] {spec[spec.length - 1], props.getProperty(property)});
				else {
					// so it is not a link or a permissions, it must be a straight file copy (with or without a config)
					descriptor.setFiles(props.getProperty(property));
				}
			}
		}
		Collection values = result.values();
		return (FileSetDescriptor[]) values.toArray(new FileSetDescriptor[values.size()]);
	}

	/**
	 * Gather any advice we can from the given feature.  In particular, it may have
	 * information about the shape of the bundles it includes.  The discovered advice is
	 * added to the given result.
	 * @param feature the feature to process
	 * @param info the publishing info to update
	 */
	public void gatherBundleShapeAdvice(Feature feature, IPublisherInfo info) {
		FeatureEntry entries[] = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			FeatureEntry entry = entries[i];
			if (entry.isUnpack() && entry.isPlugin() && !entry.isRequires())
				info.addAdvice(new BundleShapeAdvice(entry.getId(), new Version(entry.getVersion()), IBundleShapeAdvice.DIR));
		}
	}

	public IInstallableUnit createFeatureJarIU(Feature feature, ArrayList childIUs, Properties extraProperties) {
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

		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_OSGI);
		iu.setFilter(INSTALL_FEATURES_FILTER);
		iu.setSingleton(true);

		if (feature.getInstallHandler() != null && feature.getInstallHandler().trim().length() > 0) {
			String installHandlerProperty = "handler=" + feature.getInstallHandler(); //$NON-NLS-1$

			if (feature.getInstallHandlerLibrary() != null)
				installHandlerProperty += ", library=" + feature.getInstallHandlerLibrary(); //$NON-NLS-1$

			if (feature.getInstallHandlerURL() != null)
				installHandlerProperty += ", url=" + feature.getInstallHandlerURL(); //$NON-NLS-1$

			iu.setProperty(PublisherHelper.ECLIPSE_INSTALL_HANDLER_PROP, installHandlerProperty);
		}

		iu.setCapabilities(new ProvidedCapability[] {PublisherHelper.createSelfCapability(id, version), PublisherHelper.FEATURE_CAPABILITY, MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_UPDATE_FEATURE, feature.getId(), version)});
		iu.setArtifacts(new IArtifactKey[] {createFeatureArtifactKey(feature.getId(), version.toString())});

		// link in all the children (if any) as requirements.
		// TODO consider if these should be linked as exact version numbers.  Should be ok but may be brittle.
		if (childIUs != null) {
			RequiredCapability[] required = new RequiredCapability[childIUs.size()];
			for (int i = 0; i < childIUs.size(); i++) {
				IInstallableUnit child = (IInstallableUnit) childIUs.get(i);
				required[i] = MetadataFactory.createRequiredCapability(PublisherHelper.IU_NAMESPACE, child.getId(), new VersionRange(child.getVersion(), true, child.getVersion(), true), INSTALL_FEATURES_FILTER, false, false);
			}
			iu.setRequiredCapabilities(required);
		}

		// if the feature has a location and it is not a JAR then setup the touchpoint data
		// TODO its not clear when this would ever be false reasonably.  Features are always 
		// supposed to be installed unzipped.  It is also not clear what it means to set this prop.
		// Anyway, in the future it seems reasonable that features be installed as JARs...
		if (feature.getLocation() != null && !feature.getLocation().endsWith(".jar")) {
			Map touchpointData = new HashMap();
			touchpointData.put("zipped", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		}
		addExtraProperties(iu, extraProperties);
		return MetadataFactory.createInstallableUnit(iu);
	}

	private void addExtraProperties(InstallableUnitDescription iu, Properties extraProperties) {
		if (extraProperties != null) {
			Enumeration e = extraProperties.propertyNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				iu.setProperty(name, extraProperties.getProperty(name));
			}
		}
	}

	public IInstallableUnit createGroupIU(Feature feature, IInstallableUnit featureIU, Properties extraProperties) {
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

		// generate requirements for the feature inclusions/requirement 
		FeatureEntry entries[] = feature.getEntries();
		RequiredCapability[] required = new RequiredCapability[entries.length + 1];
		for (int i = 0; i < entries.length; i++) {
			VersionRange range = getVersionRange(entries[i]);
			required[i] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true), range, getFilter(entries[i]), entries[i].isOptional(), false);
		}
		required[entries.length] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, featureIU.getId(), new VersionRange(featureIU.getVersion(), true, featureIU.getVersion(), true), INSTALL_FEATURES_FILTER, false, false);
		iu.setRequiredCapabilities(required);
		iu.setTouchpointType(TouchpointType.NONE);
		iu.setProperty(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		// TODO: shouldn't the filter for the group be constructed from os, ws, arch, nl
		// 		 of the feature?
		// iu.setFilter(filter);
		iu.setCapabilities(new ProvidedCapability[] {PublisherHelper.createSelfCapability(id, version)});

		addExtraProperties(iu, extraProperties);
		return MetadataFactory.createInstallableUnit(iu);
	}

	public VersionRange getVersionRange(FeatureEntry entry) {
		String versionSpec = entry.getVersion();
		if (versionSpec == null)
			return VersionRange.emptyRange;
		Version version = new Version(versionSpec);
		if (version.equals(Version.emptyVersion))
			return VersionRange.emptyRange;
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

	public String getFilter(FeatureEntry entry) {
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

	protected Feature[] getFeatures(File[] locations) {
		ArrayList result = new ArrayList(locations.length);
		for (int i = 0; i < locations.length; i++) {
			Feature feature = new FeatureParser().parse(locations[i]);
			if (feature != null) {
				feature.setLocation(locations[i].getAbsolutePath());
				result.add(feature);
			}
		}
		return (Feature[]) result.toArray(new Feature[result.size()]);
	}
}
