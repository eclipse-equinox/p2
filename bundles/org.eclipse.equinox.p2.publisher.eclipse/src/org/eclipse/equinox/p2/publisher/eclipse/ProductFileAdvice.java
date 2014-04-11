/*******************************************************************************
 * Copyright (c) 2008, 2014 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   EclipseSource - ongoing development
 *   IBM Corporation - ongoing development
 *   Rapicorp - additional features
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.publisher.eclipse.DataLoader;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.actions.ILicenseAdvice;
import org.eclipse.equinox.p2.repository.IRepositoryReference;

/**
 * Provide advice derived from the .product file.  The product can give some info on 
 * launching as well as the configuration (bundles, properties, ...)
 */
public class ProductFileAdvice extends AbstractAdvice implements ILicenseAdvice, IExecutableAdvice, IConfigAdvice, IBrandingAdvice {
	private final static String OSGI_SPLASH_PATH = "osgi.splashPath"; //$NON-NLS-1$
	private final static String SPLASH_PREFIX = "platform:/base/plugins/"; //$NON-NLS-1$
	private final IProductDescriptor product;
	private final String configSpec;
	private String ws;
	private String os;
	private String arch;
	private ConfigData configData = null;

	@Override
	protected String getId() {
		return product.getId();
	}

	@Override
	protected Version getVersion() {
		return Version.parseVersion(product.getVersion());
	}

	/**
	 * Constructs a new ProductFileAdvice for a given product file and a
	 * particular configuration. Configurations are 
	 * specified as: ws.os.arch where:
	 *  ws is the windowing system
	 *  os is the operating system
	 *  arch is the architecture
	 */
	public ProductFileAdvice(IProductDescriptor product, String configSpec) {
		this.product = product;
		this.configSpec = configSpec;

		String[] config = AbstractPublisherAction.parseConfigSpec(configSpec);
		ws = config[0];
		if (ws == null)
			ws = AbstractPublisherAction.CONFIG_ANY;
		os = config[1];
		if (os == null)
			os = AbstractPublisherAction.CONFIG_ANY;
		arch = config[2];
		if (arch == null)
			arch = AbstractPublisherAction.CONFIG_ANY;

		configData = getConfigData();
	}

	/**
	 * Returns the program arguments for this product.  
	 */
	public String[] getProgramArguments() {
		String line = product.getProgramArguments(os, arch);
		return AbstractPublisherAction.getArrayFromString(line, " "); //$NON-NLS-1$
	}

	/**
	 * Returns the VM arguments for this product.
	 */
	public String[] getVMArguments() {
		String line = product.getVMArguments(os, arch);
		return AbstractPublisherAction.getArrayFromString(line, " "); //$NON-NLS-1$
	}

	/**
	 * Returns the Bundles that constitute this product.  These
	 * bundles may be specified in the .product file, .product file configuration
	 * area, config.ini file, or a combination of these three places.
	 */
	public BundleInfo[] getBundles() {
		return configData.getBundles();
	}

	/**
	 * Returns the properties associated with this product.  These
	 * properties may be defined in the .product file, the config.ini
	 * file, or both.
	 */
	public Map<String, String> getProperties() {
		Map<String, String> result = new HashMap<String, String>();
		CollectionUtils.putAll(configData.getProperties(), result);
		result.putAll(product.getConfigurationProperties(os, arch));
		return result;
	}

	/**
	 * Returns the name of the launcher.  This should be the OS-independent
	 * name. That is, ".exe" etc. should not be included.
	 * 
	 * @return the name of the branded launcher or <code>null</code> if none.
	 */
	public String getExecutableName() {
		return product.getLauncherName();
	}

	/**
	 * Returns the product file parser that this advice is working on
	 */
	public IProductDescriptor getProductFile() {
		return product;
	}

	/**
	 * Returns the icons defined for this product
	 */
	public String[] getIcons() {
		return product.getIcons(os);
	}

	/**
	 * Returns the operating system that this advice is configured to work with.
	 */
	public String getOS() {
		return this.os;
	}

	/**
	 * Returns the license text for this product
	 */
	public String getLicenseURL() {
		return product.getLicenseURL();
	}

	/**
	 * Returns the license URL for this product
	 */
	public String getLicenseText() {
		return product.getLicenseText();
	}

	/**
	 * Returns the update repositories for this product
	 */
	public List<IRepositoryReference> getUpdateRepositories() {
		return product.getRepositoryEntries();
	}

	private ConfigData getConfigData() {
		DataLoader loader = createDataLoader();
		ConfigData result;
		if (loader != null) {
			result = loader.getConfigData();
		} else
			result = generateConfigData();

		addProductFileBundles(result); // these are the bundles specified in the <plugins/> tag
		addProductFileConfigBundles(result); // these are the bundles specified in the <configurations> tag in the product file
		if (product.getProductId() != null)
			result.setProperty("eclipse.product", product.getProductId()); //$NON-NLS-1$
		if (product.getApplication() != null)
			result.setProperty("eclipse.application", product.getApplication()); //$NON-NLS-1$
		String location = getSplashLocation();
		if (location != null)
			result.setProperty(OSGI_SPLASH_PATH, SPLASH_PREFIX + location);
		return result;
	}

