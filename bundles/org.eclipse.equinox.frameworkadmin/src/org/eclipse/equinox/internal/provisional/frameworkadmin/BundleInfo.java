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
package org.eclipse.equinox.internal.provisional.frameworkadmin;

import java.net.URI;

/**
 * This object represents information of a bundle.
 *
 */
public class BundleInfo {
	public static final int NO_LEVEL = -1;
	public static final int NO_BUNDLEID = -1;

	private String symbolicName = null;
	private String version = null;
	private URI baseLocation;
	private URI location;
	private long bundleId = NO_BUNDLEID;

	private boolean markedAsStarted = false;
	private int startLevel = NO_LEVEL;
	private boolean resolved = false;

	private String manifest;

	public BundleInfo() {
	}

	public BundleInfo(URI location) {
		this.location = location;
	}

	public BundleInfo(URI location, boolean started) {
		this.location = location;
		this.markedAsStarted = started;
	}

	public BundleInfo(URI location, int startLevel) {
		this.location = location;
		this.startLevel = startLevel;
	}

	public BundleInfo(URI location, int startLevel, boolean started) {
		this.location = location;
		this.startLevel = startLevel;
		this.markedAsStarted = started;
	}

	public BundleInfo(URI location, int startLevel, boolean started, long bundleId) {
		this.location = location;
		this.startLevel = startLevel;
		this.markedAsStarted = started;
		this.bundleId = bundleId;
	}

	public BundleInfo(String symbolic, String version, URI location, int startLevel, boolean started) {
		this.symbolicName = symbolic;
		this.version = version;
		this.location = location;
		this.markedAsStarted = started;
		this.startLevel = startLevel;
	}

	public long getBundleId() {
		return bundleId;
	}

	public URI getBaseLocation() {
		return baseLocation;
	}

	public URI getLocation() {
		return location;
	}

	public String getManifest() {
		return manifest;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public String getVersion() {
		return version;
	}

	public boolean isMarkedAsStarted() {
		return markedAsStarted;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setBundleId(long bundleId) {
		this.bundleId = bundleId;
	}

	public void setBaseLocation(URI baseLocation) {
		this.baseLocation = baseLocation;
	}

	public void setLocation(URI location) {
		this.location = location;
	}

	public void setManifest(String manifest) {
		this.manifest = manifest;
	}

	public void setMarkedAsStarted(boolean markedAsStarted) {
		this.markedAsStarted = markedAsStarted;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	public void setStartLevel(int level) {
		this.startLevel = level;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public void setVersion(String value) {
		this.version = value;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("BundleInfo("); //$NON-NLS-1$
		if (symbolicName != null)
			buffer.append(symbolicName);
		buffer.append(", "); //$NON-NLS-1$
		if (version != null)
			buffer.append(version);

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

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((symbolicName == null) ? 0 : symbolicName.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

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

		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;

		if (location == null || other.location == null)
			return true;

		//compare absolute location URIs
		URI absoluteLocation = baseLocation == null ? location : baseLocation.resolve(location);
		URI otherAbsoluteLocation = other.baseLocation == null ? other.location : other.baseLocation.resolve(other.location);

		return absoluteLocation.equals(otherAbsoluteLocation);
	}
}
