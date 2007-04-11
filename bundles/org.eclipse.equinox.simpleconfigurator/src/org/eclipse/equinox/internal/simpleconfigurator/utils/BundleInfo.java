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
package org.eclipse.equinox.internal.simpleconfigurator.utils;

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

	//	private Dictionary manifest;

	public BundleInfo() {
	}

	public BundleInfo(String location) {
		this.location = location.trim();
	}

	public BundleInfo(String location, boolean started) {
		this.location = location.trim();
		this.markedAsStarted = started;
	}

	public BundleInfo(String location, int startLevel) {
		this.location = location.trim();
		this.startLevel = startLevel;
	}

	public BundleInfo(String location, int startLevel, boolean started) {
		this.location = location.trim();
		this.startLevel = startLevel;
		this.markedAsStarted = started;
	}

	public BundleInfo(String location, int startLevel, boolean started, long bundleId) {
		this.location = location.trim();
		this.startLevel = startLevel;
		this.markedAsStarted = started;
		this.bundleId = bundleId;
	}

	public BundleInfo(String symbolic, String version, String location, int startLevel, boolean started) {
		this.symbolicName = symbolic;
		this.version = version;
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
				if (info.symbolicName.equals(symbolicName) && info.version.equals(version) && (info.location == null || location == null ? true : info.location.equals(location)))
					return true;
			} else {
				return (info.location == null || location == null ? false : info.location.equals(location));
			}
		}
		return false;
	}

	public String getLocation() {
		return location;
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
		result = result + (location == null ? 0 : location.hashCode());
		return result;
	}

	public boolean isMarkedAsStarted() {
		return markedAsStarted;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setLocation(String location) {
		this.location = location.trim();
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

	public void setVersion(String vertion) {
		this.version = vertion;
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
		buffer.append(", "); //$NON-NLS-1$
		buffer.append("location="); //$NON-NLS-1$
		buffer.append(location);
		buffer.append(", startLevel="); //$NON-NLS-1$
		buffer.append(startLevel);
		buffer.append(", toBeStarted="); //$NON-NLS-1$
		buffer.append(markedAsStarted);
		buffer.append(", resolved="); //$NON-NLS-1$
		buffer.append(resolved);
		buffer.append(", id="); //$NON-NLS-1$
		buffer.append(this.bundleId);//		buffer.append(',').append(manifest == null ? "no manifest" : "manifest available");
		buffer.append(')');
		return buffer.toString();
	}

	public long getBundleId() {
		return bundleId;
	}

	public void setBundleId(long bundleId) {
		this.bundleId = bundleId;
	}
}
