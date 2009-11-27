/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.ConfigData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

public interface IGeneratorInfo {

	/**
	 * Returns whether or not to add the default IUs to the published result.
	 * The default IUs are typically used to configure bundles, features and 
	 * source bundles.
	 * @return whether or not to publish default IUs
	 */
	public boolean addDefaultIUs();

	/**
	 * Returns whether or not to append to existing repositories or overwrite.
	 * @return whether or not to append to existing repositories or overwrite.
	 */
	public boolean append();

	/**
	 * Returns the artifact repository into which any publishable artifacts are published
	 * or <code>null</code> if none.
	 * @return a destination artifact repository or <code>null</code>
	 */
	public IArtifactRepository getArtifactRepository();

	/**
	 * Returns a list of locations in which bundles may be found.  The locations may
	 * be directories to search or actual bundle files.
	 * @return the list of locations holding bundles to process.
	 */
	public File[] getBundleLocations();

	/** 
	 * Return the configuration data to use during publishing or <code>null</code> 
	 * if none.  The configuration data details the framework and launcher setup.
	 *
	 * @return the configuration data or <code>null</code>
	 */
	public ConfigData getConfigData();

	public ArrayList getDefaultIUs(Set ius);

	public File getExecutableLocation();

	public File getFeaturesLocation();

	public String getFlavor();

	public File getJRELocation();

	/**
	 * The platform for the data this location
	 * @return Returns a pde.build style platform config in the form os_ws_arch
	 */
	public String getLauncherConfig();

	public LauncherData getLauncherData();

	public IMetadataRepository getMetadataRepository();

	public String getRootId();

	public String getRootVersion();

	public String getProductFile();

	public String getVersionAdvice();

	/**
	 * Returns the location of the site.xml file, or <code>null</code> if not
	 * generating for an update site.
	 * @return The location of site.xml, or <code>null</code>
	 */
	public URI getSiteLocation();

	public boolean publishArtifactRepository();

	public boolean publishArtifacts();

	public boolean reuseExistingPack200Files();

	public void reuseExistingPack200Files(boolean publishPack);

	public void setArtifactRepository(IArtifactRepository value);

	public void setFlavor(String value);

	public void setMetadataRepository(IMetadataRepository value);

	public void setPublishArtifacts(boolean value);

	public void setRootId(String value);

	public void setVersionAdvice(String advice);

	// TODO: This is kind of ugly. It's purpose is to allow us to craft CUs that we know about and need for our build
	// We should try to replace this with something more generic prior to release
	public Collection getOtherIUs();
}