	private void addProductFileConfigBundles(ConfigData data) {
		Set<BundleInfo> versionBoundBundles = new HashSet<BundleInfo>();
		Map<String, List<BundleInfo>> unboundedBundles = new HashMap<String, List<BundleInfo>>();

		BundleInfo[] bundles = data.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			// For each bundle we know about, cache it.  If the bundle doesn't have a version
			// add it to a list of bundles by name
			BundleInfo bundleInfo = bundles[i];
			if (bundleInfo.getVersion().equals(BundleInfo.EMPTY_VERSION)) {
				addUnboundedBundle(unboundedBundles, bundleInfo);
			} else {
				versionBoundBundles.add(bundleInfo);
				addUnboundedBundle(unboundedBundles, bundleInfo);
			}
		}

		List<BundleInfo> bundleInfos = product.getBundleInfos();
		for (BundleInfo bundleInfo : bundleInfos) {
			// For each bundle that has configuration information, if the bundle is in the 
			// bundles bound by version, add the "configured" bundle instead
			// If the bundle is not bound to a version, then replace all bounded versions
			// with this one.  Otherwise, just add this one (don't replace)
			if (versionBoundBundles.contains(bundleInfo)) {
				// If we found a version with the same name and version, replace it with the "configured" bundle
				data.removeBundle(bundleInfo);
				data.addBundle(bundleInfo);
			} else if (bundleInfo.getVersion().equals(BundleInfo.EMPTY_VERSION)) {
				// If we don't have a version number, look for all bundles that match by name
				List<BundleInfo> list = unboundedBundles.get(bundleInfo.getSymbolicName());
				if (list == null)
					data.addBundle(bundleInfo);
				else
					for (BundleInfo target : list) {
						target.setStartLevel(bundleInfo.getStartLevel());
						target.setMarkedAsStarted(bundleInfo.isMarkedAsStarted());
					}
			} else {
				// Otherwise we have a version, but we could not match it, so just add this one.
				data.addBundle(bundleInfo);
			}

		}
	}

	private void addUnboundedBundle(Map<String, List<BundleInfo>> data, BundleInfo bundleInfo) {
		if (!data.containsKey(bundleInfo.getSymbolicName()))
			data.put(bundleInfo.getSymbolicName(), new LinkedList<BundleInfo>());
		data.get(bundleInfo.getSymbolicName()).add(bundleInfo);
	}

	private void addProductFileBundles(ConfigData data) {
		List<IVersionedId> bundles = product.getBundles(true);
		Set<BundleInfo> set = new HashSet<BundleInfo>();
		set.addAll(Arrays.asList(data.getBundles()));

		for (IVersionedId vid : bundles) {
			BundleInfo bundleInfo = new BundleInfo();
			bundleInfo.setSymbolicName(vid.getId());
			bundleInfo.setVersion(vid.getVersion().toString());
			if (!set.contains(bundleInfo))
				data.addBundle(bundleInfo);
		}
	}

	private ConfigData generateConfigData() {
		ConfigData result = new ConfigData(null, null, null, null);
		if (product.useFeatures())
			return result;

		// Add all the bundles here.  We replace / update them later
		// if we find configuration information
		List<IVersionedId> bundles = product.getBundles(true);
		for (IVersionedId vid : bundles) {
			BundleInfo bundleInfo = new BundleInfo();
			bundleInfo.setSymbolicName(vid.getId());
			bundleInfo.setVersion(vid.getVersion().toString());
			result.addBundle(bundleInfo);
		}
		return result;
	}

	private String getSplashLocation() {
		return product.getSplashLocation();
	}

	@Override
	protected String getConfigSpec() {
		return configSpec;
	}

	@Override
	protected boolean matchConfig(String spec, boolean includeDefault) {
		if (spec != null) {
			String targetWS = AbstractPublisherAction.parseConfigSpec(spec)[0];
			if (targetWS == null)
				targetWS = AbstractPublisherAction.CONFIG_ANY;
			if (!ws.equals(targetWS) && !ws.equals(AbstractPublisherAction.CONFIG_ANY) && !targetWS.equals(AbstractPublisherAction.CONFIG_ANY)) {
				return false;
			}

			String targetOS = AbstractPublisherAction.parseConfigSpec(spec)[1];
			if (targetOS == null)
				targetOS = AbstractPublisherAction.CONFIG_ANY;
			if (!os.equals(targetOS) && !os.equals(AbstractPublisherAction.CONFIG_ANY) && !targetOS.equals(AbstractPublisherAction.CONFIG_ANY)) {
				return false;
			}

			String targetArch = AbstractPublisherAction.parseConfigSpec(spec)[2];
			if (targetArch == null)
				targetArch = AbstractPublisherAction.CONFIG_ANY;
			if (!arch.equals(targetArch) && !arch.equals(AbstractPublisherAction.CONFIG_ANY) && !targetArch.equals(AbstractPublisherAction.CONFIG_ANY)) {
				return false;
			}
		}
		return true;
	}

	private DataLoader createDataLoader() {
		String location = product.getConfigIniPath(os);
		if (location == null)
			location = product.getConfigIniPath(null);
		if (location == null)
			return null;

		File configFile = new File(location);
		// We are assuming we are always relative from the product file
		// However PDE tooling puts us relative from the workspace, that "relative" path also looks like an absolute path on linux
		// Build may have copied the file to the correct place for us
		if (!configFile.isAbsolute() || !configFile.exists())
			configFile = new File(product.getLocation().getParentFile(), location);

		//We don't really have an executable location, get something reasonable based on the config.ini location
		File parent = configFile.getParentFile();
		if (parent.getName().equals("configuration") && parent.getParentFile() != null) //$NON-NLS-1$
			parent = parent.getParentFile();
		return new DataLoader(configFile, parent);
	}

}
