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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.internal.p2.publisher.IPublishingAction;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Publish CUs for all the configuration data in the current result.
 * This adds config-specific CUs to capture start levels etc found in the config.ini
 * etc for is os, ws, arch combination seen so far.
 */
public class ConfigCUsAction extends Generator implements IPublishingAction {

	public static final int NO_INI = 0;
	public static final int CONFIG_INI = 1;
	public static final int LAUNCHER_INI = 2;
	private String version;
	private String id;
	private String flavor;
	int mode;

	public ConfigCUsAction(IPublisherInfo info, String flavor, String id, String version, int mode) {
		super(createGeneratorInfo(info, flavor));
		this.flavor = flavor;
		this.mode = mode;
		this.id = id;
		this.version = version;
	}

	private static IGeneratorInfo createGeneratorInfo(IPublisherInfo info, String flavor) {
		EclipseInstallGeneratorInfoProvider result = new EclipseInstallGeneratorInfoProvider();
		result.setArtifactRepository(info.getArtifactRepository());
		result.setMetadataRepository(info.getMetadataRepository());
		result.setPublishArtifactRepository(info.publishArtifactRepository());
		result.setPublishArtifacts(info.publishArtifacts());
		result.setFlavor(flavor);
		return result;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results) {
		// generation from remembered config.ini's
		// we have N platforms, generate a CU for each
		// TODO try and find common properties across platforms
		for (Iterator iterator = results.getConfigData().keySet().iterator(); iterator.hasNext();) {
			String configuration = (String) iterator.next();
			ConfigData data = (ConfigData) results.getConfigData().get(configuration);
			BundleInfo[] bundles = fillInBundles(data.getBundles(), results);
			generateBundleConfigIUs(bundles, results, configuration);
			publishIniIUs(data, results, configuration);
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
}
