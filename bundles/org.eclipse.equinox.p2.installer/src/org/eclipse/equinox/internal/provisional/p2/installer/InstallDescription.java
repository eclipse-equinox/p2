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
package org.eclipse.equinox.internal.provisional.p2.installer;

import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Version;

/**
 * An install information captures all the data needed to perform a product install.
 * This includes information on where the installed product comes from, what will
 * be installed, and where it will be installed.
 */
public class InstallDescription {
	private URL artifactRepo;
	private IPath installLocation, agentLocation, bundleLocation;
	private boolean isAutoStart;
	private String launcherName;
	private URL metadataRepo;
	private String productName, flavor;
	private String rootId;
	private Version rootVersion;

	public InstallDescription(String name) {
		this.productName = name;
	}

	/**
	 * Returns the p2 agent location, or <code>null</code> to indicate
	 * the default agent location.
	 */
	public IPath getAgentLocation() {
		return agentLocation;
	}

	/**
	 * Returns the location of the artifact repository to install from
	 * @return an artifact repository URL
	 */
	public URL getArtifactRepository() {
		return artifactRepo;
	}

	/**
	 * Returns the bundle pool location, or <code>null</code> to
	 * indicate the default bundle pool location.
	 */
	public IPath getBundleLocation() {
		return bundleLocation;
	}

	/**
	 * Returns the flavor used to configure the product being installed.
	 * @return the install flavor
	 */
	public String getFlavor() {
		return flavor;
	}

	/**
	 * Returns the local file system location to install into.
	 * @return a local file system location
	 */
	public IPath getInstallLocation() {
		return installLocation;
	}

	/**
	 * Returns the name of the product's launcher executable
	 * @return the name of the launcher executable
	 */
	public String getLauncherName() {
		return launcherName;
	}

	/**
	 * Returns the location of the metadata repository to install from
	 * @return a metadata repository URL
	 */
	public URL getMetadataRepository() {
		return metadataRepo;
	}

	/**
	 * Returns a human-readable name for this install.
	 * @return the name of the product
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * Returns the id of the root installable unit
	 * @return the id of the root installable unit
	 */
	public String getRootId() {
		return rootId;
	}

	/**
	 * Returns the version of the root installable unit
	 * @return the version of the root installable unit
	 */
	public Version getRootVersion() {
		return rootVersion;
	}

	/**
	 * Returns whether the installed product should be started upon successful
	 * install.
	 * @return <code>true</code> if the product should be started upon successful
	 * install, and <code>false</code> otherwise
	 */
	public boolean isAutoStart() {
		return isAutoStart;
	}

	public void setAgentLocation(IPath agentLocation) {
		this.agentLocation = agentLocation;
	}

	public void setArtifactRepository(URL repository) {
		this.artifactRepo = repository;
	}

	public void setAutoStart(boolean value) {
		this.isAutoStart = value;
	}

	public void setBundleLocation(IPath bundleLocation) {
		this.bundleLocation = bundleLocation;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public void setInstallLocation(IPath location) {
		this.installLocation = location;
	}

	public void setLauncherName(String name) {
		this.launcherName = name;
	}

	public void setMetadataRepository(URL repository) {
		this.metadataRepo = repository;
	}

	public void setRootId(String root) {
		this.rootId = root;
	}

	public void setRootVersion(Version version) {
		this.rootVersion = version;
	}

}
