/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.publisher.eclipse.DataLoader;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;

/**
 * Provide advice derived from the .product file.  The product can give some info on 
 * launching as well as the configuration (bundles, properties, ...)
 */
public class ProductFileAdvice extends AbstractAdvice implements ILaunchingAdvice, IConfigAdvice {

	private ProductFile product;
	private String configSpec;
	private String os;
	private ConfigData configData = null;

	public ProductFileAdvice(ProductFile product, String configSpec) {
		this.product = product;
		this.configSpec = configSpec;
		os = AbstractPublisherAction.parseConfigSpec(configSpec)[1];
		configData = getConfigData();
	}

	private ConfigData getConfigData() {
		DataLoader loader = createDataLoader();
		ConfigData result;
		if (loader != null)
			result = loader.getConfigData();
		else
			result = generateConfigData();
		result.setProperty("eclipse.product", product.getId()); //$NON-NLS-1$
		String location = getSplashLocation();
		if (location != null)
			result.setProperty("osgi.splashPath", location); //$NON-NLS-1$
		return result;
	}

	private ConfigData generateConfigData() {
		ConfigData result = new ConfigData(null, null, null, null);
		if (product.useFeatures())
			return result;

		// TODO need to do something more interesting here.  What if update.config is around?
		// what if the product is p2 based or simpleconfig is in the list?
		List bundles = product.getBundles(true);
		for (Iterator i = bundles.iterator(); i.hasNext();) {
			String id = (String) i.next();
			BundleInfo bundleInfo = new BundleInfo();
			bundleInfo.setSymbolicName(id);
			result.addBundle(bundleInfo);
		}
		return result;
	}

	private String getSplashLocation() {
		// TODO implement this
		return null;
	}

	protected String getConfigSpec() {
		return configSpec;
	}

	public String[] getProgramArguments() {
		String line = product.getProgramArguments(os);
		return AbstractPublisherAction.getArrayFromString(line, " "); //$NON-NLS-1$
	}

	public String[] getVMArguments() {
		String line = product.getVMArguments(os);
		return AbstractPublisherAction.getArrayFromString(line, " "); //$NON-NLS-1$
	}

	protected boolean matchConfig(String spec, boolean includeDefault) {
		String targetOS = AbstractPublisherAction.parseConfigSpec(spec)[1];
		return os.equals(targetOS);
	}

	public BundleInfo[] getBundles() {
		return configData.getBundles();
	}

	public Properties getProperties() {
		Properties result = new Properties();
		result.putAll(configData.getProperties());
		return result;
	}

	private DataLoader createDataLoader() {
		String location = product.getConfigIniPath(os);
		if (location == null)
			location = product.getConfigIniPath(null);
		if (location == null)
			return null;
		File configFile = new File(location);
		if (!configFile.isAbsolute())
			configFile = new File(product.getLocation().getParentFile(), location);
		// TODO need to figure out what to do for the launcher location here...
		// for now just give any old path that has a parent
		return new DataLoader(configFile, new File(product.getLauncherName()).getAbsoluteFile());
	}

	public String getExecutableName() {
		return product.getLauncherName();
	}
}
