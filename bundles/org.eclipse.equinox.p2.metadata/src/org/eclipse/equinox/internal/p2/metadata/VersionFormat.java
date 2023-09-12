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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.equinox.internal.p2.metadata.VersionFormatParser.Fragment;
import org.eclipse.equinox.p2.metadata.IVersionFormat;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionFormatException;
import org.eclipse.osgi.util.NLS;

/**
 * <p>
 * The VersionFormat represents the Omni Version Format in compiled form. It is
 * also a parser for versions of that format.
 * </p>
 * <p>
 * An instance of VersionFormat is immutable and thus thread safe. The parser
 * does not maintain any state.
 * </p>
 * 
 * @Immutable
 * @noextend This class is not intended to be subclassed by clients.
 */
public class VersionFormat implements IVersionFormat, Serializable {

	/**
	 * The string representation of the Omni Version format used for parsing OSGi
	 * versions.
	 */
	public static final String OSGI_FORMAT_STRING = "n[.n=0;[.n=0;[.S='';=[A-Za-z0-9_-];]]]"; //$NON-NLS-1$

	/**
	 * The string representation of the Omni Version format used for parsing raw
	 * versions.
	 */
	public static final String RAW_FORMAT_STRING = "r(.r)*p?"; //$NON-NLS-1$

	private static final long serialVersionUID = -5689435955091405520L;

	private static class StateInfo {
		Fragment fragment;
		int position;
		int segmentCount;

		StateInfo(int position, int segmentCount, Fragment fragment) {
			this.fragment = fragment;
			this.position = position;
			this.segmentCount = segmentCount;
		}
	}

	static class TreeInfo extends ArrayList<StateInfo> {
		private static final long serialVersionUID = 4474591345244587260L;

		private Comparable<?> padValue;
		private int top;

		TreeInfo(Fragment frag, int pos) {
			add(new StateInfo(pos, 0, frag));
			top = 0;
		}

		Comparable<?> getPadValue() {
			return padValue;
		}

		int getPosition() {
			return get(top).position;
		}

		void popState(List<Comparable<?>> segments, Fragment frag) {
			int idx = top;
			while (idx > 0) {
				StateInfo si = get(idx);
				if (si.fragment == frag) {
					int nsegs = segments.size();
					int segMax = si.segmentCount;
					while (nsegs > segMax)
						segments.remove(--nsegs);
					top = idx - 1;
					break;
				}
			}
		}

		void pushState(int segCount, Fragment fragment) {
			int pos = get(top).position;
			if (++top == size())
				add(new StateInfo(pos, segCount, fragment));
			else {
				StateInfo si = get(top);
				si.fragment = fragment;
				si.position = pos;
				si.segmentCount = segCount;
			}
		}

		void setPadValue(Comparable<?> pad) {
			padValue = pad;
		}

		void setPosition(int pos) {
			get(top).position = pos;
		}
	}

	private static final Map<String, VersionFormat> formatCache = Collections.synchronizedMap(new HashMap<>());

	/**
	 * The predefined OSGi format that is used when parsing OSGi versions.
	 */
	public static final VersionFormat OSGI_FORMAT;

	/**
	 * The predefined OSGi format that is used when parsing raw versions.
	 */
	public static final VersionFormat RAW_FORMAT;

