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
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.p2.publisher.features.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Publish IUs for all of the features in the given set of locations.  The locations can 
 * be actual locations of the features or folders of features.
 */
public class FeaturesAction extends AbstractPublishingAction {

	public static final String INSTALL_FEATURES_FILTER = "(org.eclipse.update.install.features=true)"; //$NON-NLS-1$

	private File[] locations;

	public static String getTransformedId(String original, boolean isPlugin, boolean isGroup) {
		return (isPlugin ? original : original + (isGroup ? ".feature.group" : ".feature.jar")); //$NON-NLS-1$//$NON-NLS-2$
	}

	public FeaturesAction(File[] locations, IPublisherInfo info) {
		this.locations = expandLocations(locations);
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		Feature[] features = getFeatures(locations);
		generateFeatureIUs(features, results, info);
		return Status.OK_STATUS;
	}

	private File[] expandLocations(File[] list) {
		if (list == null)
			return new File[] {};
		ArrayList result = new ArrayList();
		for (int i = 0; i < list.length; i++) {
			File location = list[i];
			if (location.isDirectory()) {
				File[] entries = location.listFiles();
				for (int j = 0; j < entries.length; j++)
					result.add(entries[j]);
			} else {
				result.add(location);
			}
		}
		return (File[]) result.toArray(new File[result.size()]);
	}

	protected void generateFeatureIUs(Feature[] features, IPublisherResult result, IPublisherInfo info) {
		// Build Feature IUs, and add them to any corresponding categories
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];

			// generate the root file IUs for this feature.  The IU hierarchy must
			// be built from the bottom up so do the root files first.
			ArrayList childIUs = generateRootFileIUs(feature, result, info);

			// create the basic feature IU with all the children
			String location = feature.getLocation();
			boolean isExploded = (location.endsWith(".jar") ? false : true); //$NON-NLS-1$
			IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, childIUs, isExploded, null);

			// add all the artifacts associated with the feature
			IArtifactKey[] artifacts = featureIU.getArtifacts();
			for (int arti = 0; arti < artifacts.length; arti++) {
				IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(artifacts[arti], new File(location), true, false);
				if (isExploded)
					publishArtifact(ad, new File(location).listFiles(), info, INCLUDE_ROOT);
				else
					publishArtifact(ad, new File[] {new File(location)}, info, AS_IS | INCLUDE_ROOT);
			}

			gatherAdvice(feature, info);

			// create the associated group and register the feature and group in the result.
			IInstallableUnit generated = createGroupIU(feature, featureIU, null);
			result.addIU(generated, IPublisherResult.ROOT);
			result.addIU(featureIU, IPublisherResult.ROOT);
		}
	}

	private ArrayList generateRootFileIUs(Feature feature, IPublisherResult result, IPublisherInfo info) {
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
		Object[] iuAndFiles = MetadataGeneratorHelper.createFeatureRootFileIU(featureId, featureVersion, location, rootFile);
		IInstallableUnit iuResult = (IInstallableUnit) iuAndFiles[0];
		File[] fileResult = (File[]) iuAndFiles[1];
		if (fileResult != null && fileResult.length > 0) {
			IArtifactKey artifact = iuResult.getArtifacts()[0];
			ArtifactDescriptor descriptor = new ArtifactDescriptor(artifact);
			publishArtifact(descriptor, fileResult, info, 0);
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
	public void gatherAdvice(Feature feature, IPublisherInfo info) {
		FeatureEntry entries[] = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			FeatureEntry entry = entries[i];
			if (entry.isUnpack())
				info.addAdvice(new BundleShapeAdvice(entry.getId(), new Version(entry.getVersion()), IBundleAdvice.DIR));
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
		iu.setCapabilities(new ProvidedCapability[] {MetadataGeneratorHelper.createSelfCapability(id, version)});

		if (extraProperties != null) {
			Enumeration e = extraProperties.propertyNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				iu.setProperty(name, extraProperties.getProperty(name));
			}
		}
		return MetadataFactory.createInstallableUnit(iu);
	}

	public VersionRange getVersionRange(FeatureEntry entry) {
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
