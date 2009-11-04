/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.metadata.IVersionFormat;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.osgi.util.NLS;

/**
 * @Immutable
 * @noextend This class is not intended to be subclassed by clients.
 */
public class OSGiVersion extends BasicVersion {
	private static final long serialVersionUID = -4530178927569560877L;

	private static final boolean[] allowedOSGiChars;

	private final int major;

	private final int minor;

	private final int micro;

	private final Comparable qualifier;

	static {
		allowedOSGiChars = new boolean[128];
		for (int c = '0'; c <= '9'; ++c)
			allowedOSGiChars[c] = true;
		for (int c = 'A'; c <= 'Z'; ++c)
			allowedOSGiChars[c] = true;
		for (int c = 'a'; c <= 'z'; ++c)
			allowedOSGiChars[c] = true;
		allowedOSGiChars['_'] = true;
		allowedOSGiChars['-'] = true;
	}

	public static boolean isValidOSGiQualifier(Comparable e) {
		if (e == VersionVector.MAXS_VALUE)
			return true;

		if (!(e instanceof String))
			return false;

		String s = (String) e;
		int idx = s.length();
		boolean[] allowed = allowedOSGiChars;
		while (--idx >= 0) {
			int c = s.charAt(idx);
			if (c < '-' || c > 'z' || !allowed[c])
				return false;
		}
		return true;
	}

	OSGiVersion(Comparable[] vector) {
		if (vector.length != 4)
			throw new IllegalArgumentException();
		this.major = ((Integer) vector[0]).intValue();
		this.minor = ((Integer) vector[1]).intValue();
		this.micro = ((Integer) vector[2]).intValue();
		this.qualifier = vector[3];
	}

	public OSGiVersion(int major, int minor, int micro, Comparable qualifier) {
		this.major = major;
		this.minor = minor;
		this.micro = micro;
		if (!isValidOSGiQualifier(qualifier))
			throw new IllegalArgumentException(NLS.bind(Messages._0_is_not_a_valid_qualifier_in_osgi_1, "qualifier", this)); //$NON-NLS-1$
		this.qualifier = qualifier;
	}

	public int compareTo(Object v) {
		int result;
		if (!(v instanceof OSGiVersion)) {
			BasicVersion ov = (BasicVersion) v;
			result = VersionVector.compare(getVector(), null, ov.getVector(), ov.getPad());
		} else {
			OSGiVersion ov = (OSGiVersion) v;
			result = major - ov.major;
			if (result == 0) {
				result = minor - ov.minor;
				if (result == 0) {
					result = micro - ov.micro;
					if (result == 0)
						result = qualifier.compareTo(ov.qualifier);
				}
			}
		}
		return result;
	}

	public boolean equals(Object object) {
		if (object == this)
			return true;

		if (!(object instanceof OSGiVersion)) {
			if (object instanceof BasicVersion) {
				BasicVersion ov = (BasicVersion) object;
				return VersionVector.equals(getVector(), null, ov.getVector(), ov.getPad());
			}
			return false;
		}

		OSGiVersion other = (OSGiVersion) object;
		return micro == other.micro && minor == other.minor && major == other.major && qualifier.equals(other.qualifier);
	}

	public IVersionFormat getFormat() {
		return VersionFormat.OSGI_FORMAT;
	}

	public int getMajor() {
		return major;
	}

	public int getMicro() {
		return micro;
	}

	public int getMinor() {
		return minor;
	}

	public String getOriginal() {
		return toString();
	}

	public String getQualifier() {
		return qualifier == VersionVector.MAXS_VALUE ? IVersionFormat.DEFAULT_MAX_STRING_TRANSLATION : (String) qualifier;
	}

	public int hashCode() {
		return (major << 24) + (minor << 16) + (micro << 8) + qualifier.hashCode();
	}

	public boolean isOSGiCompatible() {
		return true;
	}

	public void originalToString(StringBuffer sb, boolean rangeSafe) {
		toString(sb);
	}

	public void rawToString(StringBuffer sb, boolean rangeSafe) {
		sb.append(major);
		sb.append('.');
		sb.append(minor);
		sb.append('.');
		sb.append(micro);
		sb.append('.');
		sb.append('\'');
		sb.append(qualifier);
		sb.append('\'');
	}

	public void toString(StringBuffer sb) {
		sb.append(major);
		sb.append('.');
		sb.append(minor);
		sb.append('.');
		sb.append(micro);
		if (qualifier != VersionVector.MINS_VALUE) {
			sb.append('.');
			sb.append(getQualifier());
		}
	}

	// Preserve singletons during deserialization
	private Object readResolve() {
		Version v = this;
		if (equals(OSGi_MIN))
			v = OSGi_MIN;
		else if (equals(OSGi_MAX))
			v = OSGi_MAX;
		return v;
	}

	public Comparable[] getVector() {
		return new Comparable[] {VersionParser.valueOf(major), VersionParser.valueOf(minor), VersionParser.valueOf(micro), qualifier};
	}

	public Comparable getPad() {
		return null;
	}

	public Comparable getSegment(int index) {
		switch (index) {
			case 0 :
				return VersionParser.valueOf(major);
			case 1 :
				return VersionParser.valueOf(minor);
			case 2 :
				return VersionParser.valueOf(micro);
			case 3 :
				return qualifier;
		}
		throw new ArrayIndexOutOfBoundsException(index); // Not in the imaginary vector array
	}

	public int getSegmentCount() {
		return 4;
	}
}
