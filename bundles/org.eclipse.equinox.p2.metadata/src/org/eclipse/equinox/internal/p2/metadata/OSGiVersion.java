/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.List;
import org.eclipse.equinox.p2.metadata.IVersionFormat;
import org.eclipse.equinox.p2.metadata.Version;
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

	private final Comparable<?> qualifier;

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

	public static boolean isValidOSGiQualifier(Comparable<?> e) {
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

	static BasicVersion fromVector(List<Comparable<?>> vector) {
		int vtop = vector.size() - 1;
		Comparable<?> pad = vector.get(vtop);
		if (vtop != 4) {
			if (vtop == 0) {
				if (pad == null)
					return (BasicVersion) emptyVersion;
				if (pad == VersionVector.MAX_VALUE)
					return (BasicVersion) MAX_VERSION;
			}
			throw new IllegalArgumentException();
		}
		int major = ((Integer) vector.get(0)).intValue();
		int minor = ((Integer) vector.get(1)).intValue();
		int micro = ((Integer) vector.get(2)).intValue();
		Comparable<?> qualifier = vector.get(3);
		return (major == 0 && minor == 0 && micro == 0 && qualifier == VersionVector.MINS_VALUE) ? (BasicVersion) emptyVersion : new OSGiVersion(major, minor, micro, qualifier);
	}

	public OSGiVersion(int major, int minor, int micro, Comparable<? extends Object> qualifier) {
		this.major = major;
		this.minor = minor;
		this.micro = micro;
		if (!isValidOSGiQualifier(qualifier))
			throw new IllegalArgumentException(NLS.bind(Messages._0_is_not_a_valid_qualifier_in_osgi_1, "qualifier", this)); //$NON-NLS-1$
		//intern the qualifier string to avoid duplication
		if (qualifier instanceof String)
			qualifier = ((String) qualifier).intern();
		this.qualifier = qualifier;
	}

	public int compareTo(Version v) {
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
						result = VersionVector.compareSegments(qualifier, ov.qualifier);
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

	public Comparable<?>[] getVector() {
		return new Comparable[] {VersionParser.valueOf(major), VersionParser.valueOf(minor), VersionParser.valueOf(micro), qualifier};
	}

	public Comparable<?> getPad() {
		return null;
	}

	public Comparable<?> getSegment(int index) {
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

	private Object readResolve() {
		OSGiVersion v = this;
		// Preserve the empty string singleton.
		if (qualifier.equals(VersionVector.MINS_VALUE))
			v = new OSGiVersion(major, minor, micro, VersionVector.MINS_VALUE);
		return v;
	}
}
