/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing Development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.p2.metadata.Version;

import org.eclipse.equinox.p2.metadata.ITouchpointType;

/**
 * Identifies a particular touchpoint. A touchpoint is identified by an id 
 * and a version.
 */
public class TouchpointType implements ITouchpointType {
	private String id;//never null
	private Version version;//never null

	public TouchpointType(String id, Version aVersion) {
		this.id = id;
		this.version = aVersion;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (super.equals(obj))
			return true;
		if (obj == null || !(obj instanceof ITouchpointType))
			return false;
		ITouchpointType other = (ITouchpointType) obj;
		return id.equals(other.getId()) && version.equals(other.getVersion());
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