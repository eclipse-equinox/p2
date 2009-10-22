/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cloudsmith Inc - initial API and implementation.
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.metadata;

import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

/**
 * <p>The Omni Version is composed of a vector of Comparable objects and a pad value. The pad
 * might be <code>null</code>. The vector can contain integers, strings, {@link VersionVector}
 * instances, or one of the special objects {@link VersionVector#MAX_VALUE MAX_VALUE},
 * {@link VersionVector#MAXS_VALUE MAXS_VALUE}, or {@link VersionVector#MIN_VALUE MIN_VALUE}.</p>
 *
 * <p>When two versions are compared, they are always considered padded to infinity by their
 * pad value or by {@link VersionVector#MIN_VALUE MIN_VALUE} in case the pad value is
 * <code>null</code>. The comparison is type sensitive so that:</p><pre>
 * MAX_VALUE &gt; Integer &gt; VersionVector &gt; MAXS_VALUE &gt; String &gt; MIN_VALUE<br/>
 * </pre>
 *
 * The class is signature compatible with {@link org.osgi.framework.Version} but attempts
 * to use it as such might render a {@link UnsupportedOperationException} in case the
 * vector holds incompatible values. The method {@link #isOSGiCompatible()} can be used
 * to test.
 * 
 * @Immutable
 * @noextend This class is not intended to be subclassed by clients.
 */
public class Version extends VersionVector {
	private static final char[] allowedOSGiChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-".toCharArray(); //$NON-NLS-1$

	public static final String RAW_PREFIX = "raw:"; //$NON-NLS-1$

	/**
	 * The empty OSGi version "0.0.0". Equivalent to calling
	 * <code>Version.createOSGi(0,0,0)</code>.
	 */
	public static final Version emptyVersion = Version.createOSGi(0, 0, 0);

	/**
	 * The version that is semantically greater then all other versions.
	 */
	public static final Version MAX_VERSION = Version.create("raw:MpM"); //$NON-NLS-1$

	/**
	 * The version that is semantically less then all other versions.
	 */
	public static final Version MIN_VERSION = Version.create("raw:-M"); //$NON-NLS-1$

