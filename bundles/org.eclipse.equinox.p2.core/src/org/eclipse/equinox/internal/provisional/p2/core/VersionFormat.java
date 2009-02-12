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
package org.eclipse.equinox.internal.provisional.p2.core;

import java.io.Serializable;
import java.util.*;
import org.eclipse.osgi.util.NLS;

/**
 * <p>The VersionFormat represents the Omni Version Format in compiled form. It
 * is also a parser for versions of that format.</p>
 * <p>An instance of VersionFormat is immutable and thus thread safe. The parser
 * does not maintain any state.</p>
 * 
 * @Immutable
 * @noextend This class is not intended to be subclassed by clients.
 */
public class VersionFormat implements Serializable {
	private static final long serialVersionUID = 6888925893926932754L;

	/**
	 * Represents one fragment of a format (i.e. auto, number, string, delimiter, etc.)
	 */
	static abstract class Fragment implements Serializable {
		private static final long serialVersionUID = 4109185333058622681L;

		private final Qualifier qualifier;

		Fragment(Qualifier qualifier) {
			this.qualifier = qualifier;
		}

		public final boolean equals(Object f) {
			return f == this || getClass().equals(f.getClass()) && qualifier.equals(((Fragment) f).qualifier);
		}

		public final int hashCode() {
			return 11 * qualifier.hashCode();
		}

		public boolean isGroup() {
			return false;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			toString(sb);
			return sb.toString();
		}

		Comparable getDefaultValue() {
			return null;
		}

		Fragment getFirstLeaf() {
			return this;
		}

		Comparable getPadValue() {
			return null;
		}

		Qualifier getQualifier() {
			return qualifier;
		}

		boolean parse(List segments, String version, int maxPos, TreeInfo info) {
			return qualifier.parse(new Fragment[] {this}, 0, segments, version, maxPos, info);
		}

		abstract boolean parseOne(List segments, String version, int maxPos, TreeInfo info);

		void setDefaults(List segments) {
			// No-op at this level
		}

		void toString(StringBuffer sb) {
			if (!(qualifier == VersionFormatParser.EXACT_ONE_QUALIFIER || (qualifier == VersionFormatParser.ZERO_OR_ONE_QUALIFIER && this.isGroup())))
				qualifier.toString(sb);
		}
	}

	/**
	 * Specifies the min and max occurrences of a fragment
	 */
	static class Qualifier implements Serializable {
		private static final long serialVersionUID = 7494021832824671685L;

		private final int max;
		private final int min;

