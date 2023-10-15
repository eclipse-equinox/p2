/*******************************************************************************
 * Copyright (c) 2008, 2021 Code 9 and others.
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
 *   Rapicorp - ongoing development
 *   Christoph Läubrich - Bug 574952 p2 should distinguish between "product plugins" and "configuration plugins" (gently sponsored by Compart AG)
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.metadata.TouchpointInstruction;
import org.eclipse.equinox.internal.p2.publisher.eclipse.GeneratorBundleInfo;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Publish CUs for all the configuration data in the current result.
 * This adds config-specific CUs to capture start levels etc found in the config.ini
 * etc for is os, ws, arch combination seen so far.
 */
public class ConfigCUsAction extends AbstractPublisherAction {

	protected static final String DEFAULT_START_LEVEL = "osgi.bundles.defaultStartLevel"; //$NON-NLS-1$
	private static Collection<String> PROPERTIES_TO_SKIP;
	private static HashSet<String> PROGRAM_ARGS_TO_SKIP;
	protected Version version;
	protected String id;
	protected String flavor;
	IPublisherResult outerResults = null;

	// TODO consider moving this filtering to the LaunchingAdvice and ConfigAdvice so 
	// it is not hardcoded in the action.
	static {
		PROPERTIES_TO_SKIP = new HashSet<>();
		PROPERTIES_TO_SKIP.add("osgi.frameworkClassPath"); //$NON-NLS-1$
		PROPERTIES_TO_SKIP.add("osgi.framework"); //$NON-NLS-1$
		PROPERTIES_TO_SKIP.add("osgi.bundles"); //$NON-NLS-1$
		PROPERTIES_TO_SKIP.add("eof"); //$NON-NLS-1$
		PROPERTIES_TO_SKIP.add("eclipse.p2.profile"); //$NON-NLS-1$
		PROPERTIES_TO_SKIP.add("eclipse.p2.data.area"); //$NON-NLS-1$
		PROPERTIES_TO_SKIP.add("org.eclipse.update.reconcile"); //$NON-NLS-1$
		PROPERTIES_TO_SKIP.add("org.eclipse.equinox.simpleconfigurator.configUrl"); //$NON-NLS-1$

		PROGRAM_ARGS_TO_SKIP = new HashSet<>();
		PROGRAM_ARGS_TO_SKIP.add("--launcher.library"); //$NON-NLS-1$
		PROGRAM_ARGS_TO_SKIP.add("-startup"); //$NON-NLS-1$
		PROGRAM_ARGS_TO_SKIP.add("-configuration"); //$NON-NLS-1$
	}

	public static String getAbstractCUCapabilityNamespace(String id, String type, String flavor, String configSpec) {
		return flavor + id;
	}

	public static String getAbstractCUCapabilityId(String id, String type, String flavor, String configSpec) {
		return id + "." + type; //$NON-NLS-1$
	}

	/**
	 * Returns the id of the top level IU published by this action for the given id and flavor.
	 * @param id the id of the application being published
	 * @param flavor the flavor being published
	 * @return the if for ius published by this action
	 */
	public static String computeIUId(String id, String flavor) {
		return flavor + id + ".configuration"; //$NON-NLS-1$
	}

	public ConfigCUsAction(IPublisherInfo info, String flavor, String id, Version version) {
		this.flavor = flavor;
		this.id = id;
		this.version = version;
	}

