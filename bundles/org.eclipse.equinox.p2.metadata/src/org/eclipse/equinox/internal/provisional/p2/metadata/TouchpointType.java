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
package org.eclipse.equinox.internal.provisional.p2.metadata;

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
	public static final TouchpointType NONE = new TouchpointType("null", Version.emptyVersion); //$NON-NLS-1$

	private String id;//never null
	private Version version;//never null

	TouchpointType(String id, Version aVersion) {
		this.id = id;
		this.version = aVersion;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (super.equals(obj))
			return true;
		if (getClass() != obj.getClass())
			return false;
		TouchpointType other = (TouchpointType) obj;
		return id.equals(other.id) && version.equals(other.version);
	}

	public String getId() {
		return id;
	}

	public Version getVersion() {
		return version;
	}

	public int hashCode() {
		return 31 * id.hashCode() + version.hashCode();
	}

	public String toString() {
		return "Touchpoint: " + id + ' ' + getVersion(); //$NON-NLS-1$
	}
}