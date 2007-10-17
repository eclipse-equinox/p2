/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

/**
 * This object represents information of a bundle.
 *
 */
public class BundleInfo {
	public static final int NO_LEVEL = -1;

	private String symbolicName = null;
	private String version = null;
	private String location;

	private boolean markedAsStarted = false;
	private int startLevel = NO_LEVEL;
	private boolean resolved = false;

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
		buffer.append(')');
		return buffer.toString();
	}
}
