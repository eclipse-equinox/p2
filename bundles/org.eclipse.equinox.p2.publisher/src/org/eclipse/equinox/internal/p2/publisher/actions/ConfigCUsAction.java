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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

/**
 * Publish CUs for all the configuration data in the current result.
 * This adds config-specific CUs to capture start levels etc found in the config.ini
 * etc for is os, ws, arch combination seen so far.
 */
public class ConfigCUsAction extends AbstractPublishingAction {

	protected static final String ORG_ECLIPSE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$

	public static final int NO_INI = 0;
	public static final int CONFIG_INI = 1;
	public static final int LAUNCHER_INI = 2;
	private String version;
	private String id;
	private String flavor;
	int mode;

	public ConfigCUsAction(IPublisherInfo info, String flavor, String id, String version, int mode) {
		this.flavor = flavor;
		this.mode = mode;
		this.id = id;
		this.version = version;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		// generation from remembered config.ini's
		// we have N platforms, generate a CU for each
		// TODO try and find common properties across platforms
		for (Iterator iterator = results.getConfigData().keySet().iterator(); iterator.hasNext();) {
			String configSpec = (String) iterator.next();
			ConfigData data = (ConfigData) results.getConfigData().get(configSpec);
			BundleInfo[] bundles = fillInBundles(data.getBundles(), results);
			generateBundleConfigIUs(info, bundles, configSpec, results);
			publishIniIUs(data, results, configSpec);
		}
		return Status.OK_STATUS;
	}

