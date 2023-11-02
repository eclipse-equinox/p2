/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.*;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionFormatException;
import org.eclipse.osgi.util.NLS;

/**
 * The Omni Version parser. Not intended for public API. Instead use
 * {@link Version#create(String)} or {@link Version#parseVersion(String)}.
 *
 * The class also contains some general purpose parser support methods
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class VersionParser {
	public static final Integer MAX_INT_OBJ = Integer.MAX_VALUE;

	public static Integer valueOf(int i) {
		return (i == Integer.MAX_VALUE) ? MAX_INT_OBJ : Integer.valueOf(i);
	}

	static Comparable<?> removeRedundantTrail(List<Comparable<?>> segments, Comparable<?> padValue) {
		Comparable<?> redundantTrail;
		if (padValue == null)
			redundantTrail = VersionVector.MIN_VALUE;
		else {
			redundantTrail = padValue;
			if (padValue == VersionVector.MIN_VALUE)
				padValue = null;
		}

		int idx = segments.size();
		while (--idx >= 0 && segments.get(idx).equals(redundantTrail))
			segments.remove(idx);

		return padValue;
	}

	private VersionParser() {
		// Prevent class from being instantiated
	}

	/**
	 * Parse the <code>version</code> string and assing the parsed portions to the <code>receiver</code>.
	 * This method is called from the version string constructor.
	 *
	 * @param version The string to be parsed
	 * @param start Start position in the <code>version</code> string
	 * @param maxPos End position in the <code>version</code> string
	 * @return a version if one indeed was parsed or <code>null</code> if the string
	 * contained only whitespace.
	 * @throws IllegalArgumentException if the version is malformed
	 */
	public static Version parse(String version, int start, int maxPos) throws IllegalArgumentException {
		// trim leading and trailing whitespace
		int pos = skipWhite(version, start);
		maxPos = skipTrailingWhite(version, start, maxPos);
		if (pos == maxPos)
			return null;

		List<Comparable<?>> vector = null;
		VersionFormat fmt = null;
		char c = version.charAt(pos);
		if (isDigit(c)) {
			return OSGiVersion.fromVector(VersionFormat.OSGI_FORMAT.parse(version, pos, maxPos));
		}

		if (!isLetter(c))
			throw new IllegalArgumentException();

		if (version.startsWith(Version.RAW_PREFIX, pos)) {
			VersionFormat rawFmt = VersionFormat.RAW_FORMAT;
			pos += 4;

			// Find ending '/' that is neither quoted or escaped
			int end = maxPos;
			for (int idx = pos; idx < maxPos; ++idx) {
				c = version.charAt(idx);
				switch (c) {
					case '/' :
						end = idx;
						break;
					case '\\' :
						++idx;
						continue;
					case '\'' :
					case '"' :
						for (++idx; idx < maxPos; ++idx) {
							char e = version.charAt(idx);
							if (e == c) {
								break;
							}
							if (e == '\\')
								++idx;
						}
						// fall through to default
					default :
						continue;
				}
				break;
			}

			vector = rawFmt.parse(version, pos, end);
			pos = end;
			if (pos == maxPos)
				// This was a pure raw version
				//
				return OmniVersion.fromVector(vector, null, null);

			if (version.charAt(pos) != '/')
				throw new IllegalArgumentException(NLS.bind(Messages.expected_slash_after_raw_vector_0, version.substring(start, maxPos)));
			++pos;

			if (pos == maxPos)
				throw new IllegalArgumentException(NLS.bind(Messages.expected_orignal_after_slash_0, version.substring(start, maxPos)));
		}

		if (version.startsWith("format(", pos)) { //$NON-NLS-1$
			// Parse the format
			//
			pos += 7;
			try {
				// Find matching ')' that is neither quoted or escaped
				//
				int end = findEndOfFormat(version, pos, maxPos);
				fmt = VersionFormat.compile(version, pos, end);
				pos = end + 1;
			} catch (VersionFormatException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			if (pos == maxPos) {
				// This was a raw version with format but no original
				//
				if (vector == null)
					throw new IllegalArgumentException(NLS.bind(Messages.only_format_specified_0, version.substring(start, maxPos)));
				return fmt == VersionFormat.OSGI_FORMAT ? OSGiVersion.fromVector(vector) : OmniVersion.fromVector(vector, fmt, null);
			}
		}

		if (fmt == null && vector == null)
			throw new IllegalArgumentException(NLS.bind(Messages.neither_raw_vector_nor_format_specified_0, version.substring(start, maxPos)));

		if (version.charAt(pos) != ':')
			throw new IllegalArgumentException(NLS.bind(Messages.colon_expected_before_original_version_0, version.substring(start, maxPos)));

		pos++;
		if (pos == maxPos)
			throw new IllegalArgumentException(NLS.bind(Messages.expected_orignal_after_colon_0, version.substring(start, maxPos)));

		if (vector == null) {
			// Vector and pad must be created by parsing the original
			//
			vector = fmt.parse(version, pos, maxPos);
		}
		return fmt == VersionFormat.OSGI_FORMAT ? OSGiVersion.fromVector(vector) : OmniVersion.fromVector(vector, fmt, version.substring(pos));
	}

	static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	public static boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	static boolean isLetterOrDigit(char c) {
		return isDigit(c) || isLetter(c);
	}

	public static int findEndOfFormat(String string, int pos, int maxPos) {
		int end = -1;
		int depth = 1;
		for (int idx = pos; idx < maxPos; ++idx) {
			char c = string.charAt(idx);
			switch (c) {
				case ')' :
					if (--depth == 0) {
						end = idx;
						break;
					}
					continue;
				case '(' :
					++depth;
					continue;
				case '\\' :
					++idx;
					continue;
				case '\'' :
				case '"' :
					for (++idx; idx < maxPos; ++idx) {
						char e = string.charAt(idx);
						if (e == c) {
							break;
						}
						if (e == '\\')
							++idx;
					}
					// fall through to default
				default :
					continue;
			}
			break;
		}
		if (depth != 0)
			throw new IllegalArgumentException(NLS.bind(Messages.unbalanced_format_parenthesis, string.substring(pos - 1, maxPos)));
		return end;
	}

	static Comparable<?> parseRawElement(String value, int[] position, int maxPos) {
		int current = position[0];
		if (current >= maxPos)
			return null;

		boolean negate = false;
		char c = value.charAt(current);
		Comparable<?> v;
		switch (c) {
			case '\'' :
			case '"' : {
				StringBuilder sb = new StringBuilder();
				for (;;) {
					char q = c;
					if (++current == maxPos)
						return null;
					c = value.charAt(current);
					while (c != q) {
						if (c < 32)
							return null;
						sb.append(c);
						if (++current == maxPos)
							return null;
						c = value.charAt(current);
					}
					if (++current == maxPos)
						break;
					c = value.charAt(current);
					if (c != '\'' && c != '"')
						break;
				}
				v = sb.length() == 0 ? VersionVector.MINS_VALUE : sb.toString();
				break;
			}
			case '{' : {
				if (++current == maxPos)
					return null;

				position[0] = current;
				v = parseRawEnum(value, position, maxPos);
				if (v == null)
					return null;
				current = position[0];
				break;
			}
			case '<' : {
				if (++current == maxPos)
					return null;

				position[0] = current;
				v = parseRawVector(value, position, maxPos);
				if (v == null)
					return null;
				current = position[0];
				break;
			}
			case 'm' :
				v = VersionVector.MAXS_VALUE;
				++current;
				break;
			case 'M' :
				v = VersionVector.MAX_VALUE;
				++current;
				break;
			case '-' :
				if (++current >= maxPos)
					return null;

				c = value.charAt(current);
				if (c == 'M') {
					++current;
					v = VersionVector.MIN_VALUE;
					break;
				}
				negate = true;
				// Fall through to default
			default : {
				if (isDigit(c)) {
					int start = current++;
					while (current < maxPos && isDigit(value.charAt(current)))
						++current;
					int val = Integer.parseInt(value.substring(start, current));
					if (negate)
						val = -val;
					v = valueOf(val);
					break;
				}
				return null;
			}
		}
		position[0] = current;
		return v;
	}

	private static Comparable<?> parseRawVector(String value, int[] position, int maxPos) {
		int pos = position[0];
		if (pos >= maxPos)
			return null;

		char c = value.charAt(pos);
		if (c == '>')
			return null;

		ArrayList<Comparable<?>> rawList = new ArrayList<>();
		boolean padMarkerSeen = (c == 'p');
		if (padMarkerSeen) {
			if (++pos >= maxPos)
				return null;
			position[0] = pos;
		}

		Comparable<?> pad = null;
		for (;;) {
			Comparable<?> elem = parseRawElement(value, position, maxPos);
			if (elem == null)
				return null;

			if (padMarkerSeen)
				pad = elem;
			else
				rawList.add(elem);

			pos = position[0];
			if (pos >= maxPos)
				return null;

			c = value.charAt(pos);
			position[0] = ++pos;
			if (c == '>')
				break;

			if (padMarkerSeen || pos >= maxPos)
				return null;

			if (c == 'p') {
				padMarkerSeen = true;
				continue;
			}

			if (c != '.')
				return null;
		}
		pad = removeRedundantTrail(rawList, pad);
		return new VersionVector(rawList.toArray(new Comparable[rawList.size()]), pad);
	}

	private static Comparable<?> parseRawEnum(String value, int[] position, int maxPos) {
		int pos = position[0];
		ArrayList<List<String>> identifiers = new ArrayList<>();
		int ordinal = -1;
		StringBuilder sb = new StringBuilder();
		for (;;) {
			if (pos >= maxPos)
				return null;

			char c = value.charAt(pos++);
			while (c != '}' && c != ',') {
				if (pos == maxPos)
					return null;
				if (c <= ' ')
					return null;
				if (c == '\\' || c == '^') {
					if (c == '^')
						ordinal = identifiers.size();
					c = value.charAt(pos++);
					if (pos == maxPos)
						return null;
				}
				sb.append(c);
				c = value.charAt(pos++);
			}
			identifiers.add(Collections.singletonList(sb.toString()));
			if (c == '}')
				break;
			// c must be ',' at this point
			sb.setLength(0);
		}
		if (ordinal == -1)
			return null;
		position[0] = pos;
		return EnumDefinition.getSegment(identifiers, ordinal);
	}

	public static int skipWhite(String string, int pos) {
		int top = string.length();
		while (pos < top && string.charAt(pos) <= ' ')
			++pos;
		return pos;
	}

	public static int skipTrailingWhite(String string, int start, int end) {
		while (end > start && string.charAt(end - 1) <= ' ')
			--end;
		return end;
	}
}