	static {
		try {
			VersionFormatParser parser = new VersionFormatParser();
			OSGI_FORMAT = new VersionFormat(parser.compile(OSGI_FORMAT_STRING, 0, OSGI_FORMAT_STRING.length()));
			formatCache.put(OSGI_FORMAT_STRING, OSGI_FORMAT);
			RAW_FORMAT = new RawFormat(parser.compile(RAW_FORMAT_STRING, 0, RAW_FORMAT_STRING.length()));
			formatCache.put(RAW_FORMAT_STRING, RAW_FORMAT);
		} catch (VersionFormatException e) {
			// If this happens, something is wrong with the actual
			// implementation of the FormatCompiler.
			//
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Compile a version format string into a compiled format. This method is
	 * shorthand for:
	 * 
	 * <pre>
	 * CompiledFormat.compile(format, 0, format.length())
	 * </pre>
	 * 
	 * .
	 *
	 * @param format The format to compile.
	 * @return The compiled format
	 * @throws VersionFormatException If the format could not be compiled
	 */
	public static IVersionFormat compile(String format) throws VersionFormatException {
		return compile(format, 0, format.length());
	}

	/**
	 * Compile a version format string into a compiled format. The parsing starts at
	 * position start and ends at position end. The returned format is cached so
	 * subsequent calls to this method using the same format string will yield the
	 * same compiled format instance.
	 *
	 * @param format The format string to compile.
	 * @param start  Start position in the format string
	 * @param end    End position in the format string
	 * @return The compiled format
	 * @throws VersionFormatException If the format could not be compiled
	 */
	public static VersionFormat compile(String format, int start, int end) throws VersionFormatException {
		String fmtString = format.substring(start, end).intern();
		synchronized (fmtString) {
			VersionFormat fmt = formatCache.get(fmtString);
			if (fmt == null) {
				VersionFormatParser parser = new VersionFormatParser();
				fmt = new VersionFormat(parser.compile(format, start, end));
				formatCache.put(fmtString, fmt);
			}
			return fmt;
		}
	}

	/**
	 * Parse a version string using the {@link #RAW_FORMAT} parser.
	 *
	 * @param version        The version to parse.
	 * @param originalFormat The original format to assign to the created version.
	 *                       Can be <code>null</code>.
	 * @param original       The original version string to assign to the created
	 *                       version. Can be <code>null</code>.
	 * @return A created version
	 * @throws IllegalArgumentException If the version string could not be parsed.
	 */
	public static BasicVersion parseRaw(String version, IVersionFormat originalFormat, String original) {
		List<Comparable<?>> vector = RAW_FORMAT.parse(version, 0, version.length());
		return (originalFormat == OSGI_FORMAT) ? OSGiVersion.fromVector(vector)
				: OmniVersion.fromVector(vector, originalFormat, original);
	}

	static void rawToString(StringBuffer sb, boolean forRange, Comparable<?> e) {
		if (e instanceof String) {
			writeQuotedString(sb, forRange, (String) e, '\'', 0, false);
		} else if (e instanceof VersionVector) {
			sb.append('<');
			((VersionVector) e).toString(sb, forRange);
			sb.append('>');
		} else if (e instanceof EnumDefinition.EnumSegment)
			((EnumDefinition.EnumSegment) e).toString(sb);
		else
			sb.append(e);
	}

	/**
	 * Write a string within quotes. If the string is found to contain the quote, an
	 * attempt is made to flip quote character (single quote becomes double quote
	 * and vice versa). A string that contains both will be written as several
	 * adjacent quoted strings so that each string is quoted with a quote character
	 * that it does not contain.
	 * 
	 * @param sb        The buffer that will receive the string
	 * @param rangeSafe Set to <code>true</code> if the resulting string will be
	 *                  used in a range string and hence need to escape the range
	 *                  delimiter characters
	 * @param s         The string to be written
	 * @param quote     The quote character to start with. Must be the single or
	 *                  double quote character.
	 * @param startPos  The start position
	 * @param didFlip   True if the call is recursive and thus, cannot switch quotes
	 *                  in the first string.
	 */
	private static void writeQuotedString(StringBuffer sb, boolean rangeSafe, String s, char quote, int startPos,
			boolean didFlip) {
		int quotePos = sb.length();
		sb.append(quote);
		boolean otherSeen = false;
		int top = s.length();
		for (int idx = startPos; idx < top; ++idx) {
			char c = s.charAt(idx);
			if (c == '\'' || c == '"') {
				if (c == quote) {
					char otherQuote = quote == '\'' ? '"' : '\'';
					if (didFlip || otherSeen) {
						// We can only flip once
						sb.append(quote);
						writeQuotedString(sb, rangeSafe, s, otherQuote, idx, true);
						return;
					}
					quote = otherQuote;
					sb.setCharAt(quotePos, quote);
					didFlip = true;
				} else
					otherSeen = true;
			}
			if (rangeSafe && (c == '\\' || c == '[' || c == '(' || c == ']' || c == ')' || c == ',' || c <= ' '))
				sb.append('\\');
			sb.append(c);
		}
		sb.append(quote);
	}

	private String fmtString;

	private final Fragment topFragment;

	VersionFormat(Fragment topFragment) {
		this.topFragment = topFragment;
	}

	TreeInfo createInfo(int start) {
		return new TreeInfo(topFragment, start);
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof VersionFormat && toString().equals(o.toString());
	}

	@Override
	public int hashCode() {
		return 11 * toString().hashCode();
	}

	@Override
	public Version parse(String version) {
		List<Comparable<?>> vector = parse(version, 0, version.length());
		return (this == OSGI_FORMAT) ? OSGiVersion.fromVector(vector) : OmniVersion.fromVector(vector, this, version);
	}

	List<Comparable<?>> parse(String version, int start, int maxPos) {
		if (start == maxPos)
			throw new IllegalArgumentException(
					NLS.bind(Messages.format_0_unable_to_parse_empty_version, this, version.substring(start, maxPos)));
		TreeInfo info = new TreeInfo(topFragment, start);
		ArrayList<Comparable<?>> entries = new ArrayList<>(5);
		if (!(topFragment.parse(entries, version, maxPos, info) && info.getPosition() == maxPos))
			throw new IllegalArgumentException(
					NLS.bind(Messages.format_0_unable_to_parse_1, this, version.substring(start, maxPos)));
		entries.add(VersionParser.removeRedundantTrail(entries, info.getPadValue()));
		return entries;
	}

	// Preserve cache during deserialization
	private Object readResolve() {
		synchronized (formatCache) {
			String string = toString();
			string = string.substring(7, string.length() - 1); // Strip of "format(" and ")"
			VersionFormat fmt = formatCache.get(string);
			if (fmt == null) {
				fmt = this;
				formatCache.put(string, fmt);
			}
			return fmt;
		}
	}

	/**
	 * Returns the string representation of this compiled format
	 */
	@Override
	public synchronized String toString() {
		if (fmtString == null) {
			StringBuffer sb = new StringBuffer();
			toString(sb);
		}
		return fmtString;
	}

	@Override
	public synchronized void toString(StringBuffer sb) {
		if (fmtString != null)
			sb.append(fmtString);
		else {
			int start = sb.length();
			sb.append("format"); //$NON-NLS-1$
			if (topFragment.getPadValue() != null) {
				sb.append('(');
				topFragment.toString(sb);
				sb.append(')');
			} else
				topFragment.toString(sb);
			fmtString = sb.substring(start);
		}
	}
}

class RawFormat extends VersionFormat {
	private static final long serialVersionUID = -6070590518921019745L;

	RawFormat(Fragment topFragment) {
		super(topFragment);
	}

	/**
	 * Parse but do not assign this format as the Version format nor the version
	 * string as the original.
	 */
	@Override
	public Version parse(String version) {
		List<Comparable<?>> vector = parse(version, 0, version.length());
		return OmniVersion.fromVector(vector, null, null);
	}

	// Preserve singleton when deserialized
	private Object readResolve() {
		return RAW_FORMAT;
	}
}
