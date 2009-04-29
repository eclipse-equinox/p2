/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 - initial API and implementation
 *     EclipseSource - ongoing development
 *     Thomas Hallgreen - Fix for bug 268659
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.core;

import org.eclipse.equinox.internal.p2.core.helpers.StringHelper;

public class VersionedName {
	private final String id;
	private final Version version;

	/**
	 * Creates and returns a new versioned id from the given spec.  The spec should be
	 * id/version.
	 * @param spec the spec for the versioned id to create
	 * @return the parsed versioned id
	 * @throws IllegalArgumentException If <code>spec</code> is improperly
	 *         formatted.
	 */
	public static VersionedName parse(String spec) {
		String[] segments = StringHelper.getArrayFromString(spec, '/');
		return new VersionedName(segments[0], segments.length == 1 ? null : segments[1]);
	}

	/**
	 * Creates a new versioned name with the given id and version.
	 * @param id The identifier
	 * @param version The version
	 * @throws IllegalArgumentException If <code>version</code> is improperly
	 *         formatted.
	 */
	public VersionedName(String id, String version) {
		this.id = id;
		this.version = Version.parseVersion(version);
	}

	public VersionedName(String id, Version version) {
		this.id = id;
		this.version = (version == null) ? Version.emptyVersion : version;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof VersionedName))
			return false;

		VersionedName vname = (VersionedName) obj;
		return id.equals(vname.id) && version.equals(vname.version);
	}

	public int hashCode() {
		return id.hashCode() * 31 + version.hashCode();
	}

	public String getId() {
		return id;
	}

	public Version getVersion() {
		return version;
	}

	public String toString() {
		return Version.emptyVersion.equals(version) ? id : id + '/' + version.toString();
	}
}
