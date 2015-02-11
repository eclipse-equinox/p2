/*******************************************************************************
 * Copyright (c) 2008, 2014 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   EclipseSource - ongoing development
 *   SAP AG - ongoing development
 *   Rapicorp - additional features
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher.eclipse;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.repository.IRepositoryReference;

/**
 * Represents a product file.  
 * 
 * If getLocation returns null, then config.ini and p2 advice files cannot
 * be used (since these are both relative to the product location).
 *
 */
public interface IProductDescriptor {

	/**
	 * Flag for {@link #getFeatures(int)} to obtain the features included in the product.
	 */
	int INCLUDED_FEATURES = 0x1;
	/**
	 * Flag for {@link #getFeatures(int)} to obtain the features to be installed as separately updatable roots. 
	 */
	int ROOT_FEATURES = 0x2;

	/**
	 * Gets the name of the launcher.
	 */
	public String getLauncherName();

	/**
	 * Returns the bundles listed in this product. Note: These bundles are only part of 
	 * the product if {@link #useFeatures()} returns <code>false</code>.
	 * @param includeFragments whether or not to include the fragments in the return value
	 * @return the list of bundles in this product.
	 */
	public List<IVersionedId> getBundles(boolean includeFragments);

	/**
	 * Returns <code>true</code> when <code>getBundles(includeFragments)</code> returns a non-empty list.
	 */
	public boolean hasBundles(boolean includeFragments);

	/**
	 * Returns the fragments listed in the product.
	 * @see #useFeatures()
	 */
	public List<IVersionedId> getFragments();

	/**
	 * Returns the features listed in the product. Same as <code>getFeatures(INCLUDED_FEATURES)</code>. Note: These features are only part of 
	 * the product if {@link #useFeatures()} returns <code>true</code>.
	 */
	public List<IVersionedId> getFeatures();

	/**
	 * Returns <code>true</code> when <code>getFeatures()</code> returns a non-empty list.
	 */
	public boolean hasFeatures();

	/**
	 * Returns the features listed in the product. Note: These features are only part of 
	 * the product if {@link #useFeatures()} returns <code>true</code>.
	 * @param options bitmask to indicate what kind of features to return.
	 * @see #INCLUDED_FEATURES 
	 * @see #ROOT_FEATURES 
	 */
	public List<IVersionedId> getFeatures(int options);

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
	 * Specifies whether this product was built using features only or not.
	 */
	public boolean useFeatures();

	/**
	 * Specifies what kind of installable units (e.g. bundles, features or everything) are included in the product.
	 */
	public ProductContentType getProductContentType();

	/**
	 * Returns the version of the product.
	 */
	public String getVersion();

	/**
	 * Returns the VM arguments for this product for a given OS.
	 */
	public String getVMArguments(String os);

	/**
	 * Returns the VM arguments for this product for a given OS and architecture 
	 * combination.
	 */
	public String getVMArguments(String os, String arch);

	/**
	 * Returns the program arguments for this product for a given OS.
	 */
	public String getProgramArguments(String os);

	/**
	 * Returns the program arguments for this product for a given OS and 
	 * architecture combination.
	 */
	public String getProgramArguments(String os, String arch);

	/**
	 * Returns the properties for a product file.
	 */
	public Map<String, String> getConfigurationProperties();

	/**
	 * Returns the properties for this product file for a given OS and
	 * architecture combination. If the os and/or arch are not specified,
	 * then only those properties defined without os and/or arch filtering
	 * will be returned.
	 */
	public Map<String, String> getConfigurationProperties(String os, String arch);

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
	 * Determines if the launchers should be included in the published product
	 */
	public boolean includeLaunchers();

	/**
	 * Returns the license URL for this product
	 */
	public String getLicenseURL();

	/**
	 * Returns the license text for this product
	 */
	public String getLicenseText();

	/**
	 * Returns a List<IRepositoryReference> for each update repository used by the product.
	 * @return A List<IRepositoryReference>
	 */
	public List<IRepositoryReference> getRepositoryEntries();

}
