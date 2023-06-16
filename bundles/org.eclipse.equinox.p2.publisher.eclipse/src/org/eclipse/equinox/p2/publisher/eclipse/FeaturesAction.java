/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.internal.p2.publisher.Messages;
import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitPatchDescription;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.publishing.Activator;

/**
 * Publish IUs for all of the features in the given set of locations.  The locations can
 * be actual locations of the features or folders of features.
 */
public class FeaturesAction extends AbstractPublisherAction {
	public static final String INSTALL_FEATURES_FILTER = "(org.eclipse.update.install.features=true)"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_APPLICATION_PROP = "org.eclipse.update.feature.application"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_PLUGIN_PROP = "org.eclipse.update.feature.plugin"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_EXCLUSIVE_PROP = "org.eclipse.update.feature.exclusive"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_PRIMARY_PROP = "org.eclipse.update.feature.primary"; //$NON-NLS-1$

	protected Feature[] features;
	private File[] locations;

	public static IArtifactKey createFeatureArtifactKey(String id, String version) {
		return new ArtifactKey(PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER, id, Version.parseVersion(version));
	}

	public static IInstallableUnit createFeatureJarIU(Feature feature, IPublisherInfo info) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		String id = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/false);
		iu.setId(id);
		Version version = Version.parseVersion(feature.getVersion());
		iu.setVersion(version);

		// set properties for other feature attributes
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
		if (feature.getBrandingPlugin() != null)
			iu.setProperty(UPDATE_FEATURE_PLUGIN_PROP, feature.getBrandingPlugin());
		if (feature.isExclusive())
			iu.setProperty(UPDATE_FEATURE_EXCLUSIVE_PROP, Boolean.TRUE.toString());
		if (feature.isPrimary())
			iu.setProperty(UPDATE_FEATURE_PRIMARY_PROP, Boolean.TRUE.toString());

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

		ArrayList<IProvidedCapability> providedCapabilities = new ArrayList<>();
		providedCapabilities.add(PublisherHelper.createSelfCapability(id, version));
		providedCapabilities.add(PublisherHelper.FEATURE_CAPABILITY);
		providedCapabilities.add(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_UPDATE_FEATURE, feature.getId(), version));

		iu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(id, version), PublisherHelper.FEATURE_CAPABILITY, MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_UPDATE_FEATURE, feature.getId(), version)});
		iu.setArtifacts(new IArtifactKey[] {createFeatureArtifactKey(feature.getId(), version.toString())});

		Map<String, String> touchpointData = new HashMap<>();
		touchpointData.put("zipped", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		Map<Locale, Map<String, String>> localizations = feature.getLocalizations();
		if (localizations != null) {
			for (Entry<Locale, Map<String, String>> locEntry : localizations.entrySet()) {
				Locale locale = locEntry.getKey();
				Map<String, String> translatedStrings = locEntry.getValue();
				for (Entry<String, String> entry : translatedStrings.entrySet()) {
					iu.setProperty(locale.toString() + '.' + entry.getKey(), entry.getValue());
				}
				providedCapabilities.add(PublisherHelper.makeTranslationCapability(id, locale));
			}
		}

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

	/**
	 * Looks for advice in a p2.inf file inside the feature location.
	 */
	private void createAdviceFileAdvice(Feature feature, IPublisherInfo publisherInfo) {
		//assume p2.inf is co-located with feature.xml
		String location = feature.getLocation();
		if (location != null) {
			String groupId = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/true);
			AdviceFileAdvice advice = new AdviceFileAdvice(groupId, Version.parseVersion(feature.getVersion()), IPath.fromOSString(location), IPath.fromOSString("p2.inf")); //$NON-NLS-1$
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
		for (FeatureEntry entry : entries) {
			if (entry.isUnpack() && entry.isPlugin() && !entry.isRequires())
				publisherInfo.addAdvice(new BundleShapeAdvice(entry.getId(), Version.parseVersion(entry.getVersion()), IBundleShapeAdvice.DIR));
		}
	}

	protected IInstallableUnit createFeatureRootFileIU(String featureId, String featureVersion, File location, FileSetDescriptor descriptor) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(true);
		String id = featureId + '_' + descriptor.getKey();
		iu.setId(id);
		Version version = Version.parseVersion(featureVersion);
		iu.setVersion(version);
		iu.setCapabilities(new IProvidedCapability[] {PublisherHelper.createSelfCapability(id, version)});
		iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
		String configSpec = descriptor.getConfigSpec();
		if (configSpec != null && configSpec.length() > 0)
			iu.setFilter(createFilterSpec(configSpec));

		Map<String, String> touchpointData = new HashMap<>(2);
		String configurationData = "unzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("install", configurationData); //$NON-NLS-1$
		String unConfigurationData = "cleanupzip(source:@artifact, target:${installFolder});"; //$NON-NLS-1$
		touchpointData.put("uninstall", unConfigurationData); //$NON-NLS-1$
		iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));

		// prime the IU with an artifact key that will correspond to the zipped up root files.
		IArtifactKey key = new ArtifactKey(PublisherHelper.BINARY_ARTIFACT_CLASSIFIER, iu.getId(), iu.getVersion());
		iu.setArtifacts(new IArtifactKey[] {key});
		setupLinks(iu, descriptor);
		setupPermissions(iu, descriptor);

		IInstallableUnit iuResult = MetadataFactory.createInstallableUnit(iu);
		// need to return both the iu and any files.
		return iuResult;
	}

	protected IInstallableUnit createGroupIU(Feature feature, List<IInstallableUnit> childIUs, IPublisherInfo publisherInfo) {
		if (isPatch(feature))
			return createPatchIU(feature, childIUs, publisherInfo);
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		String id = getGroupId(feature.getId());
		iu.setId(id);
		Version version = PublisherHelper.fromOSGiVersion(new org.osgi.framework.Version(feature.getVersion()));
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
		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, BundlesAction.computeUpdateRange(new org.osgi.framework.Version(feature.getVersion())), IUpdateDescriptor.NORMAL, null));

		FeatureEntry entries[] = feature.getEntries();
		List<IRequirement> required = new ArrayList<>(entries.length + (childIUs == null ? 0 : childIUs.size()));
		for (FeatureEntry entry : entries) {
			VersionRange range = getVersionRange(entry);
			String requiredId = getTransformedId(entry.getId(), entry.isPlugin(), /*isGroup*/true);
			required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, requiredId, range, getFilter(entry), entry.isOptional(), false));
		}

		// link in all the children (if any) as requirements.
		// TODO consider if these should be linked as exact version numbers.  Should be ok but may be brittle.
		if (childIUs != null) {
			for (IInstallableUnit child : childIUs) {
				IMatchExpression<IInstallableUnit> filter = child.getFilter();
				required.add(MetadataFactory.createRequirement(PublisherHelper.IU_NAMESPACE, child.getId(), new VersionRange(child.getVersion(), true, child.getVersion(), true), filter, false, false));
			}
		}
		iu.setRequirements(required.toArray(new IRequirement[required.size()]));
		iu.setTouchpointType(ITouchpointType.NONE);
		iu.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		processTouchpointAdvice(iu, null, publisherInfo);
		processInstallableUnitPropertiesAdvice(iu, publisherInfo);

		//Create a fake entry to reuse the logic to create the filters
		FeatureEntry entry = new FeatureEntry("fake", "0.0.0", false); //$NON-NLS-1$ //$NON-NLS-2$
		entry.setEnvironment(feature.getOS(), feature.getWS(), feature.getArch(), feature.getNL());
		iu.setFilter(getFilter(entry));

		// Create set of provided capabilities
		ArrayList<IProvidedCapability> providedCapabilities = new ArrayList<>();
		providedCapabilities.add(createSelfCapability(id, version));

		Map<Locale, Map<String, String>> localizations = feature.getLocalizations();
		if (localizations != null) {
			for (Entry<Locale, Map<String, String>> locEntry : localizations.entrySet()) {
				Locale locale = locEntry.getKey();
				Map<String, String> translatedStrings = locEntry.getValue();
				for (Entry<String, String> e : translatedStrings.entrySet()) {
					iu.setProperty(locale.toString() + '.' + e.getKey(), e.getValue());
				}
				providedCapabilities.add(PublisherHelper.makeTranslationCapability(id, locale));
			}
		}

		iu.setCapabilities(providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));
		processUpdateDescriptorAdvice(iu, info);
		processCapabilityAdvice(iu, publisherInfo);
		return MetadataFactory.createInstallableUnit(iu);
	}

	protected String getGroupId(String featureId) {
		return getTransformedId(featureId, /*isPlugin*/false, /*isGroup*/true);
	}

	private IInstallableUnit createPatchIU(Feature feature, List<IInstallableUnit> childIUs, IPublisherInfo publisherInfo) {
		InstallableUnitPatchDescription iu = new MetadataFactory.InstallableUnitPatchDescription();
		String id = getTransformedId(feature.getId(), /*isPlugin*/false, /*isGroup*/true);
		iu.setId(id);
		Version version = Version.parseVersion(feature.getVersion());
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
		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, BundlesAction.computeUpdateRange(new org.osgi.framework.Version(feature.getVersion())), IUpdateDescriptor.NORMAL, null));

		FeatureEntry entries[] = feature.getEntries();
		ArrayList<IRequirement> applicabilityScope = new ArrayList<>();
		ArrayList<IRequirement> patchRequirements = new ArrayList<>();
		ArrayList<IRequirementChange> requirementChanges = new ArrayList<>();
		for (FeatureEntry entry : entries) {
			VersionRange range = getVersionRange(entry);
			IRequirement req = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, getTransformedId(entry.getId(), entry.isPlugin(), /*isGroup*/true), range, getFilter(entry), entry.isOptional(), false);
			if (entry.isRequires()) {
				applicabilityScope.add(req);
				if (applicabilityScope.size() == 1) {
					iu.setLifeCycle(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, getTransformedId(entry.getId(), entry.isPlugin(), /*isGroup*/true), range, null, false, false, false));
				}
				continue;
			}
			if (entry.isPlugin()) {
				IRequirement from = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, getTransformedId(entry.getId(), entry.isPlugin(), /*isGroup*/true), VersionRange.emptyRange, getFilter(entry), entry.isOptional(), false);
				requirementChanges.add(MetadataFactory.createRequirementChange(from, req));
				continue;
			}
			patchRequirements.add(req);
		}

		//Always add a requirement on the IU containing the feature jar
		if (childIUs != null) {
			for (IInstallableUnit child : childIUs) {
				patchRequirements.add(MetadataFactory.createRequirement(PublisherHelper.IU_NAMESPACE, child.getId(), new VersionRange(child.getVersion(), true, child.getVersion(), true), child.getFilter(), false, false));
			}
		}
		iu.setRequirements(patchRequirements.toArray(new IRequirement[patchRequirements.size()]));
		iu.setApplicabilityScope(new IRequirement[][] {applicabilityScope.toArray(new IRequirement[applicabilityScope.size()])});
		iu.setRequirementChanges(requirementChanges.toArray(new IRequirementChange[requirementChanges.size()]));

		iu.setTouchpointType(ITouchpointType.NONE);
		processTouchpointAdvice(iu, null, publisherInfo);
		processInstallableUnitPropertiesAdvice(iu, publisherInfo);
		iu.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		iu.setProperty(InstallableUnitDescription.PROP_TYPE_PATCH, Boolean.TRUE.toString());
		// TODO: shouldn't the filter for the group be constructed from os, ws, arch, nl
		// 		 of the feature?
		// iu.setFilter(filter);

		// Create set of provided capabilities
		ArrayList<IProvidedCapability> providedCapabilities = new ArrayList<>();
		providedCapabilities.add(createSelfCapability(id, version));

		Map<Locale, Map<String, String>> localizations = feature.getLocalizations();
		if (localizations != null) {
			for (Entry<Locale, Map<String, String>> locEntry : localizations.entrySet()) {
				Locale locale = locEntry.getKey();
				Map<String, String> translatedStrings = locEntry.getValue();
				for (Entry<String, String> e : translatedStrings.entrySet()) {
					iu.setProperty(locale.toString() + '.' + e.getKey(), e.getValue());
				}
				providedCapabilities.add(PublisherHelper.makeTranslationCapability(id, locale));
			}
		}

		iu.setCapabilities(providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));
		processUpdateDescriptorAdvice(iu, info);
		processCapabilityAdvice(iu, publisherInfo);
		return MetadataFactory.createInstallableUnitPatch(iu);
	}

	private File[] expandLocations(File[] list) {
		ArrayList<File> result = new ArrayList<>();
		expandLocations(list, result);
		return result.toArray(new File[result.size()]);
	}

	private void expandLocations(File[] list, ArrayList<File> result) {
		if (list == null)
			return;
		for (File location : list) {
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
		for (Feature feature : featureList) {
			//first gather any advice that might help us
			createBundleShapeAdvice(feature, info);
			createAdviceFileAdvice(feature, info);

			ArrayList<IInstallableUnit> childIUs = new ArrayList<>();

			IInstallableUnit featureJarIU = queryForIU(result, getTransformedId(feature.getId(), false, false), Version.parseVersion(feature.getVersion()));
			if (featureJarIU == null)
				featureJarIU = generateFeatureJarIU(feature, info);

			if (featureJarIU != null) {
				publishFeatureArtifacts(feature, featureJarIU, info);
				result.addIU(featureJarIU, IPublisherResult.NON_ROOT);
				childIUs.add(featureJarIU);
			}

			IInstallableUnit groupIU = queryForIU(result, getGroupId(feature.getId()), Version.parseVersion(feature.getVersion()));
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

	protected ArrayList<IInstallableUnit> generateRootFileIUs(Feature feature, IPublisherResult result, IPublisherInfo publisherInfo) {
		ArrayList<IInstallableUnit> ius = new ArrayList<>();

		Collection<IFeatureRootAdvice> collection = publisherInfo.getAdvice(null, false, feature.getId(), Version.parseVersion(feature.getVersion()), IFeatureRootAdvice.class);
		if (collection.size() == 0)
			return ius;

		IFeatureRootAdvice advice = collection.iterator().next();
		String[] configs = advice.getConfigurations();
		for (String config : configs) {
			FileSetDescriptor descriptor = advice.getDescriptor(config);
			if (descriptor != null && descriptor.size() > 0) {
				IInstallableUnit iu = createFeatureRootFileIU(feature.getId(), feature.getVersion(), null, descriptor);

				File[] files = descriptor.getFiles();
				IArtifactKey artifactKey = iu.getArtifacts().iterator().next();
				IArtifactDescriptor artifactDescriptor = PublisherHelper.createArtifactDescriptor(info, artifactKey, null);
				IPathComputer computer = advice.getRootFileComputer(config);
				if (computer == null)
					computer = FileUtils.createDynamicPathComputer(1);
				publishArtifact(artifactDescriptor, files, null, publisherInfo, computer);

				result.addIU(iu, IPublisherResult.NON_ROOT);
				ius.add(iu);
			}
		}

		return ius;
	}

	/**
	 * Generates and publishes a reference to an update site location
	 * @param location The update site location
	 * @param nickname The update site label
	 * @param featureId the identifier of the feature where the error occurred, or null
	 * @param collector The list into which the references are added
	 */
	private void generateSiteReference(String location, String nickname, String featureId, List<IRepositoryReference> collector) {
		if (location == null) {
			String message = featureId == null ? NLS.bind(Messages.exception_invalidSiteReference, location) : NLS.bind(Messages.exception_invalidSiteReferenceInFeature, location, featureId);
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message));
			return;
		}

		try {
			URI associateLocation = new URI(location);
			collector.add(new RepositoryReference(associateLocation, nickname, IRepository.TYPE_METADATA, IRepository.NONE));
			collector.add(new RepositoryReference(associateLocation, nickname, IRepository.TYPE_ARTIFACT, IRepository.NONE));
		} catch (URISyntaxException e) {
			String message = featureId == null ? NLS.bind(Messages.exception_invalidSiteReference, location) : NLS.bind(Messages.exception_invalidSiteReferenceInFeature, location, featureId);
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message));
		}
	}

	protected void generateSiteReferences(Feature feature, IPublisherResult result, IPublisherInfo publisherInfo) {
		if (publisherInfo.getMetadataRepository() == null)
			return;

		//publish feature site references
		URLEntry updateURL = feature.getUpdateSite();
		//don't enable feature update sites by default since this results in too many
		//extra sites being loaded and searched (Bug 234177)
		List<IRepositoryReference> collector = new ArrayList<>();
		if (updateURL != null)
			generateSiteReference(updateURL.getURL(), updateURL.getAnnotation(), feature.getId(), collector);
		URLEntry[] discoverySites = feature.getDiscoverySites();
		for (URLEntry discoverySite : discoverySites) {
			generateSiteReference(discoverySite.getURL(), discoverySite.getAnnotation(), feature.getId(), collector);
		}
		if (!collector.isEmpty())
			publisherInfo.getMetadataRepository().addReferences(collector);
	}

	protected Feature[] getFeatures(File[] featureLocations) {
		ArrayList<Feature> result = new ArrayList<>(featureLocations.length);
		for (File featureLocation : featureLocations) {
			Feature feature = new FeatureParser().parse(featureLocation);
			if (feature != null) {
				feature.setLocation(featureLocation.getAbsolutePath());
				result.add(feature);
			}
		}
		return result.toArray(new Feature[result.size()]);
	}

	private IMatchExpression<IInstallableUnit> getFilter(FeatureEntry entry) {
		StringBuffer result = new StringBuffer();
		result.append("(&"); //$NON-NLS-1$
		if (entry.getFilter() != null)
			result.append(entry.getFilter());
		expandFilter(entry.getOS(), "osgi.os", result); //$NON-NLS-1$
		expandFilter(entry.getWS(), "osgi.ws", result); //$NON-NLS-1$
		expandFilter(entry.getArch(), "osgi.arch", result);//$NON-NLS-1$
		expandFilter(entry.getNL(), "osgi.nl", result); //$NON-NLS-1$
		if (result.length() == 2)
			return null;
		result.append(')');
		return InstallableUnit.parseFilter(result.toString());
	}

	private void expandFilter(String filter, String osgiFilterValue, StringBuffer result) {
		if (filter != null && filter.length() != 0) {
			StringTokenizer token = new StringTokenizer(filter, ","); //$NON-NLS-1$
			if (token.countTokens() == 1)
				result.append('(' + osgiFilterValue + '=' + filter + ')');
			else {
				result.append("(|"); //$NON-NLS-1$
				while (token.hasMoreElements()) {
					result.append('(' + osgiFilterValue + '=' + token.nextToken() + ')');
				}
				result.append(')');
			}
		}
	}

	protected VersionRange getVersionRange(FeatureEntry entry) {
		String versionSpec = entry.getVersion();
		if (versionSpec == null)
			return VersionRange.emptyRange;
		String match = entry.getMatch();
		if ("versionRange".equals(match)) //$NON-NLS-1$
			return VersionRange.create(versionSpec);
		Version version = Version.parseVersion(versionSpec);
		if (version.equals(Version.emptyVersion))
			return VersionRange.emptyRange;
		if (!entry.isRequires())
			return new VersionRange(version, true, version, true);
		if (match == null)
			// TODO should really be returning VersionRange.emptyRange here...
			return null;
		if (match.equals("perfect")) //$NON-NLS-1$
			return new VersionRange(version, true, version, true);

		org.osgi.framework.Version osgiVersion = PublisherHelper.toOSGiVersion(version);
		if (match.equals("equivalent")) { //$NON-NLS-1$
			Version upper = Version.createOSGi(osgiVersion.getMajor(), osgiVersion.getMinor() + 1, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("compatible")) { //$NON-NLS-1$
			Version upper = Version.createOSGi(osgiVersion.getMajor() + 1, 0, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("greaterOrEqual")) //$NON-NLS-1$
			return new VersionRange(version, true, Version.MAX_VERSION, true);
		return null;
	}

	private boolean isPatch(Feature feature) {
		FeatureEntry[] entries = feature.getEntries();
		for (FeatureEntry entry : entries) {
			if (entry.isPatch()) {
				return true;
			}
		}
		return false;
	}

	@Override
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
		Collection<IArtifactKey> artifacts = featureIU.getArtifacts();
		for (IArtifactKey artifactKey : artifacts) {
			String location = feature.getLocation();
			if (location == null) {
				if (PublisherHelper.isArtifactPublish(publisherInfo)) {
					throw new IllegalArgumentException(NLS.bind(Messages.exception_sourcePath, feature.getId()));
				}
				continue;
			}
			File file = new File(location);
			ArtifactDescriptor ad = (ArtifactDescriptor) PublisherHelper.createArtifactDescriptor(info, artifactKey, file);
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
		String[] links = getArrayFromString(descriptor.getLinks(), ","); //$NON-NLS-1$
		StringBuilder linkActions = new StringBuilder();

		int i = 0;
		while (i < links.length) {
			//format is [target,name]*
			String target = links[i++];
			if (i < links.length) {
				String name = links[i++];
				linkActions.append("ln(linkTarget:" + target); //$NON-NLS-1$
				linkActions.append(",targetDir:${installFolder},linkName:" + name); //$NON-NLS-1$
				linkActions.append(");"); //$NON-NLS-1$
			}

		}

		if (linkActions.length() > 0) {
			Map<String, String> touchpointData = new HashMap<>();
			//we do ln during configure to avoid complicating branding which uses the install phase
			touchpointData.put("configure", linkActions.toString()); //$NON-NLS-1$
			iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		}
	}

	private void setupPermissions(InstallableUnitDescription iu, FileSetDescriptor descriptor) {
		Map<String, String> touchpointData = new HashMap<>();
		String[][] permsList = descriptor.getPermissions();
		for (String[] permSpec : permsList) {
			String configurationData = " chmod(targetDir:${installFolder}, targetFile:" + permSpec[1] + ", permissions:" + permSpec[0] + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			touchpointData.put("install", configurationData); //$NON-NLS-1$
			iu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		}
	}
}
