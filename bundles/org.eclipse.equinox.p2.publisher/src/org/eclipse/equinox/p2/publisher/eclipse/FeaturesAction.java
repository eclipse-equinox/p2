/*******************************************************************************
 * Copyright (c) 2008-2009 Code 9 and others. All rights reserved. This
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitPatchDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.NLS;

/**
 * Publish IUs for all of the features in the given set of locations.  The locations can
 * be actual locations of the features or folders of features.
 */
public class FeaturesAction extends AbstractPublisherAction {
	public static final String INSTALL_FEATURES_FILTER = "(org.eclipse.update.install.features=true)"; //$NON-NLS-1$

	protected Feature[] features;
	private File[] locations;

	public static IArtifactKey createFeatureArtifactKey(String id, String version) {
		return new ArtifactKey(PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER, id, new Version(version));
	}

	public static IInstallableUnit createFeatureJarIU(Feature feature, IPublisherInfo info) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		String id = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/false);
		iu.setId(id);
		Version version = new Version(feature.getVersion());
		iu.setVersion(version);
		if (feature.getLicense() != null)
			iu.setLicense(MetadataFactory.createLicense(toURIOrNull(feature.getLicenseURL()), feature.getLicense()));
		if (feature.getCopyright() != null)
			iu.setCopyright(MetadataFactory.createCopyright(toURIOrNull(feature.getCopyrightURL()), feature.getCopyright()));

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

		iu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(id, version), PublisherHelper.FEATURE_CAPABILITY, MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_UPDATE_FEATURE, feature.getId(), version)});
		iu.setArtifacts(new IArtifactKey[] {createFeatureArtifactKey(feature.getId(), version.toString())});

		// TODO its not clear when this would ever be false reasonably.  Features are always
		// supposed to be installed unzipped.  It is also not clear what it means to set this prop.
		// Anyway, in the future it seems reasonable that features be installed as JARs...
		// Note: We have decided to always unzip features when they are installed (regardless of whether they
		//were zipped when we found them). https://bugs.eclipse.org/bugs/show_bug.cgi?id=267282
		Map touchpointData = new HashMap();
		touchpointData.put("zipped", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		processInstallableUnitPropertiesAdvice(iu, info);
		return MetadataFactory.createInstallableUnit(iu);
	}

	private static String getTransformedId(String original, boolean isPlugin, boolean isGroup) {
		return (isPlugin ? original : original + (isGroup ? ".feature.group" : ".feature.jar")); //$NON-NLS-1$//$NON-NLS-2$
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

	public FeaturesAction(Feature[] features) {
		this.features = features;
	}

	public FeaturesAction(File[] locations) {
		this.locations = locations;
	}

	// attach the described files from the given location to the given iu description.  Return
	// the list of files identified.
	private File[] attachFiles(InstallableUnitDescription iu, FileSetDescriptor descriptor, File location) {
		String fileList = descriptor.getFiles();
		String[] fileSpecs = getArrayFromString(fileList, ","); //$NON-NLS-1$
		File[] files = new File[fileSpecs.length];
		if (fileSpecs.length > 0) {
			for (int i = 0; i < fileSpecs.length; i++) {
				String spec = fileSpecs[i];
				if (spec.startsWith("file:")) //$NON-NLS-1$
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

	/**
	 * Looks for advice in a p2.inf file inside the feature location.
	 */
	private void createAdviceFileAdvice(Feature feature, IPublisherInfo publisherInfo) {
		//assume p2.inf is co-located with feature.xml
		String location = feature.getLocation();
		if (location != null) {
			String groupId = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/true);
			AdviceFileAdvice advice = new AdviceFileAdvice(groupId, new Version(feature.getVersion()), new Path(location), new Path("p2.inf")); //$NON-NLS-1$
			if (advice.containsAdvice())
				publisherInfo.addAdvice(advice);
		}
	}

	/**
	 * Gather any advice we can from the given feature.  In particular, it may have
	 * information about the shape of the bundles it includes.  The discovered advice is
	 * added to the given result.
	 * @param feature the feature to process
	 * @param publisherInfo the publishing info to update
	 */
	private void createBundleShapeAdvice(Feature feature, IPublisherInfo publisherInfo) {
		FeatureEntry entries[] = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			FeatureEntry entry = entries[i];
			if (entry.isUnpack() && entry.isPlugin() && !entry.isRequires())
				publisherInfo.addAdvice(new BundleShapeAdvice(entry.getId(), new Version(entry.getVersion()), IBundleShapeAdvice.DIR));
		}
	}

	protected Object[] createFeatureRootFileIU(String featureId, String featureVersion, File location, FileSetDescriptor descriptor) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String id = featureId + '_' + descriptor.getKey();
		iu.setId(id);
		Version version = new Version(featureVersion);
		iu.setVersion(version);
		iu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(id, version)});
		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		String configSpec = descriptor.getConfigSpec();
		if (configSpec != null && configSpec.length() > 0)
			iu.setFilter(createFilterSpec(configSpec));
		File[] fileResult = attachFiles(iu, descriptor, location);
		setupLinks(iu, descriptor);
		setupPermissions(iu, descriptor);

		IInstallableUnit iuResult = MetadataFactory.createInstallableUnit(iu);
		// need to return both the iu and any files.
		return new Object[] {iuResult, fileResult};
	}

	protected IInstallableUnit createGroupIU(Feature feature, List childIUs, IPublisherInfo publisherInfo) {
		if (isPatch(feature))
			return createPatchIU(feature, childIUs, publisherInfo);
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		String id = getGroupId(feature.getId());
		iu.setId(id);
		Version version = Version.fromOSGiVersion(new org.osgi.framework.Version(feature.getVersion()));
		iu.setVersion(version);

		iu.setProperty(IInstallableUnit.PROP_NAME, feature.getLabel());
		if (feature.getDescription() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION, feature.getDescription());
		if (feature.getDescriptionURL() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION_URL, feature.getDescriptionURL());
		if (feature.getProviderName() != null)
			iu.setProperty(IInstallableUnit.PROP_PROVIDER, feature.getProviderName());
		if (feature.getLicense() != null)
			iu.setLicense(MetadataFactory.createLicense(toURIOrNull(feature.getLicenseURL()), feature.getLicense()));
		if (feature.getCopyright() != null)
			iu.setCopyright(MetadataFactory.createCopyright(toURIOrNull(feature.getCopyrightURL()), feature.getCopyright()));
		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, BundlesAction.computeUpdateRange(new org.osgi.framework.Version(feature.getVersion())), IUpdateDescriptor.NORMAL, null));

		FeatureEntry entries[] = feature.getEntries();
		List required = new ArrayList(entries.length + (childIUs == null ? 0 : childIUs.size()));
		for (int i = 0; i < entries.length; i++) {
			VersionRange range = getVersionRange(entries[i]);
			String requiredId = getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true);
			required.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, requiredId, range, getFilter(entries[i]), entries[i].isOptional(), false));
		}

		// link in all the children (if any) as requirements.
		// TODO consider if these should be linked as exact version numbers.  Should be ok but may be brittle.
		if (childIUs != null) {
			for (int i = 0; i < childIUs.size(); i++) {
				IInstallableUnit child = (IInstallableUnit) childIUs.get(i);
				required.add(MetadataFactory.createRequiredCapability(PublisherHelper.IU_NAMESPACE, child.getId(), new VersionRange(child.getVersion(), true, child.getVersion(), true), child.getFilter(), false, false));
			}
		}
		iu.setRequiredCapabilities((IRequiredCapability[]) required.toArray(new IRequiredCapability[required.size()]));
		iu.setTouchpointType(ITouchpointType.NONE);
		processTouchpointAdvice(iu, null, publisherInfo);
		processInstallableUnitPropertiesAdvice(iu, publisherInfo);
		iu.setProperty(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
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
				providedCapabilities.add(PublisherHelper.makeTranslationCapability(id, locale));
			}
		}

		iu.setCapabilities((IProvidedCapability[]) providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));
		processCapabilityAdvice(iu, publisherInfo);
		return MetadataFactory.createInstallableUnit(iu);
	}

	protected String getGroupId(String featureId) {
		return getTransformedId(featureId, /*isPlugin*/false, /*isGroup*/true);
	}

	private IInstallableUnit createPatchIU(Feature feature, List childIUs, IPublisherInfo publisherInfo) {
		InstallableUnitPatchDescription iu = new MetadataFactory.InstallableUnitPatchDescription();
		String id = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/true);
		iu.setId(id);
		Version version = new Version(feature.getVersion());
		iu.setVersion(version);
		iu.setProperty(IInstallableUnit.PROP_NAME, feature.getLabel());
		if (feature.getDescription() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION, feature.getDescription());
		if (feature.getDescriptionURL() != null)
			iu.setProperty(IInstallableUnit.PROP_DESCRIPTION_URL, feature.getDescriptionURL());
		if (feature.getProviderName() != null)
			iu.setProperty(IInstallableUnit.PROP_PROVIDER, feature.getProviderName());
		if (feature.getLicense() != null)
			iu.setLicense(MetadataFactory.createLicense(toURIOrNull(feature.getLicenseURL()), feature.getLicense()));
		if (feature.getCopyright() != null)
			iu.setCopyright(MetadataFactory.createCopyright(toURIOrNull(feature.getCopyrightURL()), feature.getCopyright()));
		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, BundlesAction.computeUpdateRange(new org.osgi.framework.Version(feature.getVersion())), IUpdateDescriptor.NORMAL, null));

		FeatureEntry entries[] = feature.getEntries();
		ArrayList applicabilityScope = new ArrayList();
		ArrayList patchRequirements = new ArrayList();
		ArrayList requirementChanges = new ArrayList();
		for (int i = 0; i < entries.length; i++) {
			VersionRange range = getVersionRange(entries[i]);
			IRequiredCapability req = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true), range, getFilter(entries[i]), entries[i].isOptional(), false);
			if (entries[i].isRequires()) {
				applicabilityScope.add(req);
				continue;
			}
			if (entries[i].isPlugin()) {
				IRequiredCapability from = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true), VersionRange.emptyRange, getFilter(entries[i]), entries[i].isOptional(), false);
				requirementChanges.add(MetadataFactory.createRequirementChange(from, req));
				continue;
			}
			patchRequirements.add(req);
		}

		//Always add a requirement on the IU containing the feature jar
		if (childIUs != null) {
			for (int i = 0; i < childIUs.size(); i++) {
				IInstallableUnit child = (IInstallableUnit) childIUs.get(i);
				patchRequirements.add(MetadataFactory.createRequiredCapability(PublisherHelper.IU_NAMESPACE, child.getId(), new VersionRange(child.getVersion(), true, child.getVersion(), true), child.getFilter(), false, false));
			}
		}
		iu.setRequiredCapabilities((IRequiredCapability[]) patchRequirements.toArray(new IRequiredCapability[patchRequirements.size()]));
		iu.setApplicabilityScope(new IRequiredCapability[][] {(IRequiredCapability[]) applicabilityScope.toArray(new IRequiredCapability[applicabilityScope.size()])});
		iu.setRequirementChanges((IRequirementChange[]) requirementChanges.toArray(new IRequirementChange[requirementChanges.size()]));

		//Generate lifecycle
		IRequiredCapability lifeCycle = null;
		if (applicabilityScope.size() > 0) {
			IRequiredCapability req = (IRequiredCapability) applicabilityScope.get(0);
			lifeCycle = MetadataFactory.createRequiredCapability(req.getNamespace(), req.getName(), req.getRange(), null, false, false, false);
			iu.setLifeCycle(lifeCycle);
		}

		iu.setTouchpointType(ITouchpointType.NONE);
		processTouchpointAdvice(iu, null, publisherInfo);
		processInstallableUnitPropertiesAdvice(iu, publisherInfo);
		iu.setProperty(IInstallableUnit.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		iu.setProperty(IInstallableUnit.PROP_TYPE_PATCH, Boolean.TRUE.toString());
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
				providedCapabilities.add(PublisherHelper.makeTranslationCapability(id, locale));
			}
		}

		iu.setCapabilities((IProvidedCapability[]) providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));
		processCapabilityAdvice(iu, publisherInfo);
		return MetadataFactory.createInstallableUnitPatch(iu);
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
				if (new File(location, "feature.xml").exists()) //$NON-NLS-1$
					result.add(location);
				else
					expandLocations(location.listFiles(), result);
			} else {
				result.add(location);
			}
		}
	}

	protected void generateFeatureIUs(Feature[] featureList, IPublisherResult result) {
		// Build Feature IUs, and add them to any corresponding categories
		for (int i = 0; i < featureList.length; i++) {
			Feature feature = featureList[i];
			//first gather any advice that might help us
			createBundleShapeAdvice(feature, info);
			createAdviceFileAdvice(feature, info);

			ArrayList childIUs = new ArrayList();

			IInstallableUnit featureJarIU = queryForIU(result, getTransformedId(feature.getId(), false, false), new Version(feature.getVersion()));
			if (featureJarIU == null)
				featureJarIU = generateFeatureJarIU(feature, info);

			if (featureJarIU != null) {
				publishFeatureArtifacts(feature, featureJarIU, info);
				result.addIU(featureJarIU, IPublisherResult.NON_ROOT);
				childIUs.add(featureJarIU);
			}

			IInstallableUnit groupIU = queryForIU(result, getGroupId(feature.getId()), new Version(feature.getVersion()));
			if (groupIU == null) {
				childIUs.addAll(generateRootFileIUs(feature, result, info));
				groupIU = createGroupIU(feature, childIUs, info);
			}
			if (groupIU != null) {
				result.addIU(groupIU, IPublisherResult.ROOT);
				InstallableUnitDescription[] others = processAdditionalInstallableUnitsAdvice(groupIU, info);
				for (int iuIndex = 0; others != null && iuIndex < others.length; iuIndex++) {
					result.addIU(MetadataFactory.createInstallableUnit(others[iuIndex]), IPublisherResult.ROOT);
				}
			}
			generateSiteReferences(feature, result, info);
		}
	}

	protected IInstallableUnit generateFeatureJarIU(Feature feature, IPublisherInfo publisherInfo) {
		return createFeatureJarIU(feature, publisherInfo);
	}

	private IInstallableUnit generateRootFileIU(String featureId, String featureVersion, File location, FileSetDescriptor rootFile, IPublisherResult result, IPublisherInfo publisherInfo) {
		File tempLocation = null;
		try {
			if (location.isFile()) {
				// We cannot copy from a jar file. It must be expanded into a temporary folder
				try {
					tempLocation = File.createTempFile("p2.generator", ""); //$NON-NLS-1$ //$NON-NLS-2$
					tempLocation.delete();
					tempLocation.mkdirs();
					FileUtils.unzipFile(location, tempLocation);
				} catch (IOException e) {
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, e.getMessage()));
					return null;
				}
				location = tempLocation;
			}
			Object[] iuAndFiles = createFeatureRootFileIU(featureId, featureVersion, location, rootFile);
			IInstallableUnit iuResult = (IInstallableUnit) iuAndFiles[0];
			File[] fileResult = (File[]) iuAndFiles[1];
			if (fileResult != null && fileResult.length > 0) {
				IArtifactKey artifact = iuResult.getArtifacts()[0];
				ArtifactDescriptor descriptor = new ArtifactDescriptor(artifact);
				publishArtifact(descriptor, fileResult, null, publisherInfo, FileUtils.createDynamicPathComputer(1));
			}
			result.addIU(iuResult, IPublisherResult.NON_ROOT);
			return iuResult;
		} finally {
			if (tempLocation != null)
				FileUtils.deleteAll(tempLocation);
		}
	}

	protected ArrayList generateRootFileIUs(Feature feature, IPublisherResult result, IPublisherInfo publisherInfo) {
		File location = new File(feature.getLocation());
		Properties props = loadProperties(location, "build.properties"); //$NON-NLS-1$
		ArrayList ius = new ArrayList();
		FileSetDescriptor[] rootFileDescriptors = getRootFileDescriptors(props);
		for (int i = 0; i < rootFileDescriptors.length; i++) {
			IInstallableUnit iu = generateRootFileIU(feature.getId(), feature.getVersion(), location, rootFileDescriptors[i], result, publisherInfo);
			if (iu != null)
				ius.add(iu);
		}
		return ius;
	}

	/**
	 * Generates and publishes a reference to an update site location
	 * @param location The update site location
	 * @param nickname The update site label
	 * @param featureId the identifier of the feature where the error occurred, or null
	 * @param metadataRepo The repo into which the references are added
	 */
	private void generateSiteReference(String location, String nickname, String featureId, IMetadataRepository metadataRepo) {
		if (location == null) {
			String message = featureId == null ? NLS.bind(Messages.exception_invalidSiteReference, location) : NLS.bind(Messages.exception_invalidSiteReferenceInFeature, location, featureId);
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message));
			return;
		}

		try {
			URI associateLocation = new URI(location);
			metadataRepo.addReference(associateLocation, nickname, IRepository.TYPE_METADATA, IRepository.NONE);
			metadataRepo.addReference(associateLocation, nickname, IRepository.TYPE_ARTIFACT, IRepository.NONE);
		} catch (URISyntaxException e) {
			String message = featureId == null ? NLS.bind(Messages.exception_invalidSiteReference, location) : NLS.bind(Messages.exception_invalidSiteReferenceInFeature, location, featureId);
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message));
		}
	}

	protected void generateSiteReferences(Feature feature, IPublisherResult result, IPublisherInfo publisherInfo) {
		//publish feature site references
		URLEntry updateURL = feature.getUpdateSite();
		//don't enable feature update sites by default since this results in too many
		//extra sites being loaded and searched (Bug 234177)
		if (updateURL != null)
			generateSiteReference(updateURL.getURL(), updateURL.getAnnotation(), feature.getId(), publisherInfo.getMetadataRepository());
		URLEntry[] discoverySites = feature.getDiscoverySites();
		for (int i = 0; i < discoverySites.length; i++)
			generateSiteReference(discoverySites[i].getURL(), discoverySites[i].getAnnotation(), feature.getId(), publisherInfo.getMetadataRepository());
	}

	protected Feature[] getFeatures(File[] featureLocations) {
		ArrayList result = new ArrayList(featureLocations.length);
		for (int i = 0; i < featureLocations.length; i++) {
			Feature feature = new FeatureParser().parse(featureLocations[i]);
			if (feature != null) {
				feature.setLocation(featureLocations[i].getAbsolutePath());
				result.add(feature);
			}
		}
		return (Feature[]) result.toArray(new Feature[result.size()]);
	}

	private String getFilter(FeatureEntry entry) {
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

	protected FileSetDescriptor[] getRootFileDescriptors(Properties props) {
		HashMap result = new HashMap();
		for (Iterator i = props.keySet().iterator(); i.hasNext();) {
			String property = (String) i.next();
			// we only care about root properties
			if (!(property.startsWith("root") && (property.length() == 4 || property.charAt(4) == '.'))) //$NON-NLS-1$
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

			// if the property was simply 'root'
			if (spec.length == 1)
				// it must be a straight file copy (without a config)
				descriptor.setFiles(props.getProperty(property));
			// if the last segment in the spec is "link"
			else if (spec[spec.length - 1] == "link") //$NON-NLS-1$
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

	private VersionRange getVersionRange(FeatureEntry entry) {
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

	private boolean isPatch(Feature feature) {
		FeatureEntry[] entries = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].isPatch())
				return true;
		}
		return false;
	}

	private static Properties loadProperties(File location, String file) {
		Properties props = new Properties();
		try {
			if (!location.isDirectory()) {
				JarFile jar = null;
				try {
					jar = new JarFile(location);
					JarEntry entry = jar.getJarEntry(file);
					if (entry != null)
						parseProperties(jar.getInputStream(entry), props, location.toString() + '#' + file);
				} finally {
					if (jar != null)
						jar.close();
				}
			} else {
				try {
					InputStream in = null;
					try {
						File propsFile = new File(location, file);
						in = new FileInputStream(propsFile);
						parseProperties(in, props, propsFile.toString());
					} finally {
						if (in != null)
							in.close();
					}
				} catch (FileNotFoundException e) {
					// ignore if it is just a file not found.
				}
			}
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, Messages.exception_errorPublishingArtifacts, e));
		}
		return props;
	}

	private static void parseProperties(InputStream in, Properties props, String file) {
		try {
			props.load(new BufferedInputStream(in));
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.exception_errorLoadingProperties, file), e));
		}
	}

	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		if (features == null && locations == null)
			throw new IllegalStateException(Messages.exception_noFeaturesOrLocations);
		this.info = publisherInfo;
		if (features == null)
			features = getFeatures(expandLocations(locations));
		generateFeatureIUs(features, results);
		return Status.OK_STATUS;
	}

	protected void publishFeatureArtifacts(Feature feature, IInstallableUnit featureIU, IPublisherInfo publisherInfo) {
		// add all the artifacts associated with the feature
		// TODO this is a little strange.  If there are several artifacts, how do we know which files go with
		// which artifacts when we publish them?  For now it would be surprising to have more than one
		// artifact per feature IU.
		IArtifactKey[] artifacts = featureIU.getArtifacts();
		for (int j = 0; j < artifacts.length; j++) {
			File file = new File(feature.getLocation());
			ArtifactDescriptor ad = (ArtifactDescriptor) PublisherHelper.createArtifactDescriptor(artifacts[j], file);
			processArtifactPropertiesAdvice(featureIU, ad, publisherInfo);
			ad.setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, IArtifactDescriptor.TYPE_ZIP);
			// if the artifact is a dir then zip it up.
			if (file.isDirectory())
				publishArtifact(ad, new File[] {file}, null, publisherInfo, createRootPrefixComputer(file));
			else
				publishArtifact(ad, file, publisherInfo);
		}
	}

	private void setupLinks(InstallableUnitDescription iu, FileSetDescriptor descriptor) {
		// TODO setup the link support.
	}

	private void setupPermissions(InstallableUnitDescription iu, FileSetDescriptor descriptor) {
		Map touchpointData = new HashMap();
		String[][] permsList = descriptor.getPermissions();
		for (int i = 0; i < permsList.length; i++) {
			String[] permSpec = permsList[i];
			String configurationData = " chmod(targetDir:${installFolder}, targetFile:" + permSpec[1] + ", permissions:" + permSpec[0] + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			touchpointData.put("install", configurationData); //$NON-NLS-1$
			iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		}
	}
}
