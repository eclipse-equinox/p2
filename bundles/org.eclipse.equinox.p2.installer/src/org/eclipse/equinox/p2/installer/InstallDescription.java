/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.installer;

import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * An install information captures all the data needed to perform a product install.
 * This includes information on where the installed product comes from, what will
 * be installed, and where it will be installed.
 */
public class InstallDescription {
	private URL artifactRepo;
	private IPath installLocation;
	private URL metadataRepo;
	private String productName, flavor;
	private IInstallableUnit rootUnit;
	private boolean isAutoStart;
	private String launcherName;

	public InstallDescription(String name) {
		this.productName = name;
	}

	public void setArtifactRepository(URL repository) {
		this.artifactRepo = repository;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public void setInstallLocation(IPath location) {
		this.installLocation = location;
	}

	public void setMetadataRepository(URL repository) {
		this.metadataRepo = repository;
	}

	public void setRootInstallableUnit(IInstallableUnit unit) {
		this.rootUnit = unit;
	}

	public void setAutoStart(boolean value) {
		this.isAutoStart = value;
	}

	public void setLauncherName(String name) {
		this.launcherName = name;
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
	 * Returns the location of the artifact repository to install from
	 * @return an artifact repository URL
	 */
	public URL getArtifactRepository() {
		return artifactRepo;
	}

	/**
	 * Returns a human-readable name for this install.
	 * @return the name of the product
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * Returns the installable unit whose dependencies describe
	 * all IUs that should be installed.
	 * @return the root installable unit
	 */
	public IInstallableUnit getRootInstallableUnit() {
		return rootUnit;
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

}