	// there seem to be cases where the bundle infos are not filled in with symbolic name and version.
	// fill in the missing data.
	private BundleInfo[] fillInBundles(BundleInfo[] bundles, IPublisherResult results) {
		BundleInfo[] result = new BundleInfo[bundles.length];
		for (int i = 0; i < bundles.length; i++) {
			BundleInfo bundleInfo = bundles[i];
			// prime the result with the current info.  This will be replaced if there is more info...
			result[i] = bundleInfo;
			if (bundleInfo.getSymbolicName() != null && bundleInfo.getVersion() != null)
				continue;
			if (bundleInfo.getLocation() == null)
				continue;
			// read the manifest to get some more info.
			try {
				File location = new File(new URL(bundleInfo.getLocation()).getPath());
				Dictionary manifest = BundleDescriptionFactory.loadManifest(location);
				if (manifest == null)
					continue;
				GeneratorBundleInfo newInfo = new GeneratorBundleInfo(bundleInfo);
				ManifestElement[] element = ManifestElement.parseHeader("dummy-bsn", (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME)); //$NON-NLS-1$
				newInfo.setSymbolicName(element[0].getValue());
				newInfo.setVersion((String) manifest.get(Constants.BUNDLE_VERSION));
				result[i] = newInfo;
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

	private void publishIniIUs(ConfigData configData, IPublisherResult results, String configuration) {
		if (mode == NO_INI)
			return;

		String configureData = ""; //$NON-NLS-1$
		String unconfigureData = ""; //$NON-NLS-1$

		if ((mode & CONFIG_INI) > 0 && configData != null) {
			String[] dataStrings = getConfigurationStrings(configData);
			configureData += dataStrings[0];
			unconfigureData += dataStrings[1];
		}

		//		if ((mode & LAUNCHER_INI) > 0) {
		//			LauncherData launcherData = info.getLauncherData();
		//			if (launcherData != null) {
		//				String[] dataStrings = getLauncherConfigStrings(launcherData.getJvmArgs(), launcherData.getProgramArgs());
		//				configureData += dataStrings[0];
		//				unconfigureData += dataStrings[1];
		//			}
		//		}

		// if there is nothing to configure or unconfigure, then don't even bother generating this IU
		if (configureData.length() == 0 && unconfigureData.length() == 0)
			return;

		Map touchpointData = new HashMap();
		touchpointData.put("configure", configureData); //$NON-NLS-1$
		touchpointData.put("unconfigure", unconfigureData); //$NON-NLS-1$
		IInstallableUnit cu = MetadataGeneratorHelper.createIUFragment(id, version, flavor, configuration, touchpointData);
		results.addIU(cu, IPublisherResult.ROOT);
	}

	protected String[] getConfigurationStrings(ConfigData configData) {
		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$
		for (Iterator iterator = configData.getFwDependentProps().entrySet().iterator(); iterator.hasNext();) {
			Entry aProperty = (Entry) iterator.next();
			String key = ((String) aProperty.getKey());
			if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				continue;
			configurationData += "setProgramProperty(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			unconfigurationData += "setProgramProperty(propName:" + key + ", propValue:);"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (Iterator iterator = configData.getFwIndependentProps().entrySet().iterator(); iterator.hasNext();) {
			Entry aProperty = (Entry) iterator.next();
			String key = ((String) aProperty.getKey());
			if (key.equals("osgi.frameworkClassPath") || key.equals("osgi.framework") || key.equals("osgi.bundles") || key.equals("eof")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				continue;
			configurationData += "setProgramProperty(propName:" + key + ", propValue:" + ((String) aProperty.getValue()) + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			unconfigurationData += "setProgramProperty(propName:" + key + ", propValue:);"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		return new String[] {configurationData, unconfigurationData};
	}

	protected String[] getLauncherConfigStrings(final String[] jvmArgs, final String[] programArgs) {
		String configurationData = ""; //$NON-NLS-1$
		String unconfigurationData = ""; //$NON-NLS-1$

		for (int i = 0; i < jvmArgs.length; i++) {
			configurationData += "addJvmArg(jvmArg:" + jvmArgs[i] + ");"; //$NON-NLS-1$ //$NON-NLS-2$
			unconfigurationData += "removeJvmArg(jvmArg:" + jvmArgs[i] + ");"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		for (int i = 0; i < programArgs.length; i++) {
			String programArg = programArgs[i];
			if (programArg.equals("--launcher.library") || programArg.equals("-startup") || programArg.equals("-configuration")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				i++;
			configurationData += "addProgramArg(programArg:" + programArg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
			unconfigurationData += "removeProgramArg(programArg:" + programArg + ");"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new String[] {configurationData, unconfigurationData};
	}

	protected void generateBundleConfigIUs(IPublisherInfo info, BundleInfo[] bundles, String configSpec, IPublisherResult result) {
		if (bundles == null)
			return;

		String cuIdPrefix = ""; //$NON-NLS-1$
		String filter = null;
		if (configSpec != null) {
			cuIdPrefix = createIdString(configSpec);
			filter = createFilterSpec(configSpec);
		}

		for (int i = 0; i < bundles.length; i++) {
			GeneratorBundleInfo bundle = createGeneratorBundleInfo(bundles[i], result);
			if (bundle == null)
				continue;

			if (bundle.getSymbolicName().equals(ORG_ECLIPSE_UPDATE_CONFIGURATOR)) {
				bundle.setStartLevel(BundleInfo.NO_LEVEL);
				bundle.setMarkedAsStarted(false);
				bundle.setSpecialConfigCommands("setProgramProperty(propName:org.eclipse.update.reconcile, propValue:false);"); //$NON-NLS-1$
				bundle.setSpecialUnconfigCommands("setProgramProperty(propName:org.eclipse.update.reconcile, propValue:);"); //$NON-NLS-1$
			} else if (bundle.getStartLevel() == BundleInfo.NO_LEVEL && !bundle.isMarkedAsStarted()) {
				// this bundle does not require any particular configuration, the plug-in default IU will handle installing it
				continue;
			}

			IInstallableUnit cu = MetadataGeneratorHelper.createBundleConfigurationUnit(bundle.getSymbolicName(), new Version(bundle.getVersion()), false, bundle, flavor + cuIdPrefix, filter);
			if (cu != null) {
				// Product Query will run against the repo, make sure these CUs are in before then
				IMetadataRepository metadataRepository = info.getMetadataRepository();
				if (metadataRepository != null) {
					metadataRepository.addInstallableUnits(new IInstallableUnit[] {cu});
				}
				result.addIU(cu, IPublisherResult.ROOT);
				//				String key = (product != null && product.useFeatures()) ? IPublisherResult.CONFIGURATION_CUS : bundle.getSymbolicName();
				String key = bundle.getSymbolicName();
				result.addFragment(key, cu);
			}
		}
	}

	protected GeneratorBundleInfo createGeneratorBundleInfo(BundleInfo bundleInfo, IPublisherResult result) {
		if (bundleInfo.getLocation() != null)
			return new GeneratorBundleInfo(bundleInfo);

		String name = bundleInfo.getSymbolicName();

		//easy case: do we have a matching IU?
		IInstallableUnit iu = result.getIU(name, null);
		if (iu != null) {
			bundleInfo.setVersion(iu.getVersion().toString());
			return new GeneratorBundleInfo(bundleInfo);
		}

		//harder: try id_version
		int i = name.indexOf('_');
		while (i > -1) {
			Version version = null;
			try {
				version = new Version(name.substring(i));
				bundleInfo.setSymbolicName(name.substring(0, i));
				bundleInfo.setVersion(version.toString());
				return new GeneratorBundleInfo(bundleInfo);
			} catch (IllegalArgumentException e) {
				// the '_' found was probably part of the symbolic id
				i = name.indexOf('_', i);
			}
		}

		return null;
	}

}
