/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.installer;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.p2.metadata.IVersionedId;

/**
 * An install information captures all the data needed to perform a product install.
 * This includes information on where the installed product comes from, what will
 * be installed, and where it will be installed.
 */
public class InstallDescription {
	private URI[] artifactRepos;
	private IPath installLocation;
	private IPath agentLocation;
	private IPath bundleLocation;
	private boolean isAutoStart;
	private String launcherName;
	private URI[] metadataRepos;
	private String productName;
	private IVersionedId[] roots;
	private final Map<String, String> profileProperties = new HashMap<String, String>();

	/**
	 * Returns the p2 agent location, or <code>null</code> to indicate
	 * the default agent location.
	 */
	public IPath getAgentLocation() {
		return agentLocation;
	}

	/**
	 * Returns the locations of the artifact repositories to install from
	 * @return a list of artifact repository URLs
	 */
	public URI[] getArtifactRepositories() {
		return artifactRepos;
	}

	/**
	 * Returns the bundle pool location, or <code>null</code> to
	 * indicate the default bundle pool location.
	 */
	public IPath getBundleLocation() {
		return bundleLocation;
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
	 * Returns the locations of the metadata repositories to install from
	 * @return a list of metadata repository URLs
	 */
	public URI[] getMetadataRepositories() {
		return metadataRepos;
	}

	/**
	 * Returns the profile properties for this install.
	 */
	public Map<String, String> getProfileProperties() {
		return profileProperties;
	}

	/**
	 * Returns a human-readable name for this install.
	 * @return the name of the product
	 */
	public String getProductName() {
		return productName;
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

	public void setArtifactRepositories(URI[] value) {
		this.artifactRepos = value;
	}

	public void setAutoStart(boolean value) {
		this.isAutoStart = value;
	}

	public void setBundleLocation(IPath bundleLocation) {
		this.bundleLocation = bundleLocation;
	}

	public void setInstallLocation(IPath location) {
		this.installLocation = location;
	}

	public void setLauncherName(String name) {
		this.launcherName = name;
	}

	public void setMetadataRepositories(URI[] value) {
		this.metadataRepos = value;
	}

	/**
	 * Supplies a set of profile properties to be added when the profile is created.
	 * @param properties the profile properties to be added
	 */
	public void setProfileProperties(Map<String, String> properties) {
		profileProperties.putAll(properties);
	}

	/**
	 * Returns the set of roots to be installed for this installation
	 * @return the roots to install
	 */
	public IVersionedId[] getRoots() {
		return roots;
	}

	/**
	 * Set the list of roots to install
	 * @param value the set of roots to install
	 */
	public void setRoots(IVersionedId[] value) {
		roots = value;
	}

	/**
	 * Set the name of the product being installed.
	 * @param value the new name of the product to install
	 */
	public void setProductName(String value) {
		productName = value;
	}

}
