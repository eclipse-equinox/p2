/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.net.URI;

/*
 * This object represents information of a bundle.
 */
public class BundleInfo {
	public static final int NO_LEVEL = -1;

	private String symbolicName = null;
	private String version = null;
	private URI location;
	private URI baseLocation;

	private boolean markedAsStarted = false;
	private int startLevel = NO_LEVEL;

	public BundleInfo(String symbolic, String version, URI location, int startLevel, boolean started) {
		this.symbolicName = symbolic;
		this.version = version;
		this.location = location;
		this.markedAsStarted = started;
		this.startLevel = startLevel;
	}

	public URI getLocation() {
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

	public boolean isMarkedAsStarted() {
		return markedAsStarted;
	}

	public URI getBaseLocation() {
		return baseLocation;
	}

	public void setBaseLocation(URI baseLocation) {
		this.baseLocation = baseLocation;
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
		URI absoluteLocation = baseLocation == null ? location : URIUtil.append(baseLocation, location.toString());
		URI otherAbsoluteLocation = other.baseLocation == null ? other.location : URIUtil.append(other.baseLocation, other.location.toString());
		return URIUtil.sameURI(absoluteLocation, otherAbsoluteLocation);
	}
}
