/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cloudsmith Inc - initial API and implementation.
 *******************************************************************************/

package org.eclipse.equinox.p2.metadata;

import java.io.Serializable;
import org.eclipse.equinox.internal.p2.metadata.*;

/**
 * A class that represents a Version in the Omni Version format. A Version can be though of as an
 * array of comparable elements and an optional pad value. The pad value is used when comparing
 * two versions with a different number of segments.
 *
 * The Omni Version can convert almost any version into a raw format that it uses for comparisons.
 * This enables a unified order of all such versions and solves problems that arise when the
 * version semantics are different. A good example is the OSGi version versus the version used in Maven.
 * The lack of qualifier in the OSGi version implies that the qualifier is an empty string. So a version
 * without a qualifier is the smallest of all other versions with the same major,minor,micro number.
 * With Maven semantics, it's the opposite. If the qualifier is removed, the resulting version is
 * considered higher then all other versions with the same major, minor, and micro number. The
 * Omni version solves this by using different raw representations of the OSGi and Maven versions.
 *
 * The Omni version addresses a lot of other issues as well, such as reordering of the elements
 * or treating some parts of a version as irrelevant when comparing.
 * 
 * The class is signature compatible with {@link org.osgi.framework.Version} but attempts
 * to use it as such might render a {@link UnsupportedOperationException} in case the
 * raw vector holds incompatible values. The method {@link #isOSGiCompatible()} can be used
 * to test.
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public abstract class Version implements Comparable<Version>, Serializable {
	public static final String RAW_PREFIX = "raw:"; //$NON-NLS-1$

	/**
	 * The version that is semantically greater then all other versions.
	 */
	public static final Version MAX_VERSION = OmniVersion.createMaxVersion();

	/**
	 * The version that is semantically less then all other versions.
	 */
	public static final Version emptyVersion = OmniVersion.createMinVersion();

	private static final long serialVersionUID = 6218979149720923857L;

	/**
	 * Compile a version format string into a compiled format..
	 *
	 * @param format The format to compile.
	 * @return The compiled format
	 * @throws VersionFormatException If the format could not be compiled
	 */
	public static IVersionFormat compile(String format) throws VersionFormatException {
		return VersionFormat.compile(format, 0, format.length());
	}

	/**
	 * Parses a version identifier from the specified string.
	 * 
	 * @param version String representation of the version identifier. Leading
	 *        and trailing whitespace will be ignored.
	 * @return A <code>Version</code> object representing the version identifier
	 *         or <code>null</code> if <code>version</code> is <code>null</code> or
	 *         an empty string.
	 * @throws IllegalArgumentException If <code>version</code> is improperly
	 *         formatted.
	 */
	public static Version create(String version) {
		return version == null ? null : VersionParser.parse(version, 0, version.length());
	}

	/**
	 * Creates an OSGi version identifier from the specified numerical components.
	 * 
	 * <p>
	 * The qualifier is set to the empty string.
	 * 
	 * @param major Major component of the version identifier.
	 * @param minor Minor component of the version identifier.
	 * @param micro Micro component of the version identifier.
	 * @throws IllegalArgumentException If the numerical components are
	 *         negative.
	 */
	public static Version createOSGi(int major, int minor, int micro) {
		return createOSGi(major, minor, micro, null);
	}

	/**
	 * Creates an OSGi version identifier from the specified components.
	 * 
	 * @param major Major component of the version identifier.
	 * @param minor Minor component of the version identifier.
	 * @param micro Micro component of the version identifier.
	 * @param qualifier Qualifier component of the version identifier. If
	 *        <code>null</code> is specified, then the qualifier will be set to
	 *        the empty string.
	 * @throws IllegalArgumentException If the numerical components are negative
	 *         or the qualifier string is invalid.
	 */
	public static Version createOSGi(int major, int minor, int micro, String qualifier) {
		Comparable<?> logicQualifier;
		if (qualifier == null || qualifier.length() == 0) {
			if (major == 0 && minor == 0 && micro == 0)
				return emptyVersion;
			logicQualifier = VersionVector.MINS_VALUE; // So that we can do identity compare
		} else if (qualifier.equals(IVersionFormat.DEFAULT_MAX_STRING_TRANSLATION))
			logicQualifier = VersionVector.MAXS_VALUE;
		else
			logicQualifier = qualifier;
		return new OSGiVersion(major, minor, micro, logicQualifier);
	}

	/**
	 * Parses a version identifier from the specified string. This method is for backward
	 * compatibility with OSGi and will return the OSGi &quot;0.0.0&quot; version when
	 * the provided string is empty or <code>null</code>.
	 * 
	 * @param version String representation of the version identifier. Leading
	 *        and trailing whitespace will be ignored.
	 * @return A <code>Version</code> object representing the version
	 *         identifier. If <code>version</code> is <code>null</code> or
	 *         the empty string then the OSGi <code>emptyVersion</code> will be
	 *         returned.
	 * @throws IllegalArgumentException If <code>version</code> is improperly
	 *         formatted.
	 * @see #create(String)
	 */
	public static Version parseVersion(String version) {
		if (version == null || version.length() == 0)
			return Version.emptyVersion;
		Version v = create(version);
		return v == null ? Version.emptyVersion : v;
	}

	/**
	 * Returns the optional format.
	 */
	public abstract IVersionFormat getFormat();

	/**
	 * Returns the <code>original</code> part of the string for this version
	 * or <code>null</code> if no such part was provided when the version was
	 * created. An OSGi type version will always return the OSGi string representation.
	 *
	 * @return The <code>original</code> part of the version string or
	 * <code>null</code> if that part was missing.
	 */
	public abstract String getOriginal();

	/**
	 * Returns the pad value used when comparing this versions to
	 * versions that has a larger number of segments
	 * @return The pad value or <code>null</code> if not set.
	 */
	public abstract Comparable<?> getPad();

	/**
	 * An element from the raw vector representation of this version.
	 * @param index The zero based index of the desired element
	 * @return An element from the raw vector
	 */
	public abstract Comparable<?> getSegment(int index);

	/**
	 * Returns the number of elements in the raw vector representation of this version.
	 * @return The number of elements in the raw vector.
	 */
	public abstract int getSegmentCount();

	/**
	 * Checks if this version is in compliance with the OSGi version spec.
	 * @return A flag indicating whether the version is OSGi compatible or not.
	 */
	public abstract boolean isOSGiCompatible();

	public String toString() {
		StringBuffer buf = new StringBuffer(20);
		toString(buf);
		return buf.toString();
	}

	/**
	 * Appends the string representation of this version onto the
	 * <code>sb</code> StringBuffer.
	 * @param sb The buffer that will receive the version string
	 */
	public abstract void toString(StringBuffer sb);
}
