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
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IVersionedId;

/**
 * Represents a product file.  
 * 
 * If getLocation returns null, then config.ini and p2 advice files cannot
 * be used (since these are both relative to the product location).
 *
 */
public interface IProductDescriptor {

	/**
	 * Gets the name of the launcher.
	 */
	public String getLauncherName();

	/**
	 * Returns the list of all bundles in this product.
	 * @param includeFragments whether or not to include the fragments in the return value
	 * @return the list of bundles in this product
	 */
	public List<IVersionedId> getBundles(boolean includeFragments);

	/**
	 * Returns a list<VersionedName> of fragments that constitute this product.
	 */
	public List<IVersionedId> getFragments();

	/**
	 * Returns a List<VersionedName> of features that constitute this product.
	 */
	public List<IVersionedId> getFeatures();

	/**
	 * Returns the path to the config.ini file as specified in the .product file.
	 */
	public String getConfigIniPath(String os);

	/**
	 * Returns the ID for this product.
	 */
	public String getId();

	/**
	 * Returns the Product extension point ID
	 */
	public String getProductId();

	/**
	 * Returns the Applicaiton extension point ID
	 */
	public String getApplication();

	/**
	 * Returns the ID of the bundle in which the splash screen resides.
	 */
	public String getSplashLocation();

	/**
	 * Returns the name of the product
	 */
	public String getProductName();

	/**
	 * Specifies whether this product was built using features or not.
	 */
	public boolean useFeatures();

	/**
	 * Returns the version of the product.
	 */
	public String getVersion();

	/**
	 * Returns the VM arguments for this product for a given OS.
	 */
	public String getVMArguments(String os);

	/**
	 * Returns the program arguments for this product for a given OS.
	 */
	public String getProgramArguments(String os);

	/**
	 * Returns the properties for a product file.
	 */
	public Map<String, String> getConfigurationProperties();

	/**
	 * Returns a list of icons for this product for a given OS.
	 */
	public String[] getIcons(String os);

	/**
	 * Returns a List<BundleInfo> for each bundle that has custom configuration data.
	 * @return A List<BundleInfo>
	 */
	public List<BundleInfo> getBundleInfos();

	/**
	 * This is needed for config.ini files and p2 advice
	 */
	public File getLocation();

	/**
	 * Returns the license URL for this product
	 */
	public String getLicenseURL();

	/**
	 * Returns the license text for this product
	 */
	public String getLicenseText();

}