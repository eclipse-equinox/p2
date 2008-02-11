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
package org.eclipse.equinox.internal.provisional.frameworkadmin;

import org.eclipse.core.runtime.Path;

/**
 * This object represents information of a bundle.
 *
 */
public class BundleInfo {
	public static final int NO_LEVEL = -1;
	public static final int NO_BUNDLEID = -1;

	private String symbolicName = null;
	private String version = null;
	private String location;
	private long bundleId = NO_BUNDLEID;

	private boolean markedAsStarted = false;
	private int startLevel = NO_LEVEL;
	private boolean resolved = false;

	private String manifest;

	public BundleInfo() {
	}

	public BundleInfo(String location) {
		if (location != null)
			this.location = location.trim();
	}

	public BundleInfo(String location, boolean started) {
		if (location != null)
			this.location = location.trim();
		this.markedAsStarted = started;
	}

	public BundleInfo(String location, int startLevel) {
		if (location != null)
			this.location = location.trim();
		this.startLevel = startLevel;
	}

	public BundleInfo(String location, int startLevel, boolean started) {
		if (location != null)
			this.location = location.trim();
		this.startLevel = startLevel;
		this.markedAsStarted = started;
	}

	public BundleInfo(String location, int startLevel, boolean started, long bundleId) {
		if (location != null)
			this.location = location.trim();
		this.startLevel = startLevel;
		this.markedAsStarted = started;
		this.bundleId = bundleId;
	}

	public BundleInfo(String symbolic, String version, String location, int startLevel, boolean started) {
		this.symbolicName = symbolic;
		this.version = version;
		if (location != null)
			this.location = location.trim();
		this.markedAsStarted = started;
		this.startLevel = startLevel;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object toCompare) {
		if (toCompare instanceof BundleInfo) {
			BundleInfo info = (BundleInfo) toCompare;
			//if (info.symbolicName.equals(symbolicName) && info.version.equals(version) && (info.url == null || url == null ? true : info.url.equals(url)))
			if (info.symbolicName != null && info.version != null && symbolicName != null && version != null) {
				// TODO: the equalsIgnoreCase for location comparison is a bug;
				//		 need a platform sensitive location comparison method
				if (info.symbolicName.equals(symbolicName) && info.version.equals(version) && (info.location == null || location == null ? true : new Path(info.location).equals(new Path(location))))
					return true;
			} else {
				return (info.location == null || location == null ? false : info.location.equals(location));
			}
		}
		return false;
	}

	public long getBundleId() {
		return bundleId;
	}

	public String getLocation() {
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int result = symbolicName == null ? 0 : symbolicName.hashCode();
		result = result + (version == null ? 0 : version.hashCode());
		result = result + (location == null ? 0 : new Path(location).hashCode());
		return result;
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

	public void setLocation(String location) {
		this.location = (location != null ? location.trim() : null);

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
		buffer.append(", ");
		if (version != null)
			buffer.append(version);
		buffer.append(", ");
		buffer.append("location=");
		buffer.append(location);
		buffer.append(", startLevel="); //$NON-NLS-1$
		buffer.append(startLevel);
		buffer.append(", toBeStarted=");
		buffer.append(markedAsStarted);
		buffer.append(", resolved=");
		buffer.append(resolved);
		buffer.append(", id=");
		buffer.append(this.bundleId);//		buffer.append(',').append(manifest == null ? "no manifest" : "manifest available");
		buffer.append(',').append(manifest == null ? "no manifest" : "manifest available");
		buffer.append(')');
		return buffer.toString();
	}
}
