/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith Inc - rewrite to handle non-OSGi versions.
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.io.Serializable;
import org.eclipse.equinox.internal.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

/**
 * This class represents a version range with Omni Version bounds. It is signature
 * equivalent with the OSGi org.eclipse.osgi.service.resolver.VersionRange
 *
 * @Immutable
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class VersionRange implements Serializable {
	private static final long serialVersionUID = 4988030307298088028L;

	/**
	 * TODO: This should not be OSGi but it has to be that for now since the resolver creates
	 * a filter where the min and max are converted into strings. When the filter is evaluated an
	 * attempt is made to recreate them as OSGi versions.
	 *
	 * An empty OSGi Version range.
	 */
	public static final VersionRange emptyRange = new VersionRange(Version.emptyVersion, true, Version.MAX_VERSION, true);

	private final Version minVersion;
	private final boolean includeMin;
	private final Version maxVersion;
	private final boolean includeMax;

	private static int copyEscaped(String vr, int pos, String breakChars, StringBuffer sb) {
		int top = vr.length();
		pos = VersionParser.skipWhite(vr, pos);
		if (pos >= top)
			throw new IllegalArgumentException();

		char c = vr.charAt(pos);
		for (;;) {
			if (c == '\\' && ++pos < top)
				c = vr.charAt(pos);
			else {
				if (c <= ' ')
					return VersionParser.skipWhite(vr, pos);
				if (breakChars != null && breakChars.indexOf(c) >= 0)
					break;
			}
			sb.append(c);
			if (++pos >= top)
				break;
			c = vr.charAt(pos);
		}
		return pos;
	}

	/**
	 * Constructs a VersionRange with the specified minVersion and maxVersion.
	 * @param minVersion the minimum version of the range
	 * @param maxVersion the maximum version of the range
	 */
	public VersionRange(Version minVersion, boolean includeMin, Version maxVersion, boolean includeMax) {
		if (minVersion == null) {
			if (maxVersion == null) {
				minVersion = Version.emptyVersion;
				maxVersion = Version.MAX_VERSION;
			} else
				minVersion = Version.emptyVersion;
		} else {
			if (maxVersion == null)
				maxVersion = Version.MAX_VERSION;
			else {
				if (minVersion != maxVersion && minVersion.equals(maxVersion))
					maxVersion = minVersion;
				else if (!(minVersion.getFormat() == null ? maxVersion.getFormat() == null : minVersion.getFormat().equals(maxVersion.getFormat()))) {
					// We always allow the MIN and MAX boundaries
					if (!(minVersion.equals(Version.emptyVersion) || maxVersion.equals(Version.MAX_VERSION)))
						throw new IllegalArgumentException(NLS.bind(Messages.range_boundaries_0_and_1_cannot_have_different_formats, minVersion, maxVersion));
				}
			}
		}
		this.minVersion = minVersion;
		this.includeMin = includeMin;
		this.maxVersion = maxVersion;
		this.includeMax = includeMax;
		validateRange();
	}

	/**
	 * Constructs a VersionRange from the given versionRange String.
	 * @param versionRange a version range String that specifies a range of
	 * versions.
	 */
	public VersionRange(String versionRange) {
		int top = 0;
		int pos = 0;
		if (versionRange != null) {
			top = versionRange.length();
			pos = VersionParser.skipWhite(versionRange, 0);
			top = VersionParser.skipTrailingWhite(versionRange, pos, top);
		}

		if (pos >= top) {
			minVersion = Version.emptyVersion;
			includeMin = true;
			maxVersion = Version.MAX_VERSION;
			includeMax = true;
			return;
		}

		char c = versionRange.charAt(pos);
		int[] position = new int[1];
		boolean rawPrefix = false;
		IVersionFormat fmt = null;
		if (VersionParser.isLetter(c)) {
			if (versionRange.startsWith("raw:", pos)) { //$NON-NLS-1$
				rawPrefix = true;
				pos += 4;
			} else {
				position[0] = pos;
				fmt = parseFormat(versionRange, position);
				pos = position[0];
				if (pos >= versionRange.length())
					throw new IllegalArgumentException(NLS.bind(Messages.format_must_be_delimited_by_colon_0, versionRange));

				c = versionRange.charAt(pos);
				if (c != ':')
					throw new IllegalArgumentException(NLS.bind(Messages.format_must_be_delimited_by_colon_0, versionRange));
				++pos;
			}
			pos = VersionParser.skipWhite(versionRange, pos);
			if (pos >= top)
				throw new IllegalArgumentException(NLS.bind(Messages.premature_EOS_0, versionRange));
			c = versionRange.charAt(pos);
		} else
			fmt = VersionFormat.OSGI_FORMAT;

		String minStr;
		String maxStr;
		StringBuffer sb = new StringBuffer();
		if (c == '[' || c == '(') {
			includeMin = (c == '[');
			pos = copyEscaped(versionRange, ++pos, ",)]", sb); //$NON-NLS-1$
			if (pos >= top)
				throw new IllegalArgumentException(NLS.bind(Messages.premature_EOS_0, versionRange));
			c = versionRange.charAt(pos++);
			if (c != ',')
				throw new IllegalArgumentException(NLS.bind(Messages.missing_comma_in_range_0, versionRange));

			minStr = sb.toString();
			sb.setLength(0);
			pos = copyEscaped(versionRange, pos, ")]", sb); //$NON-NLS-1$
			if (pos >= top)
				throw new IllegalArgumentException();
			maxStr = sb.toString();

			c = versionRange.charAt(pos++);
			includeMax = (c == ']');
		} else {
			StringBuffer sbMin = new StringBuffer();
			pos = copyEscaped(versionRange, pos, rawPrefix ? "/" : null, sbMin); //$NON-NLS-1$
			includeMin = includeMax = true;
			minStr = sbMin.toString();
			maxStr = null;
		}

		if (rawPrefix) {
			String origMin = null;
			String origMax = null;
			pos = VersionParser.skipWhite(versionRange, pos);
			if (pos < top && versionRange.charAt(pos) == '/') {
				if (++pos == top)
					throw new IllegalArgumentException(NLS.bind(Messages.original_stated_but_missing_0, versionRange));
				position[0] = pos;
				fmt = parseFormat(versionRange, position);
				pos = VersionParser.skipWhite(versionRange, position[0]);
				if (pos < top) {
					boolean origUseIncDelims = false;
					c = versionRange.charAt(pos);
					if (c != ':')
						throw new IllegalArgumentException(NLS.bind(Messages.original_must_start_with_colon_0, versionRange));

					pos = VersionParser.skipWhite(versionRange, ++pos);
					if (pos == top)
						throw new IllegalArgumentException(NLS.bind(Messages.original_stated_but_missing_0, versionRange));

					c = versionRange.charAt(pos);
					if (c == '[' || c == '(') {
						if (includeMin != (c == '[') || maxStr == null)
							throw new IllegalArgumentException(NLS.bind(Messages.raw_and_original_must_use_same_range_inclusion_0, versionRange));
						pos = VersionParser.skipWhite(versionRange, ++pos);
						origUseIncDelims = true;
					}

					sb.setLength(0);
					if (maxStr == null) {
						copyEscaped(versionRange, pos, ",])", sb); //$NON-NLS-1$
						origMin = sb.toString();
					} else {
						pos = copyEscaped(versionRange, pos, ",])", sb); //$NON-NLS-1$
						if (pos >= top)
							throw new IllegalArgumentException(NLS.bind(Messages.premature_EOS_0, versionRange));
						c = versionRange.charAt(pos++);
						if (c != ',')
							throw new IllegalArgumentException(NLS.bind(Messages.missing_comma_in_range_0, versionRange));
						origMin = sb.toString();

						sb.setLength(0);
						pos = copyEscaped(versionRange, pos, "])", sb); //$NON-NLS-1$
						if (origUseIncDelims) {
							if (pos >= top)
								throw new IllegalArgumentException(NLS.bind(Messages.premature_EOS_0, versionRange));
							c = versionRange.charAt(pos++);
							if (includeMax != (c == ']'))
								throw new IllegalArgumentException(NLS.bind(Messages.raw_and_original_must_use_same_range_inclusion_0, versionRange));
						}
						origMax = sb.toString();
					}
				}
			}
			minVersion = VersionFormat.parseRaw(minStr, fmt, origMin);
			if (maxStr != null) {
				if (maxStr.equals(minStr))
					maxVersion = minVersion;
				else
					maxVersion = VersionFormat.parseRaw(maxStr, fmt, origMax);
			} else
				maxVersion = Version.MAX_VERSION;
		} else {
			if (fmt == null)
				fmt = VersionFormat.OSGI_FORMAT;
			minVersion = fmt.parse(minStr);
			if (maxStr != null) {
				if (maxStr.equals(minStr))
					maxVersion = minVersion;
				else
					maxVersion = fmt.parse(maxStr);
			} else {
				maxVersion = Version.MAX_VERSION;
			}
		}
		validateRange();
	}

	private static IVersionFormat parseFormat(String versionRange, int[] position) {
		int pos = VersionParser.skipWhite(versionRange, position[0]);
		if (!versionRange.startsWith("format(", pos)) //$NON-NLS-1$
			return null;

		pos += 7;
		int end = VersionParser.findEndOfFormat(versionRange, pos, versionRange.length());
		try {
			position[0] = end + 1;
			return VersionFormat.compile(versionRange, pos, end);
		} catch (VersionFormatException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 * Returns the version format.
	 */
	public IVersionFormat getFormat() {
		return minVersion.equals(Version.emptyVersion) ? maxVersion.getFormat() : minVersion.getFormat();
	}

	/**
	 * Returns the minimum Version of this VersionRange
	 * @return the minimum Version of this VersionRange
	 */
	public Version getMinimum() {
		return minVersion;
	}

	/**
	 * Indicates if the minimum version is included in the version range.
	 * @return true if the minimum version is included in the version range;
	 * otherwise false is returned
	 */
	public boolean getIncludeMinimum() {
		return includeMin;
	}

	/**
	 * Returns the maximum Version of this VersionRange
	 * @return the maximum Version of this VersionRange
	 */
	public Version getMaximum() {
		return maxVersion;
	}

	/**
	 * Indicates if the maximum version is included in the version range.
	 * @return true if the maximum version is included in the version range;
	 * otherwise false is returned
	 */
	public boolean getIncludeMaximum() {
		return includeMax;
	}

	public VersionRange intersect(VersionRange r2) {
		int minCompare = minVersion.compareTo(r2.getMinimum());
		int maxCompare = maxVersion.compareTo(r2.getMaximum());

		boolean resultMinIncluded;
		Version resultMin;
		if (minCompare == 0) {
			if (maxCompare == 0 && includeMin == r2.getIncludeMinimum() && includeMax == r2.getIncludeMaximum())
				return this;
			resultMin = minVersion;
			resultMinIncluded = includeMin && r2.getIncludeMinimum();
		} else if (minCompare < 0) {
			resultMin = r2.getMinimum();
			resultMinIncluded = r2.getIncludeMinimum();
		} else { // minCompare > 0)
			resultMin = minVersion;
			resultMinIncluded = includeMin;
		}

		boolean resultMaxIncluded;
		Version resultMax;
		if (maxCompare > 0) {
			resultMax = r2.getMaximum();
			resultMaxIncluded = r2.getIncludeMaximum();
		} else if (maxCompare < 0) {
			resultMax = maxVersion;
			resultMaxIncluded = includeMax;
		} else {//maxCompare == 0
			resultMax = maxVersion;
			resultMaxIncluded = includeMax && r2.getIncludeMaximum();
		}

		int minMaxCmp = resultMin.compareTo(resultMax);
		if (minMaxCmp < 0 || (minMaxCmp == 0 && resultMinIncluded && resultMaxIncluded))
			return new VersionRange(resultMin, resultMinIncluded, resultMax, resultMaxIncluded);

		return null;
	}

	/**
	 * Returns whether the given version is included in this VersionRange.
	 * This will depend on the minimum and maximum versions of this VersionRange
	 * and the given version.
	 * 
	 * @param version a version to be tested for inclusion in this VersionRange. 
	 * (may be <code>null</code>)
	 * @return <code>true</code> if the version is include, 
	 * <code>false</code> otherwise 
	 */
	public boolean isIncluded(Version version) {
		if (version == null)
			return false;

		if (minVersion == maxVersion)
			// Can only happen when both includeMin and includeMax are true
			return minVersion.equals(version);

		int minCheck = includeMin ? 0 : -1;
		int maxCheck = includeMax ? 0 : 1;
		return minVersion.compareTo(version) <= minCheck && maxVersion.compareTo(version) >= maxCheck;
	}

	/**
	 * Checks if the versions of this range is in compliance with the OSGi version spec.
	 * @return A flag indicating whether the range is OSGi compatible or not.
	 */
	public boolean isOSGiCompatible() {
		return minVersion.isOSGiCompatible() && maxVersion.isOSGiCompatible();
	}

	public boolean equals(Object object) {
		if (!(object instanceof VersionRange))
			return false;
		VersionRange vr = (VersionRange) object;
		return includeMin == vr.includeMin && includeMax == vr.includeMax && minVersion.equals(vr.getMinimum()) && maxVersion.equals(vr.getMaximum());
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxVersion.hashCode();
		result = prime * result + minVersion.hashCode();
		result = prime * result + (includeMax ? 1231 : 1237);
		result = prime * result + (includeMin ? 1231 : 1237);
		return result;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		toString(result);
		return result.toString();
	}

	public void toString(StringBuffer result) {
		boolean gtEqual = includeMin && includeMax && Version.MAX_VERSION.equals(maxVersion);
		if (gtEqual && Version.emptyVersion.equals(minVersion)) {
			minVersion.toString(result);
			return;
		}

		IVersionFormat fmt = getFormat();
		if (fmt == VersionFormat.OSGI_FORMAT) {
			if (gtEqual) {
				minVersion.toString(result);
			} else {
				result.append(includeMin ? '[' : '(');
				minVersion.toString(result);
				result.append(',');
				maxVersion.toString(result);
				result.append(includeMax ? ']' : ')');
			}
			return;
		}

		result.append("raw:"); //$NON-NLS-1$
		if (gtEqual) {
			((BasicVersion) minVersion).rawToString(result, true);
		} else {
			result.append(includeMin ? '[' : '(');
			((BasicVersion) minVersion).rawToString(result, true);
			result.append(',');
			((BasicVersion) maxVersion).rawToString(result, true);
			result.append(includeMax ? ']' : ')');
		}
		boolean hasOriginal = (minVersion.getOriginal() != null || maxVersion.getOriginal() != null);
		if (fmt != null || hasOriginal) {
			result.append('/');
			if (fmt != null)
				fmt.toString(result);
			if (hasOriginal) {
				result.append(':');
				if (gtEqual) {
					((BasicVersion) minVersion).originalToString(result, true);
				} else {
					if (Version.emptyVersion.equals(minVersion))
						((BasicVersion) minVersion).rawToString(result, true);
					else
						((BasicVersion) minVersion).originalToString(result, true);
					result.append(',');
					((BasicVersion) maxVersion).originalToString(result, true);
				}
			}
		}
	}

	// Preserve singletons during deserialization
	private Object readResolve() {
		VersionRange vr = this;
		if (equals(emptyRange))
			vr = emptyRange;
		return vr;
	}

	private void validateRange() {
		int cmp = minVersion.compareTo(maxVersion);
		if (!(cmp < 0 || (cmp == 0 && includeMin && includeMax)))
			throw new IllegalArgumentException(NLS.bind(Messages.range_min_0_is_not_less_then_range_max_1, minVersion, maxVersion));
	}

}
