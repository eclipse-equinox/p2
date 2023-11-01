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
 *   EclipseSource - ongoing development
 *   IBM Corporation - ongoing development
 *   Rapicorp - additional features
 *   Christoph Läubrich - Bug 574952 p2 should distinguish between "product plugins" and "configuration plugins" (gently sponsored by Compart AG)
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 * Provide advice derived from the .product file. The product can give some info
 * on launching as well as the configuration (bundles, properties, ...)
 */
public class ProductFileAdvice extends AbstractAdvice
		implements ILicenseAdvice, IExecutableAdvice, IConfigAdvice, IBrandingAdvice {
	private final static String OSGI_SPLASH_PATH = "osgi.splashPath"; //$NON-NLS-1$
	private final static String SPLASH_PREFIX = "platform:/base/plugins/"; //$NON-NLS-1$
	private final IProductDescriptor product;
	private final String configSpec;
	private String ws;
	private String os;
	private String arch;
	private final ProductConfigData configData;

	@Override
	protected String getId() {
		return product.getId();
	}

	@Override
	protected Version getVersion() {
		return Version.parseVersion(product.getVersion());
	}

	/**
	 * Constructs a new ProductFileAdvice for a given product file and a particular
	 * configuration. Configurations are specified as: ws.os.arch where: ws is the
	 * windowing system os is the operating system arch is the architecture
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
	@Override
	public String[] getProgramArguments() {
		String line = product.getProgramArguments(os, arch);
		return AbstractPublisherAction.getArrayFromString(line, " "); //$NON-NLS-1$
	}

	/**
	 * Returns the VM arguments for this product.
	 */
	@Override
	public String[] getVMArguments() {
		String line = product.getVMArguments(os, arch);
		return AbstractPublisherAction.getArrayFromString(line, " "); //$NON-NLS-1$
	}

	@Override
	public BundleInfo[] getBundles() {
		return Stream.concat(getBundles(false), getBundles(true))
				.toArray(BundleInfo[]::new);
	}

	Stream<BundleInfo> getBundles(boolean configOnly) {
		if (configOnly) {
			return configData.configBundles.stream();
		}
		return Arrays.stream(configData.data.getBundles());
	}

	/**
	 * Returns the properties associated with this product. These properties may be
	 * defined in the .product file, the config.ini file, or both.
	 */
	@Override
	public Map<String, String> getProperties() {
		Map<String, String> result = new HashMap<>();
		CollectionUtils.putAll(configData.data.getProperties(), result);
		result.putAll(product.getConfigurationProperties(os, arch));
		return result;
	}

	/**
	 * Returns the name of the launcher. This should be the OS-independent name.
	 * That is, ".exe" etc. should not be included.
	 * 
	 * @return the name of the branded launcher or <code>null</code> if none.
	 */
	@Override
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
	@Override
	public String[] getIcons() {
		return product.getIcons(os);
	}

	/**
	 * Returns the operating system that this advice is configured to work with.
	 */
	@Override
	public String getOS() {
		return this.os;
	}

	/**
	 * Returns the license text for this product
	 */
	@Override
	public String getLicenseURL() {
		return product.getLicenseURL();
	}

	/**
	 * Returns the license URL for this product
	 */
	@Override
	public String getLicenseText() {
		return product.getLicenseText();
	}

	/**
	 * Returns the update repositories for this product
	 */
	public List<IRepositoryReference> getUpdateRepositories() {
		return product.getRepositoryEntries();
	}

	private ProductConfigData getConfigData() {
		DataLoader loader = createDataLoader();
		ConfigData result;
		if (loader != null) {
			result = loader.getConfigData();
		} else {
			result = generateConfigData();
		}
		ProductConfigData data = new ProductConfigData(result);
		addProductFileBundles(data); // these are the bundles specified in the <plugins/> tag
		addProductFileConfigBundles(data); // these are the bundles specified in the <configurations> tag in the
											// product file
		if (product.getProductId() != null)
			result.setProperty("eclipse.product", product.getProductId()); //$NON-NLS-1$
		if (product.getApplication() != null)
			result.setProperty("eclipse.application", product.getApplication()); //$NON-NLS-1$
		String location = getSplashLocation();
		if (location != null)
			result.setProperty(OSGI_SPLASH_PATH, SPLASH_PREFIX + location);
		return data;
	}

	private void addProductFileConfigBundles(ProductConfigData productConfigData) {
		Map<String, List<BundleInfo>> configBundleMap = Arrays.stream(productConfigData.data.getBundles())
				.collect(Collectors.groupingBy(BundleInfo::getSymbolicName));
		for (BundleInfo configBundleInfo : product.getBundleInfos()) {
			List<BundleInfo> matches = configBundleMap.get(configBundleInfo.getSymbolicName());
			if (matches == null || matches.isEmpty()) {
				// if there is no match in the ConfigData (e.g. plugin or mixed type product)
				// simply add this to the list of config bundles
				productConfigData.configBundles.add(configBundleInfo);
			} else {
				// otherwise just update the existing data
				for (BundleInfo target : matches) {
					target.setStartLevel(configBundleInfo.getStartLevel());
					target.setMarkedAsStarted(configBundleInfo.isMarkedAsStarted());
				}
			}
		}
	}

	private void addProductFileBundles(ProductConfigData productConfigData) {
		if (product.useFeatures()) {
			return;
		}
		List<IVersionedId> bundles = product.getBundles();
		Set<BundleInfo> set = new HashSet<>();
		set.addAll(Arrays.asList(productConfigData.data.getBundles()));

		for (IVersionedId vid : bundles) {
			BundleInfo bundleInfo = new BundleInfo();
			bundleInfo.setSymbolicName(vid.getId());
			bundleInfo.setVersion(vid.getVersion().toString());
			if (!set.contains(bundleInfo))
				productConfigData.data.addBundle(bundleInfo);
		}
	}

	private ConfigData generateConfigData() {
		ConfigData result = new ConfigData(null, null, null, null);
		if (product.useFeatures())
			return result;

		// Add all the bundles here. We replace / update them later
		// if we find configuration information
		List<IVersionedId> bundles = product.getBundles();
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
			if (!ws.equals(targetWS) && !ws.equals(AbstractPublisherAction.CONFIG_ANY)
					&& !targetWS.equals(AbstractPublisherAction.CONFIG_ANY)) {
				return false;
			}

			String targetOS = AbstractPublisherAction.parseConfigSpec(spec)[1];
			if (targetOS == null)
				targetOS = AbstractPublisherAction.CONFIG_ANY;
			if (!os.equals(targetOS) && !os.equals(AbstractPublisherAction.CONFIG_ANY)
					&& !targetOS.equals(AbstractPublisherAction.CONFIG_ANY)) {
				return false;
			}

			String targetArch = AbstractPublisherAction.parseConfigSpec(spec)[2];
			if (targetArch == null)
				targetArch = AbstractPublisherAction.CONFIG_ANY;
			if (!arch.equals(targetArch) && !arch.equals(AbstractPublisherAction.CONFIG_ANY)
					&& !targetArch.equals(AbstractPublisherAction.CONFIG_ANY)) {
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
		// However PDE tooling puts us relative from the workspace, that "relative" path
		// also looks like an absolute path on linux
		// Build may have copied the file to the correct place for us
		if (!configFile.isAbsolute() || !configFile.exists())
			configFile = new File(product.getLocation().getParentFile(), location);

		// We don't really have an executable location, get something reasonable based
		// on the config.ini location
		File parent = configFile.getParentFile();
		if (parent.getName().equals("configuration") && parent.getParentFile() != null) //$NON-NLS-1$
			parent = parent.getParentFile();
		return new DataLoader(configFile, parent);
	}

	private static final class ProductConfigData {

		private final List<BundleInfo> configBundles = new ArrayList<>();
		private final ConfigData data;

		public ProductConfigData(ConfigData data) {
			this.data = data;
		}

	}

}