		Qualifier(int min, int max) {
			this.min = min;
			this.max = max;
		}

		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Qualifier))
				return false;
			Qualifier oq = (Qualifier) o;
			return min == oq.min && max == oq.max;
		}

		public int hashCode() {
			return 31 * min + 67 * max;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();
			toString(sb);
			return sb.toString();
		}

		int getMax() {
			return max;
		}

		int getMin() {
			return min;
		}

		boolean parse(Fragment[] fragments, int fragIdx, List segments, String version, int maxPos, TreeInfo info) {
			Fragment fragment = fragments[fragIdx++];
			int idx = 0;

			// Do the required parsing. I.e. iterate this fragment
			// min number of times.
			//
			for (; idx < min; ++idx)
				if (!fragment.parseOne(segments, version, maxPos, info))
					return false;

			for (; idx < max; ++idx) {
				// We are greedy. Continue parsing until we get an exception
				// and remember the state before each parse is performed.
				//
				info.pushState(segments.size(), fragment);
				if (!fragment.parseOne(segments, version, maxPos, info)) {
					info.popState(segments, fragment);
					break;
				}
			}
			int maxParsed = idx;

			for (;;) {
				// Pad with default values unless the max is unbounded
				//
				if (max != Integer.MAX_VALUE) {
					for (; idx < max; ++idx)
						fragment.setDefaults(segments);
				}

				if (fragIdx == fragments.length)
					// We are the last segment
					//
					return true;

				// Try to parse the next segment. If it fails, pop the state of
				// this segment (or a child thereof) and try again
				//
				if (fragments[fragIdx].getQualifier().parse(fragments, fragIdx, segments, version, maxPos, info))
					return true;

				// Be less greedy, step back one position and try again.
				//
				if (maxParsed <= min)
					// We have no more states to pop. Tell previous that we failed.
					//
					return false;

				info.popState(segments, fragment);
				idx = --maxParsed; // segments now have room for one more default value
			}
		}

		void toString(StringBuffer sb) {
			if (min == 0) {
				if (max == 1)
					sb.append('?');
				else if (max == Integer.MAX_VALUE)
					sb.append('*');
				else {
					sb.append('{');
					sb.append(min);
					sb.append(',');
					sb.append(max);
					sb.append('}');
				}
			} else if (max == Integer.MAX_VALUE) {
				if (min == 1)
					sb.append('+');
				else {
					sb.append('{');
					sb.append(min);
					sb.append(",}"); //$NON-NLS-1$
				}
			} else {
				sb.append('{');
				sb.append(min);
				if (min != max) {
					sb.append(',');
					sb.append(max);
				}
				sb.append('}');
			}
		}

		// Preserve singleton when deserialized
		private Object readResolve() {
			Qualifier q = this;
			if (min == 0) {
				if (max == 1)
					q = VersionFormatParser.ZERO_OR_ONE_QUALIFIER;
				else if (max == Integer.MAX_VALUE)
					q = VersionFormatParser.ZERO_OR_MANY_QUALIFIER;
			} else if (min == 1) {
				if (max == 1)
					q = VersionFormatParser.EXACT_ONE_QUALIFIER;
				else if (max == Integer.MAX_VALUE)
					q = VersionFormatParser.ONE_OR_MANY_QUALIFIER;
			}
			return q;
		}
	}

	private static class AutoFragment extends RangeFragment {
		private static final long serialVersionUID = -1016534328164247755L;

		AutoFragment(VersionFormatParser.Instructions instr, Qualifier qualifier) {
			super(instr, qualifier);
		}

		boolean parseOne(List segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			maxPos = checkRange(pos, maxPos);
			if (maxPos < 0)
				return false;

			char c = version.charAt(pos);
			if (VersionParser.isDigit(c) && isAllowed(c)) {
				// Parse to next non-digit
				//
				int start = pos;
				int value = c - '0';
				while (++pos < maxPos) {
					c = version.charAt(pos);
					if (!(VersionParser.isDigit(c) && isAllowed(c)))
						break;
					value *= 10;
					value += (c - '0');
				}
				int len = pos - start;
				if (rangeMin > len || len > rangeMax)
					return false;

				if (!isIgnored())
					segments.add(Version.valueOf(value));
				info.setPosition(pos);
				return true;
			}

			if (!(VersionParser.isLetter(c) && isAllowed(c)))
				return false;

			// Parse to next non-letter or next delimiter
			//
			int start = pos++;
			for (; pos < maxPos; ++pos) {
				c = version.charAt(pos);
				if (!(VersionParser.isLetter(c) && isAllowed(c)))
					break;
			}
			int len = pos - start;
			if (rangeMin > len || len > rangeMax)
				return false;

			if (!isIgnored())
				segments.add(version.substring(start, pos));
			info.setPosition(pos);
			return true;
		}

		void toString(StringBuffer sb) {
			sb.append('a');
			super.toString(sb);
		}
	}

	private static class DelimiterFragment extends Fragment {
		private static final long serialVersionUID = 8173654376143370605L;
		private final char[] delimChars;
		private final boolean inverted;

		DelimiterFragment(VersionFormatParser.Instructions ep, Qualifier qualifier) {
			super(qualifier);
			if (ep == null) {
				delimChars = null;
				inverted = false;
			} else {
				inverted = ep.inverted;
				delimChars = ep.characters;
			}
		}

		boolean isMatch(String version, int pos) {
			char c = version.charAt(pos);
			if (delimChars != null) {
				for (int idx = 0; idx < delimChars.length; ++idx)
					if (c == delimChars[idx])
						return !inverted;
				return inverted;
			} else if (VersionParser.isLetterOrDigit(c))
				return false;

			return true;
		}

		boolean parseOne(List segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			if (pos < maxPos && isMatch(version, pos)) {
				// Just swallow, a delimiter does not contribute to the vector.
				//
				info.setPosition(pos + 1);
				return true;
			}
			return false;
		}

		void toString(StringBuffer sb) {
			sb.append('d');
			if (delimChars != null)
				appendCharacterRange(sb, delimChars, inverted);
			super.toString(sb);
		}
	}

	static boolean equalsAllowNull(Object a, Object b) {
		return (a == null) ? (b == null) : (b != null && a.equals(b));
	}

	private static abstract class ElementFragment extends Fragment {
		private static final long serialVersionUID = -6834591415456539713L;
		private final Comparable defaultValue;
		private final boolean ignored;
		private final Comparable padValue;

		ElementFragment(VersionFormatParser.Instructions instr, Qualifier qualifier) {
			super(qualifier);
			if (instr != null) {
				ignored = instr.ignore;
				defaultValue = instr.defaultValue;
				padValue = instr.padValue;
			} else {
				ignored = false;
				defaultValue = null;
				padValue = null;
			}
		}

		Comparable getDefaultValue() {
			return defaultValue;
		}

		Comparable getPadValue() {
			return padValue;
		}

		boolean isIgnored() {
			return ignored;
		}

		void setDefaults(List segments) {
			Object defaultVal = getDefaultValue();
			if (defaultVal != null)
				segments.add(defaultVal);
		}

		void toString(StringBuffer sb) {
			if (ignored) {
				sb.append('=');
				sb.append('!');
				sb.append(';');
			}
			if (defaultValue != null) {
				sb.append('=');
				VersionVector.rawToString(sb, false, defaultValue);
				sb.append(';');
			}
			if (padValue != null) {
				sb.append('=');
				sb.append('p');
				VersionVector.rawToString(sb, false, padValue);
				sb.append(';');
			}
			super.toString(sb);
		}
	}

	private static class GroupFragment extends ElementFragment {
		private static final long serialVersionUID = 9219978678087669699L;
		private final boolean array;
		private final Fragment[] fragments;

		GroupFragment(VersionFormatParser.Instructions instr, Qualifier qualifier, Fragment[] fragments, boolean array) {
			super(instr, qualifier);
			this.fragments = fragments;
			this.array = array;
		}

		public boolean isGroup() {
			return !array;
		}

		Fragment getFirstLeaf() {
			return fragments[0].getFirstLeaf();
		}

		Fragment[] getFragments() {
			return fragments;
		}

		boolean isArray() {
			return array;
		}

		boolean parseOne(List segments, String version, int maxPos, TreeInfo info) {
			if (array) {
				ArrayList subSegs = new ArrayList();
				boolean success = fragments[0].getQualifier().parse(fragments, 0, subSegs, version, maxPos, info);
				if (!success || subSegs.isEmpty())
					return false;

				Comparable padValue = info.getPadValue();
				if (padValue != null)
					info.setPadValue(null); // Prevent outer group from getting this.
				else
					padValue = getPadValue();

				VersionParser.removeRedundantTrail(segments, padValue);
				segments.add(new VersionVector((Comparable[]) subSegs.toArray(new Comparable[subSegs.size()]), padValue));
				return true;
			}

			if (fragments[0].getQualifier().parse(fragments, 0, segments, version, maxPos, info)) {
				Comparable padValue = getPadValue();
				if (padValue != null)
					info.setPadValue(padValue);
				return true;
			}
			return false;
		}

		void setDefaults(List segments) {
			Comparable dflt = getDefaultValue();
			if (dflt != null) {
				// A group default overrides any defaults within the
				// group fragments
				super.setDefaults(segments);
			} else {
				// Assign defaults for all fragments
				for (int idx = 0; idx < fragments.length; ++idx)
					fragments[idx].setDefaults(segments);
			}
		}

		void toString(StringBuffer sb) {
			if (array) {
				sb.append('<');
				for (int idx = 0; idx < fragments.length; ++idx)
					fragments[idx].toString(sb);
				sb.append('>');
			} else {
				if (getQualifier() == VersionFormatParser.ZERO_OR_ONE_QUALIFIER) {
					sb.append('[');
					for (int idx = 0; idx < fragments.length; ++idx)
						fragments[idx].toString(sb);
					sb.append(']');
				} else {
					sb.append('(');
					for (int idx = 0; idx < fragments.length; ++idx)
						fragments[idx].toString(sb);
					sb.append(')');
				}
			}
			super.toString(sb);
		}
	}

	private static class LiteralFragment extends Fragment {
		private static final long serialVersionUID = 6210696245839471802L;
		private final String string;

		LiteralFragment(Qualifier qualifier, String string) {
			super(qualifier);
			this.string = string;
		}

		String getString() {
			return string;
		}

		boolean parseOne(List segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			int litLen = string.length();
			if (pos + litLen > maxPos)
				return false;

			for (int idx = 0; idx < litLen; ++idx, ++pos) {
				if (string.charAt(idx) != version.charAt(pos))
					return false;
			}
			info.setPosition(pos);
			return true;
		}

		void toString(StringBuffer sb) {
			String str = string;
			if (str.length() != 1) {
				sb.append('\'');
				toStringEscaped(sb, str, "\'"); //$NON-NLS-1$
				sb.append('\'');
			} else {
				char c = str.charAt(0);
				switch (c) {
					case '\'' :
					case '\\' :
					case '<' :
					case '[' :
					case '(' :
					case '{' :
					case '?' :
					case '*' :
					case '+' :
					case '=' :
						sb.append('\\');
						sb.append(c);
						break;
					default :
						if (VersionParser.isLetterOrDigit(c)) {
							sb.append('\\');
							sb.append(c);
						} else
							sb.append(c);
				}
			}
			super.toString(sb);
		}
	}

	private static class NumberFragment extends RangeFragment {
		private static final long serialVersionUID = -8552754381106711507L;
		private final boolean signed;

		NumberFragment(VersionFormatParser.Instructions instr, Qualifier qualifier, boolean signed) {
			super(instr, qualifier);
			this.signed = signed;
		}

		boolean parseOne(List segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			maxPos = checkRange(pos, maxPos);
			if (maxPos < 0)
				return false;

			// Parse to next non-digit
			//
			int start = pos;
			int value;

			char c = version.charAt(pos);
			if (signed || characters != null) {
				boolean negate = false;
				if (signed && c == '-' && pos + 1 < maxPos) {
					negate = true;
					c = version.charAt(++pos);
				}

				if (!(c >= '0' && c <= '9' && isAllowed(c)))
					return false;

				// Parse to next non-digit
				//
				value = c - '0';
				while (++pos < maxPos) {
					c = version.charAt(pos);
					if (!(c >= '0' && c <= '9' && isAllowed(c)))
						break;
					value *= 10;
					value += (c - '0');
				}
				if (negate)
					value = -value;
			} else {
				if (c < '0' || c > '9')
					return false;

				// Parse to next non-digit
				//
				value = c - '0';
				while (++pos < maxPos) {
					c = version.charAt(pos);
					if (c < '0' || c > '9')
						break;
					value *= 10;
					value += (c - '0');
				}
			}

			int len = pos - start;
			if (rangeMin > len || len > rangeMax)
				return false;

			if (!isIgnored())
				segments.add(Version.valueOf(value));
			info.setPosition(pos);
			return true;
		}

		void toString(StringBuffer sb) {
			sb.append(signed ? 'N' : 'n');
			super.toString(sb);
		}
	}

	private static class PadFragment extends ElementFragment {
		private static final long serialVersionUID = 5052010199974380170L;

		PadFragment(Qualifier qualifier) {
			super(null, qualifier);
		}

		boolean parseOne(List segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			if (pos >= maxPos || version.charAt(pos) != 'p')
				return false;

			int[] position = new int[] {++pos};
			Comparable v = VersionParser.parseRawElement(version, position, maxPos);
			if (v == null)
				return false;

			if (!isIgnored())
				info.setPadValue(v);
			info.setPosition(position[0]);
			return true;
		}

		void toString(StringBuffer sb) {
			sb.append('p');
			super.toString(sb);
		}
	}

	private static class QuotedFragment extends RangeFragment {
		private static final long serialVersionUID = 6057751133533608969L;

		QuotedFragment(VersionFormatParser.Instructions instr, Qualifier qualifier) {
			super(instr, qualifier);
		}

		boolean parseOne(List segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			if (pos >= maxPos)
				return false;

			char endQuote;
			char quote = version.charAt(pos);
			switch (quote) {
				case '<' :
					endQuote = '>';
					break;
				case '{' :
					endQuote = '}';
					break;
				case '(' :
					endQuote = ')';
					break;
				case '[' :
					endQuote = ']';
					break;
				case '>' :
					endQuote = '<';
					break;
				case '}' :
					endQuote = '{';
					break;
				case ')' :
					endQuote = '(';
					break;
				case ']' :
					endQuote = '[';
					break;
				default :
					if (VersionParser.isLetterOrDigit(quote))
						return false;
					endQuote = quote;
			}
			int start = ++pos;
			char c = version.charAt(pos);
			while (c != endQuote && isAllowed(c) && ++pos < maxPos)
				c = version.charAt(pos);

			if (c != endQuote || rangeMin > pos - start)
				// End quote not found
				return false;

			int len = pos - start;
			if (rangeMin > len || len > rangeMax)
				return false;

			if (!isIgnored())
				segments.add(version.substring(start, pos));
			info.setPosition(++pos); // Skip quote
			return true;
		}

		void toString(StringBuffer sb) {
			sb.append('q');
			super.toString(sb);
		}
	}

	private static abstract class RangeFragment extends ElementFragment {
		private static final long serialVersionUID = -6680402803630334708L;
		final char[] characters;
		final boolean inverted;
		final int rangeMax;
		final int rangeMin;

		RangeFragment(VersionFormatParser.Instructions instr, Qualifier qualifier) {
			super(instr, qualifier);
			if (instr == null) {
				characters = null;
				inverted = false;
				rangeMin = 0;
				rangeMax = Integer.MAX_VALUE;
			} else {
				characters = instr.characters;
				inverted = instr.inverted;
				rangeMin = instr.rangeMin;
				rangeMax = instr.rangeMax;
			}
		}

		/**
		 * Checks that pos is at a valid character position, that we
		 * have at least the required minimum characters left, and
		 * if a maximum number of characters is set, limits the
		 * returned value to a maxPos that reflects that maximum.
		 * @param pos the current position
		 * @param maxPos the current maxPos
		 * @return maxPos, possibly limited by rangeMax
		 */
		int checkRange(int pos, int maxPos) {
			int check = pos;
			if (rangeMin == 0)
				check++; // Verify one character
			else
				check += rangeMin;

			if (check > maxPos)
				// Less then min characters left
				maxPos = -1;
			else {
				if (rangeMax != Integer.MAX_VALUE) {
					check = pos + rangeMax;
					if (check < maxPos)
						maxPos = check;
				}
			}
			return maxPos;
		}

		boolean isAllowed(char c) {
			char[] crs = characters;
			if (crs != null) {
				int idx = crs.length;
				while (--idx >= 0)
					if (c == crs[idx])
						return !inverted;
				return inverted;
			}
			return true;
		}

		void toString(StringBuffer sb) {
			if (characters != null)
				appendCharacterRange(sb, characters, inverted);
			if (rangeMin != 0 || rangeMax != Integer.MAX_VALUE) {
				sb.append('=');
				sb.append('{');
				sb.append(rangeMin);
				if (rangeMin != rangeMax) {
					sb.append(',');
					if (rangeMax != Integer.MAX_VALUE)
						sb.append(rangeMax);
				}
				sb.append('}');
				sb.append(';');
			}
			super.toString(sb);
		}
	}

	private static class RawFragment extends ElementFragment {
		private static final long serialVersionUID = 4107448125256042602L;

		RawFragment(VersionFormatParser.Instructions processing, Qualifier qualifier) {
			super(processing, qualifier);
		}

		boolean parseOne(List segments, String version, int maxPos, TreeInfo info) {
			int[] position = new int[] {info.getPosition()};
			Comparable v = VersionParser.parseRawElement(version, position, maxPos);
			if (v == null)
				return false;

			if (!isIgnored())
				segments.add(v);
			info.setPosition(position[0]);
			return true;
		}

		void toString(StringBuffer sb) {
			sb.append('r');
			super.toString(sb);
		}
	}

	private static class StringFragment extends RangeFragment {
		private static final long serialVersionUID = -2265924553606430164L;
		final boolean anyChar;

		StringFragment(VersionFormatParser.Instructions instr, Qualifier qualifier, boolean noLimit) {
			super(instr, qualifier);
			anyChar = noLimit;
		}

		boolean parseOne(List segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			maxPos = checkRange(pos, maxPos);
			if (maxPos < 0)
				return false;

			// Parse to next delimiter or end of string
			//
			int start = pos;
			if (characters != null) {
				if (anyChar) {
					// Swallow everything that matches the allowed characters
					for (; pos < maxPos; ++pos) {
						if (!isAllowed(version.charAt(pos)))
							break;
					}
				} else {
					// Swallow letters that matches the allowed characters
					for (; pos < maxPos; ++pos) {
						char c = version.charAt(pos);
						if (!(VersionParser.isLetter(c) && isAllowed(c)))
							break;
					}
				}
			} else {
				if (anyChar)
					// Swallow all characters
					pos = maxPos;
				else {
					// Swallow all letters
					for (; pos < maxPos; ++pos) {
						if (!VersionParser.isLetter(version.charAt(pos)))
							break;
					}
				}
			}
			int len = pos - start;
			if (len == 0 || rangeMin > len || len > rangeMax)
				return false;

			if (!isIgnored())
				segments.add(version.substring(start, pos));
			info.setPosition(pos);
			return true;
		}

		void toString(StringBuffer sb) {
			sb.append(anyChar ? 'S' : 's');
			super.toString(sb);
		}
	}

	private static class TreeInfo extends ArrayList {
		private static final long serialVersionUID = 4770093863009659750L;

		private static class StateInfo {
			Fragment fragment;
			int segmentCount;
			int position;

			StateInfo(int position, int segmentCount, Fragment fragment) {
				this.fragment = fragment;
				this.position = position;
				this.segmentCount = segmentCount;
			}
		}

		private Comparable padValue;
		private int top;

		TreeInfo(Fragment frag, int pos) {
			add(new StateInfo(pos, 0, frag));
			top = 0;
		}

		Comparable getPadValue() {
			return padValue;
		}

		int getPosition() {
			return ((StateInfo) get(top)).position;
		}

		void popState(List segments, Fragment frag) {
			int idx = top;
			while (idx > 0) {
				StateInfo si = (StateInfo) get(idx);
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
			int pos = ((StateInfo) get(top)).position;
			if (++top == size())
				add(new StateInfo(pos, segCount, fragment));
			else {
				StateInfo si = (StateInfo) get(top);
				si.fragment = fragment;
				si.position = pos;
				si.segmentCount = segCount;
			}
		}

		void setPadValue(Comparable pad) {
			padValue = pad;
		}

		void setPosition(int pos) {
			((StateInfo) get(top)).position = pos;
		}
	}

	/**
	 * The predefined OSGi format that is used when parsing OSGi
	 * versions.
	 */
	public static final VersionFormat OSGI_FORMAT;

	/**
	 * The predefined OSGi format that is used when parsing raw
	 * versions.
	 */
	public static final VersionFormat RAW_FORMAT;

	private static final Map formatCache = Collections.synchronizedMap(new HashMap());

	private static final String OSGI_FORMAT_STRING = "n[.n=0;[.n=0;[.S=[A-Za-z0-9_-];]]]"; //$NON-NLS-1$

	private static final String RAW_FORMAT_STRING = "r(.r)*p?"; //$NON-NLS-1$

	static {
		try {
			VersionFormatParser parser = new VersionFormatParser();
			OSGI_FORMAT = new VersionFormat(parser.compile(OSGI_FORMAT_STRING, 0, OSGI_FORMAT_STRING.length()));
			formatCache.put(OSGI_FORMAT_STRING, OSGI_FORMAT);
			RAW_FORMAT = new RawFormat(parser.compile(RAW_FORMAT_STRING, 0, RAW_FORMAT_STRING.length()));
			formatCache.put(RAW_FORMAT_STRING, RAW_FORMAT);
		} catch (FormatException e) {
			// If this happens, something is wrong with the actual
			// implementation of the FormatCompiler.
			//
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Compile a version format string into a compiled format. This method is
	 * shorthand for:<pre>CompiledFormat.compile(format, 0, format.length())</pre>.
	 *
	 * @param format The format to compile.
	 * @return The compiled format
	 * @throws FormatException If the format could not be compiled
	 */
	public static VersionFormat compile(String format) throws FormatException {
		return compile(format, 0, format.length());
	}

	/**
	 * Compile a version format string into a compiled format. The parsing starts
	 * at position start and ends at position end. The returned format is cached so
	 * subsequent calls to this method using the same format string will yield the
	 * same compiled format instance.
	 *
	 * @param format The format string to compile.
	 * @param start Start position in the format string
	 * @param end End position in the format string
	 * @return The compiled format
	 * @throws FormatException If the format could not be compiled
	 */
	public static VersionFormat compile(String format, int start, int end) throws FormatException {
		String fmtString = format.substring(start, end).intern();
		synchronized (fmtString) {
			VersionFormat fmt = (VersionFormat) formatCache.get(fmtString);
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
	 * @param version The version to parse.
	 * @param originalFormat The original format to assign to the created version. Can be <code>null</code>.
	 * @param original The original version string to assign to the created version. Can be <code>null</code>.
	 * @return A created version
	 * @throws IllegalArgumentException If the version string could not be parsed.
	 */
	public static Version parseRaw(String version, VersionFormat originalFormat, String original) {
		Comparable[] padReturn = new Comparable[1];
		Comparable[] vector = RAW_FORMAT.parse(version, 0, version.length(), padReturn);
		return new Version(vector, padReturn[0], originalFormat, original);
	}

	static void appendCharacterRange(StringBuffer sb, char[] range, boolean inverted) {
		sb.append('=');
		sb.append('[');
		if (inverted)
			sb.append('^');
		int top = range.length;
		for (int idx = 0; idx < top; ++idx) {
			char b = range[idx];
			if (b == '\\' || b == ']' || (b == '-' && idx + 1 < top))
				sb.append('\\');

			sb.append(b);
			int ndx = idx + 1;
			if (ndx + 2 < top) {
				char c = b;
				for (; ndx < top; ++ndx) {
					char n = range[ndx];
					if (c + 1 != n)
						break;
					c = n;
				}
				if (ndx <= idx + 3)
					continue;

				sb.append('-');
				if (c == '\\' || c == ']' || (c == '-' && idx + 1 < top))
					sb.append('\\');
				sb.append(c);
				idx = ndx - 1;
			}
		}
		sb.append(']');
		sb.append(';');
	}

	static Fragment createAutoFragment(VersionFormatParser.Instructions instr, Qualifier qualifier) {
		return new AutoFragment(instr, qualifier);
	}

	static Fragment createDelimiterFragment(VersionFormatParser.Instructions instr, Qualifier qualifier) {
		return new DelimiterFragment(instr, qualifier);
	}

	static Fragment createGroupFragment(VersionFormatParser.Instructions instr, Qualifier qualifier, Fragment[] fragments, boolean array) {
		return new GroupFragment(instr, qualifier, fragments, array);
	}

	static Fragment createLiteralFragment(Qualifier qualifier, String literal) {
		return new LiteralFragment(qualifier, literal);
	}

	static Fragment createNumberFragment(VersionFormatParser.Instructions instr, Qualifier qualifier, boolean signed) {
		return new NumberFragment(instr, qualifier, signed);
	}

	static Fragment createPadFragment(Qualifier qualifier) {
		return new PadFragment(qualifier);
	}

	static Fragment createQuotedFragment(VersionFormatParser.Instructions instr, Qualifier qualifier) {
		return new QuotedFragment(instr, qualifier);
	}

	static Fragment createRawFragment(VersionFormatParser.Instructions instr, Qualifier qualifier) {
		return new RawFragment(instr, qualifier);
	}

	static Fragment createStringFragment(VersionFormatParser.Instructions instr, Qualifier qualifier, boolean unbound) {
		return new StringFragment(instr, qualifier, unbound);
	}

	static void toStringEscaped(StringBuffer sb, String value, String escapes) {
		for (int idx = 0; idx < value.length(); ++idx) {
			char c = value.charAt(idx);
			if (c == '\\' || escapes.indexOf(c) >= 0)
				sb.append('\\');
			sb.append(c);
		}
	}

	private final Fragment topFragment;

	private String fmtString;

	VersionFormat(Fragment topFragment) {
		this.topFragment = topFragment;
	}

	public boolean equals(Object o) {
		return this == o || o instanceof VersionFormat && toString().equals(o.toString());
	}

	public int hashCode() {
		return 11 * toString().hashCode();
	}

	/**
	 * Parse the given version string.
	 * @param version The version string to parse.
	 * @return A created version.
	 * @throws IllegalArgumentException If the version string could not be parsed.
	 */
	public Version parse(String version) {
		return parse(version, 0, version.length());
	}

	/**
	 * Parse the given version string.
	 * @param version The version string to parse.
	 * @param start Start position in the version string
	 * @return A created version.
	 * @throws IllegalArgumentException If the version string could not be parsed.
	 */
	public Version parse(String version, int start, int maxPos) {
		Comparable[] padReturn = new Comparable[1];
		Comparable[] vector = parse(version, start, maxPos, padReturn);
		return new Version(vector, padReturn[0], this, version.substring(start, maxPos));
	}

	/**
	 * Returns the string representation of this compiled format
	 */
	public synchronized String toString() {
		if (fmtString == null) {
			StringBuffer sb = new StringBuffer();
			toString(sb);
		}
		return fmtString;
	}

	/**
	 * Appends the string representation of this compiled format to
	 * the given StringBuffer.
	 * @param sb The buffer that will receive the string representation
	 */
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

	TreeInfo createInfo(int start) {
		return new TreeInfo(topFragment, start);
	}

	Comparable[] parse(String version, int start, int maxPos, Comparable[] padReturn) {
		ArrayList entries = new ArrayList();
		if (start == maxPos)
			throw new IllegalArgumentException(NLS.bind(Messages.format_0_unable_to_parse_empty_version, this, version.substring(start, maxPos)));
		TreeInfo info = new TreeInfo(topFragment, start);
		if (!(topFragment.parse(entries, version, maxPos, info) && info.getPosition() == maxPos))
			throw new IllegalArgumentException(NLS.bind(Messages.format_0_unable_to_parse_1, this, version.substring(start, maxPos)));
		Comparable pad = info.getPadValue();
		VersionParser.removeRedundantTrail(entries, pad);
		padReturn[0] = pad;
		return (Comparable[]) entries.toArray(new Comparable[entries.size()]);
	}

	// Preserve cache during deserialization
	private Object readResolve() {
		synchronized (formatCache) {
			String string = toString();
			VersionFormat fmt = (VersionFormat) formatCache.put(string, this);
			if (fmt == null)
				fmt = this;
			else
				// Put old format back
				formatCache.put(string, fmt);
			return fmt;
		}
	}
}

class RawFormat extends VersionFormat {
	private static final long serialVersionUID = 8851695938450999819L;

	RawFormat(Fragment topFragment) {
		super(topFragment);
	}

	/**
	 * Parse but do not assign this format as the Version format nor the version
	 * string as the original.
	 */
	public Version parse(String version, int start, int maxPos) {
		Comparable[] padReturn = new Comparable[1];
		Comparable[] vector = parse(version, start, maxPos, padReturn);
		return new Version(vector, padReturn[0], null, null);
	}

	// Preserve singleton when deserialized
	private Object readResolve() {
		return RAW_FORMAT;
	}
}