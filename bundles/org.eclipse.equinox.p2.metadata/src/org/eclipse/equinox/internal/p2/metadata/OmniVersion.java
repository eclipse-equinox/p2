/*******************************************************************************
 * Copyright (c) 2009, 2016 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.Collections;
import java.util.List;
import org.eclipse.equinox.p2.metadata.IVersionFormat;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * <p>The Generic Omni Version is composed of a vector of Comparable objects and a pad value. The pad
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
 * @Immutable
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OmniVersion extends BasicVersion {
	private static final long serialVersionUID = 1996212688810048879L;

	private static OmniVersion minimumVersion;

	private static OmniVersion maximumVersion;

	private static final Comparable<?>[] emptyVector = new Comparable<?>[0];

	private final Comparable<?>[] vector;

	private final Comparable<?> padValue;

	/**
	 * The optional format
	 */
	private final IVersionFormat format;

	/**
	 * The optional original string
	 */
	private final String original;

	static BasicVersion fromVector(List<Comparable<?>> vector, IVersionFormat format, String original) {
		int vtop = vector.size() - 1;
		Comparable<?> padValue = vector.get(vtop);
		if (vtop == 0) {
			if (padValue == null)
				return (BasicVersion) emptyVersion;
			if (padValue == VersionVector.MAX_VALUE)
				return (BasicVersion) MAX_VERSION;
		}
		if (vtop == 3 && padValue == null && vector.get(0) == (Integer) 0 && vector.get(1) == (Integer) 0 && vector.get(2) == (Integer) 0)
			return (BasicVersion) emptyVersion;

		return new OmniVersion(vector, format, original);
	}

	public static Version createMinVersion() {
		if (minimumVersion == null)
			minimumVersion = new OmniVersion(Collections.<Comparable<?>> singletonList(null), null, null);
		return minimumVersion;
	}

	public static Version createMaxVersion() {
		if (maximumVersion == null)
			maximumVersion = new OmniVersion(Collections.<Comparable<?>> singletonList(VersionVector.MAX_VALUE), null, null);
		return maximumVersion;
	}

	private OmniVersion(List<Comparable<?>> vector, IVersionFormat format, String original) {
		int vtop = vector.size() - 1;
		if (vtop > 0) {
			Comparable<?>[] v = new Comparable<?>[vtop];
			for (int idx = 0; idx < vtop; ++idx)
				v[idx] = vector.get(idx);
			this.vector = v;
		} else
			this.vector = emptyVector;
		this.padValue = vector.get(vtop);
		this.format = format;
		this.original = original;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof BasicVersion))
			return false;

		BasicVersion ov = (BasicVersion) o;
		return VersionVector.equals(vector, padValue, ov.getVector(), ov.getPad());
	}

	public IVersionFormat getFormat() {
		return format;
	}

	public int getMajor() {
		return getIntElement(0);
	}

	public int getMicro() {
		return getIntElement(2);
	}

	public int getMinor() {
		return getIntElement(1);
	}

	public String getOriginal() {
		return original;
	}

	public String getQualifier() {
		if (vector.length == 3)
			return VersionVector.MINS_VALUE;

		if (vector.length != 4)
			throw new UnsupportedOperationException();

		Comparable<?> qualifier = vector[3];
		if (qualifier == VersionVector.MAXS_VALUE)
			return IVersionFormat.DEFAULT_MAX_STRING_TRANSLATION;
		if (!(qualifier instanceof String))
			throw new UnsupportedOperationException();
		return (String) qualifier;
	}

	public int hashCode() {
		return VersionVector.hashCode(vector, padValue);
	}

	/**
	 * Checks if this version is in compliance with the OSGi version spec.
	 * @return A flag indicating whether the version is OSGi compatible or not.
	 */
	public boolean isOSGiCompatible() {
		if (vector.length < 3 || vector.length > 4)
			return (this == emptyVersion || this == MAX_VERSION);

		if (getPad() != null)
			return false;

		for (int i = 0; i < 3; ++i) {
			Object e = vector[i];
			if (!(e instanceof Integer && ((Integer) e).intValue() >= 0))
				return false;
		}

		if (vector.length == 3)
			return true; // No qualifier. Still compatible
		return OSGiVersion.isValidOSGiQualifier(vector[3]);
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
		VersionVector.toString(sb, vector, padValue, rangeSafe);
	}

	/**
	 * Appends the string representation of this version onto the
	 * <code>sb</code> StringBuffer.
	 * @param sb The buffer that will receive the version string
	 */
	public void toString(StringBuffer sb) {
		if (this == emptyVersion)
			sb.append("0.0.0"); //$NON-NLS-1$
		else {
			sb.append(RAW_PREFIX);
			VersionVector.toString(sb, vector, padValue, false);
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
	}

	private int getIntElement(int i) {
		if (!(vector.length > i && vector[i] instanceof Integer))
			throw new UnsupportedOperationException();
		return ((Integer) vector[i]).intValue();
	}

	// Preserve singletons during deserialization
	private Object readResolve() {
		Version v = this;
		if (equals(MAX_VERSION))
			v = MAX_VERSION;
		else if (equals(emptyVersion))
			v = emptyVersion;
		return v;
	}

	public Comparable<?> getPad() {
		return padValue;
	}

	public Comparable<?> getSegment(int index) {
		return vector[index];
	}

	public int getSegmentCount() {
		return vector.length;
	}

	Comparable<?>[] getVector() {
		return vector;
	}

	public int compareTo(Version v) {
		BasicVersion ov = (BasicVersion) v;
		return VersionVector.compare(vector, padValue, ov.getVector(), ov.getPad());
	}
}