	@Override
	public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
		IPublisherResult innerResult = new PublisherResult();
		this.outerResults = results;
		this.info = publisherInfo;
		// we have N platforms, generate a CU for each
		// TODO try and find common properties across platforms
		String[] configSpecs = publisherInfo.getConfigurations();
		for (String configSpec1 : configSpecs) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			String configSpec = configSpec1;
			Collection<IConfigAdvice> configAdvice = publisherInfo.getAdvice(configSpec, false, id, version, IConfigAdvice.class);
			BundleInfo[] bundles = fillInBundles(configAdvice, results);
			Set<String> configBundles = configAdvice.stream().filter(ProductFileAdvice.class::isInstance)
					.map(ProductFileAdvice.class::cast).flatMap(pca -> pca.getBundles(true))
					.map(BundleInfo::getSymbolicName).filter(Objects::nonNull)
					.distinct()
					.collect(Collectors.toSet());
			publishBundleCUs(publisherInfo, bundles, configSpec, innerResult, configBundles);
			publishConfigIUs(configAdvice, innerResult, configSpec);
			Collection<IExecutableAdvice> launchingAdvice = publisherInfo.getAdvice(configSpec, false, id, version, IExecutableAdvice.class);
			publishIniIUs(launchingAdvice, innerResult, configSpec);
		}
		// merge the IUs  into the final result as non-roots and create a parent IU that captures them all
		results.merge(innerResult, IPublisherResult.MERGE_ALL_NON_ROOT);
		publishTopLevelConfigurationIU(innerResult.getIUs(null, IPublisherResult.ROOT), results);
		return Status.OK_STATUS;
	}

	private void publishTopLevelConfigurationIU(Collection<? extends IVersionedId> children, IPublisherResult result) {
		InstallableUnitDescription descriptor = createParentIU(children, computeIUId(id, flavor), version);
		descriptor.setSingleton(true);
		IInstallableUnit rootIU = MetadataFactory.createInstallableUnit(descriptor);
		if (rootIU == null)
			return;
		result.addIU(rootIU, IPublisherResult.ROOT);
	}

	// there seem to be cases where the bundle infos are not filled in with symbolic name and version.
	// fill in the missing data.
	private BundleInfo[] fillInBundles(Collection<IConfigAdvice> configAdvice, IPublisherResult results) {
		ArrayList<BundleInfo> result = new ArrayList<>();
		for (IConfigAdvice advice : configAdvice) {

			int defaultStart = BundleInfo.NO_LEVEL;
			Map<String, String> adviceProperties = advice.getProperties();
			if (adviceProperties.containsKey(DEFAULT_START_LEVEL)) {
				try {
					defaultStart = Integer.parseInt(adviceProperties.get(DEFAULT_START_LEVEL));
				} catch (NumberFormatException e) {
					//don't know default
				}
			}

			BundleInfo[] bundles = advice.getBundles();
			for (BundleInfo bundleInfo : bundles) {
				if (bundleInfo.getStartLevel() != BundleInfo.NO_LEVEL && bundleInfo.getStartLevel() == defaultStart) {
					bundleInfo.setStartLevel(BundleInfo.NO_LEVEL);
				}
				// prime the result with the current info.  This will be replaced if there is more info...
				if ((bundleInfo.getSymbolicName() != null && bundleInfo.getVersion() != null) || bundleInfo.getLocation() == null) {
					result.add(bundleInfo);
				} else {
					try {
						File location = new File(bundleInfo.getLocation());
						Dictionary<String, String> manifest = BundlesAction.loadManifestIgnoringExceptions(location);
						if (manifest == null)
							continue;
						GeneratorBundleInfo newInfo = new GeneratorBundleInfo(bundleInfo);
						ManifestElement[] element = ManifestElement.parseHeader("dummy-bsn", manifest.get(Constants.BUNDLE_SYMBOLICNAME)); //$NON-NLS-1$
						newInfo.setSymbolicName(element[0].getValue());
						newInfo.setVersion(manifest.get(Constants.BUNDLE_VERSION));
						result.add(newInfo);
					} catch (BundleException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return result.toArray(new BundleInfo[result.size()]);
	}

	/**
	 * Publish the IUs that capture the eclipse.ini information such as vmargs and program args, etc
	 */
	private void publishIniIUs(Collection<IExecutableAdvice> launchingAdvice, IPublisherResult results, String configSpec) {
		if (launchingAdvice.isEmpty())
			return;

		String configureData = ""; //$NON-NLS-1$
		String unconfigureData = ""; //$NON-NLS-1$
		if (!launchingAdvice.isEmpty()) {
			String[] dataStrings = getLauncherConfigStrings(launchingAdvice);
			configureData += dataStrings[0];
			unconfigureData += dataStrings[1];
		}
		// if there is nothing to configure or unconfigure, then don't even bother generating this IU
		if (configureData.length() == 0 && unconfigureData.length() == 0)
			return;

		Map<String, String> touchpointData = new HashMap<>();
		touchpointData.put("configure", configureData); //$NON-NLS-1$
		touchpointData.put("unconfigure", unconfigureData); //$NON-NLS-1$
		IInstallableUnit cu = createCU(id, version, "ini", flavor, configSpec, touchpointData); //$NON-NLS-1$
		results.addIU(cu, IPublisherResult.ROOT);
	}

	/**
	 * Publish the IUs that capture the config.ini information such as properties etc
	 */
	private void publishConfigIUs(Collection<IConfigAdvice> configAdvice, IPublisherResult results, String configSpec) {
		if (configAdvice.isEmpty())
			return;

		String configureData = ""; //$NON-NLS-1$
		String unconfigureData = ""; //$NON-NLS-1$
		if (!configAdvice.isEmpty()) {
			String[] dataStrings = getConfigurationStrings(configAdvice);
			configureData += dataStrings[0];
			unconfigureData += dataStrings[1];
		}
		// if there is nothing to configure or unconfigure, then don't even bother generating this IU
		if (configureData.length() == 0 && unconfigureData.length() == 0)
			return;

		Map<String, String> touchpointData = new HashMap<>();
		touchpointData.put("configure", configureData); //$NON-NLS-1$
		touchpointData.put("unconfigure", unconfigureData); //$NON-NLS-1$
		IInstallableUnit cu = createCU(id, version, "config", flavor, configSpec, touchpointData); //$NON-NLS-1$
		results.addIU(cu, IPublisherResult.ROOT);
	}

	/**
	 * Create a CU whose id is flavor+id.type.configspec with the given version. 
	 * The resultant IU has the self capability and an abstract capabilty in the flavor+id namespace
	 * with the name id.type and the given version.  This allows others to create an abstract
	 * dependency on having one of these things around but not having to list out the configs.
	 */
	private IInstallableUnit createCU(String cuId, Version cuVersion, String cuType, String cuFlavor, String configSpec, Map<String, String> touchpointData) {
		InstallableUnitDescription cu = new InstallableUnitDescription();
		String resultId = createCUIdString(cuId, cuType, cuFlavor, configSpec);
		cu.setId(resultId);
		cu.setVersion(cuVersion);
		cu.setFilter(createFilterSpec(configSpec));
		IProvidedCapability selfCapability = PublisherHelper.createSelfCapability(resultId, cuVersion);
		String namespace = getAbstractCUCapabilityNamespace(cuId, cuType, cuFlavor, configSpec);
		String abstractId = getAbstractCUCapabilityId(cuId, cuType, cuFlavor, configSpec);
		IProvidedCapability abstractCapability = MetadataFactory.createProvidedCapability(namespace, abstractId, cuVersion);
		cu.setCapabilities(new IProvidedCapability[] {selfCapability, abstractCapability});
		cu.addTouchpointData(MetadataFactory.createTouchpointData(touchpointData));
		cu.setTouchpointType(PublisherHelper.TOUCHPOINT_OSGI);
		return MetadataFactory.createInstallableUnit(cu);
	}

	protected String[] getConfigurationStrings(Collection<IConfigAdvice> configAdvice) {
		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$
		Set<String> properties = new HashSet<>();
		for (IConfigAdvice advice : configAdvice) {
			for (Entry<String, String> aProperty : advice.getProperties().entrySet()) {
				String key = aProperty.getKey();
				if (shouldPublishProperty(key) && !properties.contains(key)) {
					properties.add(key);
					Map<String, String> parameters = new LinkedHashMap<>();
					parameters.put("propName", key); //$NON-NLS-1$
					parameters.put("propValue", aProperty.getValue()); //$NON-NLS-1$
					configurationData += TouchpointInstruction.encodeAction("setProgramProperty", parameters); //$NON-NLS-1$
					parameters.put("propValue", ""); //$NON-NLS-1$//$NON-NLS-2$
					unconfigurationData += TouchpointInstruction.encodeAction("setProgramProperty", parameters); //$NON-NLS-1$
				}
			}
			if (advice instanceof ProductFileAdvice) {
				for (IRepositoryReference repo : ((ProductFileAdvice) advice).getUpdateRepositories()) {
					Map<String, String> parameters = new LinkedHashMap<>();
					parameters.put("type", Integer.toString(repo.getType())); //$NON-NLS-1$
					parameters.put("location", repo.getLocation().toString()); //$NON-NLS-1$
					if (repo.getNickname() != null) {
						parameters.put("name", repo.getNickname()); //$NON-NLS-1$
					}
					parameters.put("enabled", Boolean.toString((repo.getOptions() & IRepository.ENABLED) == IRepository.ENABLED)); //$NON-NLS-1$
					configurationData += TouchpointInstruction.encodeAction("addRepository", parameters); //$NON-NLS-1$
					parameters.remove("enabled"); //$NON-NLS-1$
					unconfigurationData += TouchpointInstruction.encodeAction("removeRepository", parameters);//$NON-NLS-1$
				}
			}
		}
		return new String[] {configurationData, unconfigurationData};
	}

	private boolean shouldPublishProperty(String key) {
		return !PROPERTIES_TO_SKIP.contains(key);
	}

	private boolean shouldPublishJvmArg(String key) {
		return true;
	}

	private boolean shouldPublishProgramArg(String key) {
		return !PROGRAM_ARGS_TO_SKIP.contains(key);
	}

	protected String[] getLauncherConfigStrings(Collection<IExecutableAdvice> launchingAdvice) {
		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$

		Map<String, String> touchpointParameters = new LinkedHashMap<>();
		Set<String> jvmSet = new HashSet<>();
		Set<String> programSet = new HashSet<>();
		for (IExecutableAdvice advice : launchingAdvice) {
			String[] jvmArgs = advice.getVMArguments();
			for (String jvmArg : jvmArgs) {
				if (shouldPublishJvmArg(jvmArg) && !jvmSet.contains(jvmArg)) {
					jvmSet.add(jvmArg);
					touchpointParameters.clear();
					touchpointParameters.put("jvmArg", jvmArg); //$NON-NLS-1$
					configurationData += TouchpointInstruction.encodeAction("addJvmArg", touchpointParameters); //$NON-NLS-1$
					unconfigurationData += TouchpointInstruction.encodeAction("removeJvmArg", touchpointParameters); //$NON-NLS-1$
				}
			}
			String[] programArgs = advice.getProgramArguments();
			for (int i = 0; i < programArgs.length; i++)
				if (shouldPublishProgramArg(programArgs[i]) && !programSet.contains(programArgs[i])) {
					if (programArgs[i].startsWith("-")) //$NON-NLS-1$
						programSet.add(programArgs[i]);
					touchpointParameters.clear();
					touchpointParameters.put("programArg", programArgs[i]); //$NON-NLS-1$
					configurationData += TouchpointInstruction.encodeAction("addProgramArg", touchpointParameters); //$NON-NLS-1$
					unconfigurationData += TouchpointInstruction.encodeAction("removeProgramArg", touchpointParameters); //$NON-NLS-1$
				} else if (i + 1 < programArgs.length && !programArgs[i + 1].startsWith("-")) { //$NON-NLS-1$
					// if we are not publishing then skip over the following arg as it is assumed to be a parameter
					// to this command line arg.
					i++;
				}
		}
		return new String[] {configurationData, unconfigurationData};
	}

	/**
	 * Publish the CUs related to the given set of bundles. This generally covers
	 * the start-level and and whether or not the bundle is to be started.
	 * 
	 * @param configBundles
	 */
	protected void publishBundleCUs(IPublisherInfo publisherInfo, BundleInfo[] bundles, String configSpec,
			IPublisherResult result, Set<String> configBundles) {
		if (bundles == null)
			return;

		String cuIdPrefix = ""; //$NON-NLS-1$
		IMatchExpression<IInstallableUnit> filter = null;
		if (configSpec != null) {
			cuIdPrefix = createIdString(configSpec);
			filter = createFilterSpec(configSpec);
		}

		for (BundleInfo bundleInfo : bundles) {
			GeneratorBundleInfo generatorBundle = createGeneratorBundleInfo(bundleInfo, result);
			if (generatorBundle == null)
				continue;
			IInstallableUnit iu = generatorBundle.getIU();
			// If there is no host, or the filters don't match, skip this one.
			if (iu == null || !filterMatches(iu.getFilter(), configSpec))
				continue;
			if (generatorBundle.getStartLevel() == BundleInfo.NO_LEVEL && !generatorBundle.isMarkedAsStarted()) {
				// this bundle does not require any particular configuration, the plug-in
				// default IU will handle installing it
				continue;
			}
			IInstallableUnit cu;
			if (configBundles.contains(bundleInfo.getSymbolicName())) {
				cu = BundlesAction.createBundleConfigurationUnit(generatorBundle.getSymbolicName(), this.version, false, generatorBundle,
						flavor + cuIdPrefix, filter, true);
			} else if (this.version != null && !this.version.equals(Version.emptyVersion)) {
				cu = BundlesAction.createBundleConfigurationUnit(generatorBundle.getSymbolicName(), this.version, false, generatorBundle, flavor + cuIdPrefix, filter);
			} else {
				cu = BundlesAction.createBundleConfigurationUnit(generatorBundle.getSymbolicName(), Version.parseVersion(generatorBundle.getVersion()), false, generatorBundle, flavor + cuIdPrefix, filter);
			}
			if (cu != null) {
				// Product Query will run against the repo, make sure these CUs are in before then
				// TODO review the aggressive addition to the metadata repo.  perhaps the query can query the result as well.
				//				IMetadataRepository metadataRepository = info.getMetadataRepository();
				//				if (metadataRepository != null) {
				//					metadataRepository.addInstallableUnits(new IInstallableUnit[] {cu});
				//				}
				result.addIU(cu, IPublisherResult.ROOT);
			}
		}
	}

	protected GeneratorBundleInfo createGeneratorBundleInfo(BundleInfo bundleInfo, IPublisherResult result) {
		String name = bundleInfo.getSymbolicName();

		//query for a matching IU
		IInstallableUnit iu = queryForIU(outerResults, name, Version.create(bundleInfo.getVersion()));
		if (iu != null) {
			if (iu.getVersion() == null)
				bundleInfo.setVersion("0.0.0"); //$NON-NLS-1$
			else
				bundleInfo.setVersion(iu.getVersion().toString());
			GeneratorBundleInfo newInfo = new GeneratorBundleInfo(bundleInfo);
			newInfo.setIU(iu);
			return newInfo;
		}

		if (bundleInfo.getLocation() != null || bundleInfo.getVersion() != null)
			return new GeneratorBundleInfo(bundleInfo);
		//harder: try id_version
		int i = name.indexOf('_');
		while (i > -1) {
			try {
				Version bundleVersion = Version.parseVersion(name.substring(i));
				bundleInfo.setSymbolicName(name.substring(0, i));
				bundleInfo.setVersion(bundleVersion.toString());
				return new GeneratorBundleInfo(bundleInfo);
			} catch (IllegalArgumentException e) {
				// the '_' found was probably part of the symbolic id
				i = name.indexOf('_', i);
			}
		}

		return null;
	}

}
