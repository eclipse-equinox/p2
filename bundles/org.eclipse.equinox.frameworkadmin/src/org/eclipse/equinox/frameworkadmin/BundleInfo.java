/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.frameworkadmin;

import java.net.URI;
import org.eclipse.core.runtime.URIUtil;
import org.osgi.framework.Version;

/**
 * This object represents information of a bundle. 
 * @since 2.0
 */
public class BundleInfo {
	public static final String EMPTY_VERSION = "0.0.0"; //$NON-NLS-1$
	public static final int NO_LEVEL = -1;
	public static final int NO_BUNDLEID = -1;

	private String symbolicName = null;
	private String version = EMPTY_VERSION;
	private URI baseLocation = null;
	private URI location = null;
	private long bundleId = NO_BUNDLEID;

	private boolean markedAsStarted = false;
	private int startLevel = NO_LEVEL;
	private boolean resolved = false;

	private String manifest = null;
	private String fragmentHost = null;

	/**
	 * Create a new BundleInfo object
	 */
	public BundleInfo() {
	}

	/**
	 * Create a new BundleInfo object
	 * @param location - the location of the bundle
	 */
	public BundleInfo(URI location) {
		this.location = location;
	}

	/**
	 * Create a new BundleInfo object
	 * @param location - the location of the bundle
	 * @param startLevel - the start to be used or {@link BundleInfo#NO_LEVEL}
	 * @param started - whether or not the bundle should be started
	 */
	public BundleInfo(URI location, int startLevel, boolean started) {
		this.location = location;
		this.startLevel = startLevel < 0 ? NO_LEVEL : startLevel;
		this.markedAsStarted = started;
	}

	/**
	 * Create a new BundleInfo object
	 * @param symbolic  The Bundle-SymbolicName name for this bundle
	 * @param version - The version for this bundle, this must be a valid {@link Version} string, if null is passed {@link #EMPTY_VERSION} will be used instead
	 * @param location - the location of the bundle
	 * @param startLevel - the start level of the bundle or {@link BundleInfo#NO_LEVEL}
	 * @param started - whether or not the bundle should be started
	 */
	public BundleInfo(String symbolic, String version, URI location, int startLevel, boolean started) {
		this.symbolicName = symbolic;
		this.version = version != null ? version : EMPTY_VERSION;
		this.location = location;
		this.markedAsStarted = started;
		this.startLevel = startLevel < 0 ? NO_LEVEL : startLevel;
	}

	/**
	 * Get the bundle id
	 * @return the bundle id or {@link #NO_BUNDLEID}
	 */
	public long getBundleId() {
		return bundleId;
	}

	/**
	 * The base location
	 * An absolute URI which may be used to resolve relative {@link #getLocation()} URIs
	 * @return absolute URI or null if not set
	 */
	public URI getBaseLocation() {
		return baseLocation;
	}

	/**
	 * The location of this bundle.
	 * A location is required if this bundle will be persisted into a configuration file
	 * @return URI location or null if not set
	 */
	public URI getLocation() {
		return location;
	}

	/**
	 * The manifest for this bundle
	 * @return the manifest or null if not set
	 */
	public String getManifest() {
		return manifest;
	}

	/**
	 * The start level for this bundle
	 * @return the start level or {@link #NO_LEVEL}
	 */
	public int getStartLevel() {
		return startLevel;
	}

	/**
	 * The Bundle-SymbolicName for this bundle.
	 * A symbolic name is required if this bundle will be persisted into a configuration file
	 * @return the symbolic name or null if not set
	 */
	public String getSymbolicName() {
		return symbolicName;
	}

	/**
	 * Return the version
	 * @return an {@link Version} string, or "0.0.0" if not set 
	 */
	public String getVersion() {
		if (version == null)
			return EMPTY_VERSION;
		return version;
	}

	/**
	 * Return the host if this bundle is a fragment
	 * @return the host, or null if this is not a fragment
	 */
	public String getFragmentHost() {
		return fragmentHost;
	}

	/**
	 * Whether or not this bundle is marked to be started
	 * Default is false
	 * @return boolean
	 */
	public boolean isMarkedAsStarted() {
		return markedAsStarted;
	}

