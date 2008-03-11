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
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;

/**
 * Publish IUs for all of the features in the given set of locations.  The locations can 
 * be actual locations of the features or folders of features.
 */
public class FeaturesAction extends Generator implements IPublishingAction {

	private File[] locations;
	IPublisherInfo info;

	public FeaturesAction(File[] locations, IPublisherInfo info) {
		super(createGeneratorInfo(info));
		this.locations = expandLocations(locations);
		this.info = info;
	}

	private static IGeneratorInfo createGeneratorInfo(IPublisherInfo info) {
		EclipseInstallGeneratorInfoProvider result = new EclipseInstallGeneratorInfoProvider();
		result.setArtifactRepository(info.getArtifactRepository());
		result.setMetadataRepository(info.getMetadataRepository());
		result.setPublishArtifactRepository(info.publishArtifactRepository());
		result.setPublishArtifacts(info.publishArtifacts());
		return result;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		Feature[] features = getFeatures(locations);
		generateFeatureIUs(features, results, info.getArtifactRepository());
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

	protected void generateFeatureIUs(Feature[] features, IPublisherResult result, IArtifactRepository destination) {
		//Build Feature IUs, and add them to any corresponding categories
		for (int i = 0; i < features.length; i++) {
			Feature feature = features[i];

			// generate the root file IUs for this feature.  The IU hierarchy must
			// be built from the bottom up so do the root files first.
			ArrayList childIUs = generateFeatureRootIUs(feature, result, destination);

			// create the basic feature IU with all the children
			String location = feature.getLocation();
			boolean isExploded = (location.endsWith(".jar") ? false : true); //$NON-NLS-1$
			IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, childIUs, isExploded, null);

			// add all the artifacts associated with the feature
			IArtifactKey[] artifacts = featureIU.getArtifacts();
			for (int arti = 0; arti < artifacts.length; arti++) {
				IArtifactDescriptor ad = MetadataGeneratorHelper.createArtifactDescriptor(artifacts[arti], new File(location), true, false);
				if (isExploded)
					publishArtifact(ad, new File(location).listFiles(), destination, false, true);
				else
					publishArtifact(ad, new File[] {new File(location)}, destination, true, true);
			}

			// create the associated group and register the feature and group in the result.
			IInstallableUnit generated = MetadataGeneratorHelper.createGroupIU(feature, featureIU);
			result.addIU(generated, IPublisherResult.ROOT);
			result.addIU(featureIU, IPublisherResult.ROOT);
		}
	}

	private ArrayList generateFeatureRootIUs(Feature feature, IPublisherResult result, IArtifactRepository destination) {
		ArrayList ius = new ArrayList();
		File location = new File(feature.getLocation());
		File tempFile = null;
		try {
			// if the feature is a dir then just return the location
			if (!location.isDirectory()) {
				// otherwise extract the JAR into a temp location and return that location
				tempFile = File.createTempFile("p2.generator", ""); //$NON-NLS-1$ //$NON-NLS-2$
				FileUtils.unzipFile(location, tempFile);
				location = tempFile;
			}
			Properties props = new Properties();
			try {
				InputStream in = null;
				try {
					in = new BufferedInputStream(new FileInputStream(new File(location, "build.properties"))); //$NON-NLS-1$
					props.load(in);
				} finally {
					if (in != null)
						in.close();
				}
			} catch (FileNotFoundException e) {
				// ignore if it is just a file not found.
			} catch (IOException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error parsing feature build.properties", e)); //$NON-NLS-1$
			}

			FileSetDescriptor[] rootFileDescriptors = getRootFileDescriptors(props);
			for (int i = 0; i < rootFileDescriptors.length; i++) {
				IInstallableUnit iu = generateFeatureRootFileIU(feature, location, rootFileDescriptors[i], result, destination);
				ius.add(iu);
			}
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
		} finally {
			if (tempFile != null)
				tempFile.delete();
		}
		return ius;
	}

	private IInstallableUnit generateFeatureRootFileIU(Feature feature, File location, FileSetDescriptor rootFile, IPublisherResult result, IArtifactRepository destination) {
		Object[] iuAndFiles = MetadataGeneratorHelper.createFeatureRootFileIU(feature, location, rootFile);
		IInstallableUnit iuResult = (IInstallableUnit) iuAndFiles[0];
		File[] fileResult = (File[]) iuAndFiles[1];
		if (fileResult != null && fileResult.length > 0) {
			IArtifactKey artifact = iuResult.getArtifacts()[0];
			ArtifactDescriptor descriptor = new ArtifactDescriptor(artifact);
			publishArtifact(descriptor, fileResult, destination, false, false);
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

}
