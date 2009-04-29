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
 *     IBM - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.core;

import org.eclipse.equinox.internal.p2.core.helpers.StringHelper;

/**
 * An object representing a (name,version) pair. 
 * @TODO Should be consistent in calling the first part either "name" or "id", but not both.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class VersionedName {
	private final String id;
	private final Version version;

	/**
	 * Creates and returns a new {@link VersionedName} from the given string specification.  
	 * The specification must be of the form "name/version", or just "name" if the version is absent
	 * <p>
	 * This factory method can be used to reconstruct a {@link VersionedName}
	 * instance from the string representation produced by a previous invocation of 
	 * {@link #toString()}.
	 * 
	 * @param spec the specification for the versioned name to create
	 * @return the parsed versioned named
	 * @throws IllegalArgumentException If <code>spec</code> is improperly
	 *         formatted.
	 */
	public static VersionedName parse(String spec) {
		String[] segments = StringHelper.getArrayFromString(spec, '/');
		return new VersionedName(segments[0], segments.length == 1 ? null : segments[1]);
	}

	/**
	 * Creates a new versioned name with the given id and version.
	 * 
	 * @param id The identifier
	 * @param version The version
	 * @throws IllegalArgumentException If <code>version</code> is improperly
	 *         formatted.
	 */
	public VersionedName(String id, String version) {
		this.id = id;
		this.version = Version.parseVersion(version);
	}

	/**
	 * Creates a new versioned name with the given id and version.
	 * 
	 * @param id The identifier
	 * @param version The version
	 */
	public VersionedName(String id, Version version) {
		this.id = id;
		this.version = (version == null) ? Version.emptyVersion : version;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (!(obj instanceof VersionedName))
			return false;

		VersionedName vname = (VersionedName) obj;
		return id.equals(vname.id) && version.equals(vname.version);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return id.hashCode() * 31 + version.hashCode();
	}

	/**
	 * Returns the name portion of this versioned name.
	 * @TODO Should be getName() for consistency?
	 * 
	 * @return The name portion of this versioned name.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the version portion of this versioned name.
	 * @return the version portion of this versioned name.
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Returns a string representation of this versioned name.
	 * The result can be used to later construct an equal {@link VersionedName}
	 * instance using {{@link #parse(String)}.
	 * @return A string representation of this name
	 */
	public String toString() {
		return Version.emptyVersion.equals(version) ? id : id + '/' + version.toString();
	}
}
