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

import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitPatchDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

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

	public static IInstallableUnit createFeatureJarIU(Feature feature, ArrayList childIUs, IPublisherInfo info) {
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

		// link in all the children (if any) as requirements.
		// TODO consider if these should be linked as exact version numbers.  Should be ok but may be brittle.
		if (childIUs != null) {
			IRequiredCapability[] required = new IRequiredCapability[childIUs.size()];
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
		if (feature.getLocation() == null || !feature.getLocation().endsWith(".jar")) {
			Map touchpointData = new HashMap();
			touchpointData.put("zipped", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		}
		processFeatureAdvice(feature, iu, info);
		return MetadataFactory.createInstallableUnit(iu);
	}

	private static Properties getFeatureAdvice(Feature feature, IPublisherInfo info) {
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

	private static String getTransformedId(String original, boolean isPlugin, boolean isGroup) {
		return (isPlugin ? original : original + (isGroup ? ".feature.group" : ".feature.jar")); //$NON-NLS-1$//$NON-NLS-2$
	}

	private static void processFeatureAdvice(Feature feature, InstallableUnitDescription iu, IPublisherInfo info) {
		Properties extraProperties = getFeatureAdvice(feature, info);
		if (extraProperties != null) {
			Enumeration e = extraProperties.propertyNames();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				iu.setProperty(name, extraProperties.getProperty(name));
			}
		}
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

	/**
	 * Add all of the advice for the feature at the given location to the given descriptor.
	 * @param descriptor the descriptor to decorate
	 * @param feature the feature we are getting advice for
	 * @param info the publisher info supplying the advice
	 */
	protected void addProperties(ArtifactDescriptor descriptor, Feature feature, IPublisherInfo info) {
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

	// attach the described files from the given location to the given iu description.  Return
	// the list of files identified.
	private File[] attachFiles(InstallableUnitDescription iu, FileSetDescriptor descriptor, File location) {
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

	/**
	 * Looks for advice in a p2.inf file inside the feature location.
	 */
	private void createAdviceFileAdvice(Feature feature, IPublisherInfo info) {
		//assume p2.inf is co-located with feature.xml
		String location = feature.getLocation();
		if (location != null)
			info.addAdvice(new AdviceFileAdvice(feature.getId(), new Version(feature.getVersion()), new Path(location), new Path("p2.inf"))); //$NON-NLS-1$
	}

	/**
	 * Gather any advice we can from the given feature.  In particular, it may have
	 * information about the shape of the bundles it includes.  The discovered advice is
	 * added to the given result.
	 * @param feature the feature to process
	 * @param info the publishing info to update
	 */
	private void createBundleShapeAdvice(Feature feature, IPublisherInfo info) {
		FeatureEntry entries[] = feature.getEntries();
		for (int i = 0; i < entries.length; i++) {
			FeatureEntry entry = entries[i];
			if (entry.isUnpack() && entry.isPlugin() && !entry.isRequires())
				info.addAdvice(new BundleShapeAdvice(entry.getId(), new Version(entry.getVersion()), IBundleShapeAdvice.DIR));
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
		if (configSpec != null)
			iu.setFilter(createFilterSpec(configSpec));
		File[] fileResult = attachFiles(iu, descriptor, location);
		setupLinks(iu, descriptor);
		setupPermissions(iu, descriptor);

		IInstallableUnit iuResult = MetadataFactory.createInstallableUnit(iu);
		// need to return both the iu and any files.
		return new Object[] {iuResult, fileResult};
	}

	protected IInstallableUnit createGroupIU(Feature feature, IInstallableUnit featureIU, IPublisherInfo info) {
		if (isPatch(feature))
			return createPatchIU(feature, featureIU, info);
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		String id = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/true);
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
		IRequiredCapability[] required = new IRequiredCapability[entries.length + (featureIU == null ? 0 : 1)];
		for (int i = 0; i < entries.length; i++) {
			VersionRange range = getVersionRange(entries[i]);
			String requiredId = getTransformedId(entries[i].getId(), entries[i].isPlugin(), /*isGroup*/true);
			required[i] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, requiredId, range, getFilter(entries[i]), entries[i].isOptional(), false);
		}
		// the feature IU could be null if we are just generating a feature structure rather than
		// actual features.
		if (featureIU != null)
			required[entries.length] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, featureIU.getId(), new VersionRange(featureIU.getVersion(), true, featureIU.getVersion(), true), INSTALL_FEATURES_FILTER, false, false);
		iu.setRequiredCapabilities(required);
		iu.setTouchpointType(ITouchpointType.NONE);
		processTouchpointAdvice(iu, info);
		processFeatureAdvice(feature, iu, info);
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
		return MetadataFactory.createInstallableUnit(iu);
	}

	private IInstallableUnit createPatchIU(Feature feature, IInstallableUnit featureIU, IPublisherInfo info) {
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
		patchRequirements.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, featureIU.getId(), new VersionRange(featureIU.getVersion(), true, featureIU.getVersion(), true), INSTALL_FEATURES_FILTER, false, false));
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
		processTouchpointAdvice(iu, info);
		processFeatureAdvice(feature, iu, info);
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
			//first gather any advice that might help us
			createBundleShapeAdvice(feature, info);
			createAdviceFileAdvice(feature, info);

			ArrayList childIUs = generateRootFileIUs(feature, result, info);
			IInstallableUnit featureIU = generateFeatureJarIU(feature, childIUs, info);
			if (featureIU != null) {
				publishFeatureArtifacts(feature, featureIU, info);
				result.addIU(featureIU, IPublisherResult.ROOT);
			}
			generateSiteReferences(feature, result, info);

			IInstallableUnit groupIU = createGroupIU(feature, featureIU, info);
			result.addIU(groupIU, IPublisherResult.ROOT);
		}
	}

	protected IInstallableUnit generateFeatureJarIU(Feature feature, ArrayList childIUs, IPublisherInfo info) {
		return createFeatureJarIU(feature, childIUs, info);
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

	protected ArrayList generateRootFileIUs(Feature feature, IPublisherResult result, IPublisherInfo info) {
		File location = new File(feature.getLocation());
		Properties props = loadProperties(location, "build.properties"); //$NON-NLS-1$
		ArrayList ius = new ArrayList();
		FileSetDescriptor[] rootFileDescriptors = getRootFileDescriptors(props);
		for (int i = 0; i < rootFileDescriptors.length; i++) {
			IInstallableUnit iu = generateRootFileIU(feature.getId(), feature.getVersion(), location, rootFileDescriptors[i], result, info);
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
		try {
			URI associateLocation = new URI(location);
			metadataRepo.addReference(associateLocation, nickname, IRepository.TYPE_METADATA, IRepository.NONE);
			metadataRepo.addReference(associateLocation, nickname, IRepository.TYPE_ARTIFACT, IRepository.NONE);
		} catch (URISyntaxException e) {
			String message = "Invalid site reference: " + location; //$NON-NLS-1$
			if (featureId != null)
				message = message + " in feature: " + featureId; //$NON-NLS-1$
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message));
		}
	}

	private void generateSiteReferences(Feature feature, IPublisherResult result, IPublisherInfo info) {
		//publish feature site references
		URLEntry updateURL = feature.getUpdateSite();
		//don't enable feature update sites by default since this results in too many
		//extra sites being loaded and searched (Bug 234177)
		if (updateURL != null)
			generateSiteReference(updateURL.getURL(), updateURL.getAnnotation(), feature.getId(), info.getMetadataRepository());
		URLEntry[] discoverySites = feature.getDiscoverySites();
		for (int i = 0; i < discoverySites.length; i++)
			generateSiteReference(discoverySites[i].getURL(), discoverySites[i].getAnnotation(), feature.getId(), info.getMetadataRepository());
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
					in = new BufferedInputStream(new FileInputStream(new File(location, file)));
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

	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		if (features == null && locations == null)
			throw new IllegalStateException("No features or locations provided");
		if (features == null)
			features = getFeatures(expandLocations(locations));
		generateFeatureIUs(features, results, info);
		return Status.OK_STATUS;
	}

	private void processTouchpointAdvice(InstallableUnitDescription iu, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, false, null, null, ITouchpointAdvice.class);
		ITouchpointData result = MetadataFactory.createTouchpointData(new HashMap());
		for (Iterator i = advice.iterator(); i.hasNext();) {
			ITouchpointAdvice entry = (ITouchpointAdvice) i.next();
			result = entry.getTouchpointData(result);
		}
		iu.addTouchpointData(result);
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
