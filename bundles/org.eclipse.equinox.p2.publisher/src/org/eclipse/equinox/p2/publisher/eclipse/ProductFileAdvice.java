/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.publisher.VersionedName;
import org.eclipse.equinox.internal.p2.publisher.eclipse.DataLoader;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;

/**
 * Provide advice derived from the .product file.  The product can give some info on 
 * launching as well as the configuration (bundles, properties, ...)
 */
public class ProductFileAdvice extends AbstractAdvice implements IExecutableAdvice, IConfigAdvice, IBrandingAdvice {

	private final static String OSGI_SPLASH_PATH = "osgi.splashPath"; //$NON-NLS-1$
	private final static String SPLASH_PREFIX = "platform:/base/plugins/"; //$NON-NLS-1$
	private IProductDescriptor product;
	private String configSpec;
	private String os;
	private ConfigData configData = null;

	/**
	 * A way of comparing BundleInfos.  Bundles are first sorted by 
	 * Name and then sorted by version.
	 */
	Comparator bundleInfoComparator = new Comparator() {
		public int compare(Object arg0, Object arg1) {
			BundleInfo b1 = (BundleInfo) arg0;
			BundleInfo b2 = (BundleInfo) arg1;
			boolean useVersion = b1.getVersion() != null && b2.getVersion() != null;
			if (b1.getSymbolicName().compareTo(b2.getSymbolicName()) != 0 || !useVersion)
				return b1.getSymbolicName().compareTo(b2.getSymbolicName());
			return new Version(b1.getVersion()).compareTo(new Version(b2.getVersion()));
		}
	};

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
		os = AbstractPublisherAction.parseConfigSpec(configSpec)[1];
		configData = getConfigData();
	}

	/**
	 * Returns the program arguments for this product.  
	 */
	public String[] getProgramArguments() {
		String line = product.getProgramArguments(os);
		return AbstractPublisherAction.getArrayFromString(line, " "); //$NON-NLS-1$
	}

	/**
	 * Returns the VM arguments for this product.
	 */
	public String[] getVMArguments() {
		String line = product.getVMArguments(os);
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
	public Properties getProperties() {
		Properties result = new Properties();
		result.putAll(configData.getProperties());
		result.putAll(product.getConfigurationProperties());
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

	private ConfigData getConfigData() {
		DataLoader loader = createDataLoader();
		ConfigData result;
		if (loader != null)
			result = loader.getConfigData();
		else
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

	private void addProductFileConfigBundles(ConfigData configData) {
		TreeSet set = new TreeSet(bundleInfoComparator);
		set.addAll(Arrays.asList(configData.getBundles()));
		List bundleInfos = product.getBundleInfos();
		for (Iterator i = bundleInfos.iterator(); i.hasNext();) {
			BundleInfo bundleInfo = (BundleInfo) i.next();
			if (!set.contains(bundleInfo)) {
				configData.addBundle(bundleInfo);
			}
		}
	}

	private void addProductFileBundles(ConfigData configData) {
		List bundles = product.getBundles(true);
		Set set = new TreeSet(bundleInfoComparator);
		set.addAll(Arrays.asList(configData.getBundles()));

		for (Iterator i = bundles.iterator(); i.hasNext();) {
			VersionedName name = (VersionedName) i.next();
			BundleInfo bundleInfo = new BundleInfo();
			bundleInfo.setSymbolicName(name.getId());
			bundleInfo.setVersion(name.getVersion().toString());
			if (!set.contains(bundleInfo))
				configData.addBundle(bundleInfo);
		}
	}

	private ConfigData generateConfigData() {
		ConfigData result = new ConfigData(null, null, null, null);
		if (product.useFeatures())
			return result;

		// TODO need to do something more interesting here.  What if update.config is around?
		// what if the product is p2 based or simpleconfig is in the list?
		List bundles = product.getBundles(true);
		for (Iterator i = bundles.iterator(); i.hasNext();) {
			VersionedName name = (VersionedName) i.next();
			BundleInfo bundleInfo = new BundleInfo();
			bundleInfo.setSymbolicName(name.getId());
			bundleInfo.setVersion(name.getVersion().toString());
			result.addBundle(bundleInfo);
		}
		return result;
	}

	private String getSplashLocation() {
		return product.getSplashLocation();
	}

	protected String getConfigSpec() {
		return configSpec;
	}

	protected boolean matchConfig(String spec, boolean includeDefault) {
		String targetOS = AbstractPublisherAction.parseConfigSpec(spec)[1];
		return os.equals(targetOS);
	}

	private DataLoader createDataLoader() {
		String location = product.getConfigIniPath(os);
		if (location == null)
			location = product.getConfigIniPath(null);
		if (location == null)
			return null;
		File configFile = new File(location);

		// We are assuming we are always relative from the product file
		// However PDE tooling puts us relative from the workspace
		if (!configFile.isAbsolute())
			configFile = new File(product.getLocation().getParentFile(), location);
		// TODO need to figure out what to do for the launcher location here...
		// for now just give any old path that has a parent
		return new DataLoader(configFile, new File(product.getLauncherName()).getAbsoluteFile());
	}

}
