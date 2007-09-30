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
package org.eclipse.equinox.p2.metadata;

import org.osgi.framework.Version;

/**
 * Identifies a particular touchpoint. A touchpoint is identified by an id 
 * and a version.
 */
public class TouchpointType {
	/**
	 * A touchpoint type indicating that the "null" touchpoint should be used.
	 * The null touchpoint does not participate in any install phase.
	 */
	public static final TouchpointType NONE = new TouchpointType("null", new Version("0")); //$NON-NLS-1$//$NON-NLS-2$
	private String id;
	private Version versionObject;
	private String version;

	public TouchpointType(String id, Version aVersion) {
		this.id = id;
		this.versionObject = aVersion;
		this.version = aVersion == null ? null : aVersion.toString();
	}

	public String getId() {
		return id;
	}

	public Version getVersion() {
		if (versionObject == null)
			versionObject = version == null ? Version.emptyVersion : new Version(version);
		return versionObject;
	}

	public String toString() {
		return "Touchpoint: " + id + ' ' + getVersion();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (super.equals(obj))
			return true;
		if (getClass() != obj.getClass())
			return false;
		final TouchpointType other = (TouchpointType) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (getVersion() == null) {
			if (other.getVersion() != null)
				return false;
		} else if (!getVersion().equals(other.getVersion()))
			return false;
		return true;
	}

}
