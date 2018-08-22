/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing Development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.Version;

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

	@Override
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

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		return 31 * id.hashCode() + version.hashCode();
	}

	@Override
	public String toString() {
		return "Touchpoint: " + id + ' ' + getVersion(); //$NON-NLS-1$
	}
}