	private static final long serialVersionUID = 8202715438560849928L;

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
		// TODO: Eliminate duplicates
		Comparable[] vector = new Comparable[4];
		vector[0] = VersionParser.valueOf(major);
		vector[1] = VersionParser.valueOf(minor);
		vector[2] = VersionParser.valueOf(micro);
		if (qualifier == null || qualifier.length() == 0)
			vector[3] = VersionVector.MINS_VALUE;
		else if (qualifier.equals(VersionFormat.DEFAULT_MAX_STRING_TRANSLATION))
			vector[3] = VersionVector.MAXS_VALUE;
		else
			vector[3] = qualifier;
		Version v = new Version(vector, null, VersionFormat.OSGI_FORMAT, null);
		v.validateOSGI(true);
		return v;
	}

	/**
	 * Create an omni version from an OSGi <code>version</code>.
	 * @param version The OSGi version. Can be <code>null</code>.
	 * @return The created omni version
	 */
	public static Version fromOSGiVersion(org.osgi.framework.Version version) {
		if (version == null)
			return null;
		if (version.equals(org.osgi.framework.Version.emptyVersion))
			return emptyVersion;
		return createOSGi(version.getMajor(), version.getMinor(), version.getMicro(), version.getQualifier());
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
		// TODO: Eliminate duplicates
		return version == null ? null : VersionParser.parse(version, 0, version.length());
	}

	/**
	 * Parses a version identifier from the specified string. This method is for backward
	 * compatibility with OSGi and will return the OSGi {@link #emptyVersion} when
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
		if (version == null || version.length() == 0 || "0.0.0".equals(version)) //$NON-NLS-1$
			return emptyVersion;
		Version v = create(version);
		return v == null ? emptyVersion : v;
	}

	/**
	 * Convert <code>version</code> into its OSGi equivalent if possible.
	 *
	 * @param version The version to convert. Can be <code>null</code>
	 * @return The converted version or <code>null</code> if the argument was <code>null</code>
	 * @throws UnsupportedOperationException if the version could not be converted into an OSGi version
	 */
	public static org.osgi.framework.Version toOSGiVersion(Version version) {
		if (version == null)
			return null;
		if (version.equals(emptyVersion))
			return org.osgi.framework.Version.emptyVersion;
		return new org.osgi.framework.Version(version.getMajor(), version.getMinor(), version.getMicro(), version.getQualifier());
	}

	/**
	 * For exception messages only
	 * @param i the index of the entry
	 * @return the name of the entry
	 */
	private static String getOSGiEntryName(int i) {
		String name = null;
		switch (i) {
			case 0 :
				name = "major"; //$NON-NLS-1$
				break;
			case 1 :
				name = "minor"; //$NON-NLS-1$
				break;
			case 2 :
				name = "micro"; //$NON-NLS-1$
				break;
			case 3 :
				name = "qualifier"; //$NON-NLS-1$
		}
		return name;
	}

	/**
	 * The optional format
	 */
	private final IVersionFormat format;

	/**
	 * The optional original string
	 */
	private final String original;

	public Version(Comparable[] array, Comparable padValue, IVersionFormat format, String original) {
		super(array, padValue);
		this.format = format;
		//don't need to retain original for OSGi version
		this.original = format == VersionFormat.OSGI_FORMAT ? null : original;
	}

	/**
	 * Returns the optional format.
	 */
	public IVersionFormat getFormat() {
		return format;
	}

	/**
	 * Returns the OSGi major component of this version identifier.
	 * 
	 * @return The major component.
	 * @throws UnsupportedOperationException if the first element in the
	 * vector is not a number.
	 * @see #isOSGiCompatible()
	 */
	public int getMajor() {
		return getIntElement(0);
	}

	/**
	 * Returns the OSGi micro component of this version identifier.
	 * 
	 * @return The micro component.
	 * @throws UnsupportedOperationException if the third element in the
	 * vector is not a number.
	 * @see #isOSGiCompatible()
	 */
	public int getMicro() {
		return getIntElement(2);
	}

	/**
	 * Returns the OSGi minor component of this version identifier.
	 * 
	 * @return The minor component.
	 * @throws UnsupportedOperationException if the second element in the
	 * vector is not a number.
	 * @see #isOSGiCompatible()
	 */
	public int getMinor() {
		return getIntElement(1);
	}

	/**
	 * Returns the <code>original</code> part of the string for this version
	 * or <code>null</code> if no such part was provided when the version was
	 * created. An OSGi type version will always return the OSGi string representation.
	 *
	 * @return The <code>original</code> part of the version string or
	 * <code>null</code> if that part was missing.
	 */
	public String getOriginal() {
		return original;
	}

	/**
	 * Returns the OSGi qualifier component of this version identifier.
	 * 
	 * @return The qualifier component or <code>null</code> if not set.
	 * @throws UnsupportedOperationException if the fourth element in the
	 * vector is set to something other then a string.
	 * @see #isOSGiCompatible()
	 */
	public String getQualifier() {
		Comparable[] vector = getVector();
		if (vector.length == 3)
			return MINS_VALUE;

		if (vector.length != 4)
			throw new UnsupportedOperationException();

		Comparable qualifier = vector[3];
		if (qualifier == MAXS_VALUE)
			return VersionFormat.DEFAULT_MAX_STRING_TRANSLATION;
		if (!(qualifier instanceof String))
			throw new UnsupportedOperationException();
		return (String) qualifier;
	}

	/**
	 * Checks if this version is in compliance with the OSGi version spec.
	 * @return A flag indicating whether the version is OSGi compatible or not.
	 */
	public boolean isOSGiCompatible() {
		return format == VersionFormat.OSGI_FORMAT || validateOSGI(false);
	}

	/**
	 * Appends the original for this version onto the <code>sb</code> StringBuffer
	 * if present.
	 * @param sb The buffer that will receive the raw string format
	 * @param rangeSafe Set to <code>true</code> if range delimiters should be escaped
	 */
	public void originalToString(StringBuffer sb, boolean rangeSafe) {
		if (original != null) {
			if (rangeSafe) {
				// Escape all range delimiters while appending
				String s = original;
				int end = s.length();
				for (int idx = 0; idx < end; ++idx) {
					char c = s.charAt(idx);
					if (c == '\\' || c == '[' || c == '(' || c == ']' || c == ')' || c == ',' || c <= ' ')
						sb.append('\\');
					sb.append(c);
				}
			} else
				sb.append(original);
		}
	}

	/**
	 * Appends the raw format for this version onto the <code>sb</code> StringBuffer.
	 * @param sb The buffer that will receive the raw string format
	 * @param rangeSafe Set to <code>true</code> if range delimiters should be escaped
	 */
	public void rawToString(StringBuffer sb, boolean rangeSafe) {
		super.toString(sb, rangeSafe);
	}

	/**
	 * Appends the string representation of this version onto the
	 * <code>sb</code> StringBuffer.
	 * @param sb The buffer that will receive the version string
	 */
	public void toString(StringBuffer sb) {
		if (format == VersionFormat.OSGI_FORMAT) {
			Comparable[] vector = getVector();
			sb.append(vector[0]);
			sb.append('.');
			sb.append(vector[1]);
			sb.append('.');
			sb.append(vector[2]);
			Comparable qualifier = vector[3];
			if (qualifier != MINS_VALUE) {
				sb.append('.');
				sb.append(qualifier == MAXS_VALUE ? VersionFormat.DEFAULT_MAX_STRING_TRANSLATION : qualifier);
			}
			return;
		}
		sb.append(RAW_PREFIX);
		super.toString(sb, false);
		if (format != null || original != null) {
			sb.append('/');
			if (format != null)
				format.toString(sb);
			if (original != null) {
				sb.append(':');
				originalToString(sb, false);
			}
		}
	}

	private int getIntElement(int i) {
		Comparable[] vector = getVector();
		if (!(vector.length > i && vector[i] instanceof Integer))
			throw new UnsupportedOperationException();
		return ((Integer) vector[i]).intValue();
	}

	// Preserve singletons during deserialization
	private Object readResolve() {
		Version v = this;
		if (equals(MAX_VERSION))
			v = MAX_VERSION;
		else if (equals(MIN_VERSION))
			v = MIN_VERSION;
		else if (equals(emptyVersion))
			v = emptyVersion;
		else if (equals(VersionRange.OSGi_versionMax))
			v = VersionRange.OSGi_versionMax;
		else if (equals(VersionRange.OSGi_versionMin))
			v = VersionRange.OSGi_versionMin;
		return v;
	}

	boolean validateOSGI(boolean throwDetailed) {
		Comparable[] vector = getVector();
		if (vector.length < 3 || vector.length > 4) {
			if (throwDetailed)
				throw new IllegalArgumentException(NLS.bind(Messages.illegal_number_of_entries_0_in_osgi_1, VersionParser.valueOf(vector.length), this));
			return false;
		}

		if (getPad() != null) {
			if (throwDetailed)
				throw new IllegalArgumentException(NLS.bind(Messages.pad_not_allowed_in_osgi_0, this));
			return false;
		}

		for (int i = 0; i < 3; ++i) {
			Object e = vector[i];
			if (!(e instanceof Integer && ((Integer) e).intValue() >= 0)) {
				if (throwDetailed)
					throw new IllegalArgumentException(NLS.bind(Messages._0_is_not_a_positive_integer_in_osgi_1, getOSGiEntryName(i), this));
				return false;
			}
		}

		if (vector.length == 3)
			return true; // No qualifier. Still compatible

		Object e = vector[3];
		if (e == VersionVector.MAXS_VALUE)
			return true;

		if (!(e instanceof String)) {
			if (throwDetailed)
				throw new IllegalArgumentException(NLS.bind(Messages._0_is_not_a_string_in_osgi_1, getOSGiEntryName(3), this));
			return false;
		}

		String s = (String) e;
		int idx = s.length();
		char[] allowed = allowedOSGiChars;
		int ctop = allowed.length;
		outer: while (--idx >= 0) {
			char c = s.charAt(idx);
			int cdx = ctop;
			while (--cdx >= 0)
				if (c == allowed[cdx])
					continue outer;
			if (throwDetailed)
				throw new IllegalArgumentException(NLS.bind(Messages._0_is_not_a_valid_qualifier_in_osgi_1, getOSGiEntryName(3), this));
			return false;
		}
		return true;
	}
}