	/**
	 * Whether or not this bundle is resolved
	 * Default is false
	 * @return boolean
	 */
	public boolean isResolved() {
		return resolved;
	}

	/**
	 * Set the bundle id
	 * @param bundleId
	 */
	public void setBundleId(long bundleId) {
		this.bundleId = bundleId;
	}

	/**
	 * Set a base location against which relative {@link #getLocation()} URIs may be resolved
	 * @param baseLocation - an absolute URI
	 */
	public void setBaseLocation(URI baseLocation) {
		this.baseLocation = baseLocation;
	}

	/**
	 * Set the location for this bundle.
	 * @param location
	 */
	public void setLocation(URI location) {
		this.location = location;
	}

	/**
	 * Set the manifest for this bundle
	 * @param manifest
	 */
	public void setManifest(String manifest) {
		this.manifest = manifest;
	}

	/**
	 * Set whether or not this bundle should be started
	 * @param markedAsStarted
	 */
	public void setMarkedAsStarted(boolean markedAsStarted) {
		this.markedAsStarted = markedAsStarted;
	}

	/** 
	 * Set whether or not the bundle is resolved
	 * @param resolved
	 */
	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	/**
	 * Set the start level.
	 * @param level if a value < 0 is passed, the start level will be set to {@link #NO_LEVEL}
	 */
	public void setStartLevel(int level) {
		this.startLevel = level < 0 ? NO_LEVEL : level;
	}

	/**
	 * Set the Bundle-SymbolicName
	 * @param symbolicName
	 */
	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	/**
	 * Set the version, should be a valid {@link Version} string
	 * @param value
	 */
	public void setVersion(String value) {
		if (value == null)
			this.version = EMPTY_VERSION;
		else
			this.version = value;
	}

	/**
	 * Set the host if this bundle is a fragment
	 * @param fragmentHost
	 */
	public void setFragmentHost(String fragmentHost) {
		this.fragmentHost = fragmentHost;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("BundleInfo("); //$NON-NLS-1$
		if (symbolicName != null)
			buffer.append(symbolicName);
		buffer.append(", "); //$NON-NLS-1$
		buffer.append(version);

		if (fragmentHost != null) {
			buffer.append(", fragmentHost="); //$NON-NLS-1$
			buffer.append(fragmentHost);
		}

		if (baseLocation != null) {
			buffer.append(", baseLocation="); //$NON-NLS-1$
			buffer.append(baseLocation);
		}
		buffer.append(", location="); //$NON-NLS-1$
		buffer.append(location);
		buffer.append(", startLevel="); //$NON-NLS-1$
		buffer.append(startLevel);
		buffer.append(", toBeStarted="); //$NON-NLS-1$
		buffer.append(markedAsStarted);
		buffer.append(", resolved="); //$NON-NLS-1$
		buffer.append(resolved);
		buffer.append(", id="); //$NON-NLS-1$
		buffer.append(this.bundleId);//		buffer.append(',').append(manifest == null ? "no manifest" : "manifest available");
		buffer.append(',').append(manifest == null ? "no manifest" : "manifest available"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(')');
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((symbolicName == null) ? 0 : symbolicName.hashCode());
		result = prime * result + version.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		BundleInfo other = (BundleInfo) obj;
		if (symbolicName == null) {
			if (other.symbolicName != null)
				return false;
		} else if (!symbolicName.equals(other.symbolicName))
			return false;

		if (!version.equals(other.getVersion()))
			return false;

		if (location == null || other.location == null)
			return true;

		//compare absolute location URIs
		URI absoluteLocation = null;
		if (location.isAbsolute() || baseLocation == null)
			absoluteLocation = location;
		else
			absoluteLocation = URIUtil.append(baseLocation, URIUtil.toUnencodedString(location));

		URI otherAbsoluteLocation = null;
		if (other.location.isAbsolute() || other.baseLocation == null)
			otherAbsoluteLocation = other.location;
		else
			otherAbsoluteLocation = URIUtil.append(other.baseLocation, URIUtil.toUnencodedString(other.location));
		return URIUtil.sameURI(absoluteLocation, otherAbsoluteLocation);
	}
}
