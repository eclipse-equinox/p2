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

import java.io.Serializable;
import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.EnumDefinition.EnumSegment;
import org.eclipse.equinox.internal.p2.metadata.VersionFormat.TreeInfo;
import org.eclipse.equinox.p2.metadata.VersionFormatException;
import org.eclipse.osgi.util.NLS;

/**
 * This is the Omni Version Format parser. It will parse a version format in string form
 * into a group of {@link VersionFormatParser.Fragment} elements. That group, wrapped in a
 * {@link VersionFormat}, becomes the parser for versions corresponding to the format.
 *
 * The class is not intended to included in a public API. Instead VersionFormats should
 * be created using {@link VersionFormat#parse(String)}
 *
 */
class VersionFormatParser {

	private static class EnumInstruction {
		private final EnumDefinition definition;
		private final boolean caseSensitive;
		private final boolean optional;
		private final boolean begins;

		EnumInstruction(EnumDefinition definition, boolean caseSensitive, boolean optional, boolean begins) {
			this.definition = definition;
			this.caseSensitive = caseSensitive;
			this.optional = optional;
			this.begins = begins;
		}

		EnumSegment getEnumSegment(RangeFragment fragment, String version, int[] posHolder, int maxPos) {
			int pos = posHolder[0];
			int len = maxPos - pos;
			int minLen = definition.getShortestLength();
			if (minLen > len)
				return null;

			int maxLen = definition.getLongestLength();
			if (maxLen < len)
				len = maxLen;

			++len;
			while (--len >= minLen) {
				int last = pos + len;
				if (!begins && last < maxPos) {
					char c = version.charAt(last);
					if (VersionParser.isLetter(c) && fragment.isAllowed(c)) {
						// We are not allowed to truncate at this point
						continue;
					}
				}
				String identifier = version.substring(pos, last);
				if (!caseSensitive)
					identifier = identifier.toLowerCase();
				int ordinal = definition.getOrdinal(identifier);
				if (ordinal >= 0) {
					posHolder[0] = pos + len;
					return definition.getSegment(ordinal);
				}
			}
			return null;
		}

		void toString(StringBuffer bld) {
			bld.append('=');
			definition.toString(bld);
			if (begins)
				bld.append('b');
			if (!caseSensitive)
				bld.append('i');
			if (optional)
				bld.append('?');
			bld.append(';');
		}

		public boolean isOptional() {
			return optional;
		}
	}

	static class Instructions {
		char[] characters = null;
		Comparable<?> defaultValue = null;
		char oppositeTranslationChar = 0;
		int oppositeTranslationRepeat = 0;
		boolean ignore = false;
		boolean inverted = false;
		Comparable<?> padValue = null;
		int rangeMax = Integer.MAX_VALUE;
		int rangeMin = 0;
		EnumInstruction enumInstruction = null;
	}

	static final Qualifier EXACT_ONE_QUALIFIER = new Qualifier(1, 1);

	static final Qualifier ONE_OR_MANY_QUALIFIER = new Qualifier(1, Integer.MAX_VALUE);

	static final Qualifier ZERO_OR_MANY_QUALIFIER = new Qualifier(0, Integer.MAX_VALUE);

	static final Qualifier ZERO_OR_ONE_QUALIFIER = new Qualifier(0, 1);

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

		Comparable<?> getDefaultValue() {
			return null;
		}

		Fragment getFirstLeaf() {
			return this;
		}

		Comparable<?> getPadValue() {
			return null;
		}

		Qualifier getQualifier() {
			return qualifier;
		}

		boolean parse(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
			return qualifier.parse(new Fragment[] {this}, 0, segments, version, maxPos, info);
		}

		abstract boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info);

		void setDefaults(List<Comparable<?>> segments) {
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

		boolean parse(Fragment[] fragments, int fragIdx, List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
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
				if (idx < max) {
					if (max != Integer.MAX_VALUE) {
						for (; idx < max; ++idx)
							fragment.setDefaults(segments);
					}
				} else {
					if (fragment instanceof StringFragment) {
						// Check for translations if we default to for MINS or MAXS
						StringFragment stringFrag = (StringFragment) fragment;
						Comparable<?> opposite = stringFrag.getOppositeDefaultValue();
						if (opposite != null) {
							idx = segments.size() - 1;
							if (stringFrag.isOppositeTranslation(segments.get(idx)))
								segments.set(idx, opposite);
						}
					}
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

		boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			maxPos = checkRange(pos, maxPos);
			if (maxPos < 0)
				return false;

			char c = version.charAt(pos);
			if (VersionParser.isDigit(c) && isAllowed(c) && (enumInstruction == null || enumInstruction.isOptional())) {
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
					segments.add(VersionParser.valueOf(value));
				info.setPosition(pos);
				return true;
			}

			int start = pos;
			if (enumInstruction != null) {
				int[] posHolder = new int[] {pos};
				EnumSegment es = enumInstruction.getEnumSegment(this, version, posHolder, maxPos);
				if (es != null) {
					pos = posHolder[0];
					int len = pos - start;
					if (rangeMin > len || len > rangeMax)
						return false;
					if (!isIgnored())
						segments.add(es);
					info.setPosition(pos);
					return true;
				}
				if (!enumInstruction.isOptional())
					return false;
			}

			if (!(VersionParser.isLetter(c) && isAllowed(c)))
				return false;

			// Parse to next non-letter or next delimiter
			//
			for (++pos; pos < maxPos; ++pos) {
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

		boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
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

	static boolean equalsAllowNull(Object a, Object b) {
		return (a == null) ? (b == null) : (b != null && a.equals(b));
	}

	private static abstract class ElementFragment extends Fragment {
		private static final long serialVersionUID = -6834591415456539713L;
		private final Comparable<?> defaultValue;
		private final boolean ignored;
		private final Comparable<?> padValue;

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

		Comparable<?> getDefaultValue() {
			return defaultValue;
		}

		Comparable<?> getPadValue() {
			return padValue;
		}

		boolean isIgnored() {
			return ignored;
		}

		void setDefaults(List<Comparable<?>> segments) {
			Comparable<?> defaultVal = getDefaultValue();
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
				VersionFormat.rawToString(sb, false, defaultValue);
				sb.append(';');
			}
			if (padValue != null) {
				sb.append('=');
				sb.append('p');
				VersionFormat.rawToString(sb, false, padValue);
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

		boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
			if (array) {
				ArrayList<Comparable<?>> subSegs = new ArrayList<Comparable<?>>();
				boolean success = fragments[0].getQualifier().parse(fragments, 0, subSegs, version, maxPos, info);
				if (!success || subSegs.isEmpty())
					return false;

				Comparable<?> padValue = info.getPadValue();
				if (padValue != null)
					info.setPadValue(null); // Prevent outer group from getting this.
				else
					padValue = getPadValue();

				padValue = VersionParser.removeRedundantTrail(segments, padValue);
				segments.add(new VersionVector(subSegs.toArray(new Comparable[subSegs.size()]), padValue));
				return true;
			}

			if (fragments[0].getQualifier().parse(fragments, 0, segments, version, maxPos, info)) {
				Comparable<?> padValue = getPadValue();
				if (padValue != null)
					info.setPadValue(padValue);
				return true;
			}
			return false;
		}

		void setDefaults(List<Comparable<?>> segments) {
			Comparable<?> dflt = getDefaultValue();
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

		boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
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
				VersionFormatParser.toStringEscaped(sb, str, "\'"); //$NON-NLS-1$
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

		boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
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
				segments.add(VersionParser.valueOf(value));
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

		boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			if (pos >= maxPos || version.charAt(pos) != 'p')
				return false;

			int[] position = new int[] {++pos};
			Comparable<?> v = VersionParser.parseRawElement(version, position, maxPos);
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

		boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
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
		final EnumInstruction enumInstruction;

		RangeFragment(VersionFormatParser.Instructions instr, Qualifier qualifier) {
			super(instr, qualifier);
			if (instr == null) {
				characters = null;
				inverted = false;
				rangeMin = 0;
				rangeMax = Integer.MAX_VALUE;
				enumInstruction = null;
			} else {
				characters = instr.characters;
				inverted = instr.inverted;
				rangeMin = instr.rangeMin;
				rangeMax = instr.rangeMax;
				enumInstruction = instr.enumInstruction;
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
			if (enumInstruction != null)
				enumInstruction.toString(sb);
			super.toString(sb);
		}
	}

	private static class RawFragment extends ElementFragment {
		private static final long serialVersionUID = 4107448125256042602L;

		RawFragment(VersionFormatParser.Instructions processing, Qualifier qualifier) {
			super(processing, qualifier);
		}

		boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
			int[] position = new int[] {info.getPosition()};
			Comparable<?> v = VersionParser.parseRawElement(version, position, maxPos);
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
		private final char oppositeTranslationChar;
		private final int oppositeTranslationRepeat;

		StringFragment(VersionFormatParser.Instructions instr, Qualifier qualifier, boolean noLimit) {
			super(instr, qualifier);
			anyChar = noLimit;
			char otc = 0;
			int otr = 0;
			if (instr != null) {
				otc = instr.oppositeTranslationChar;
				otr = instr.oppositeTranslationRepeat;
				if (instr.defaultValue == VersionVector.MINS_VALUE) {
					if (otc == 0)
						otc = 'z';
					if (otr == 0)
						otr = 3;
				} else if (instr.defaultValue == VersionVector.MAXS_VALUE) {
					if (otc == 0)
						otc = '-';
					otr = 1;
				}
			}
			oppositeTranslationChar = otc;
			oppositeTranslationRepeat = otr;
		}

		Comparable<?> getOppositeDefaultValue() {
			Comparable<?> dflt = getDefaultValue();
			return dflt == VersionVector.MAXS_VALUE ? VersionVector.MINS_VALUE : (dflt == VersionVector.MINS_VALUE ? VersionVector.MAXS_VALUE : null);
		}

		public boolean isOppositeTranslation(Object val) {
			if (val instanceof String) {
				String str = (String) val;
				int idx = oppositeTranslationRepeat;
				if (str.length() == idx) {
					while (--idx >= 0)
						if (str.charAt(idx) != oppositeTranslationChar)
							break;
					return idx < 0;
				}
			}
			return false;
		}

		boolean parseOne(List<Comparable<?>> segments, String version, int maxPos, TreeInfo info) {
			int pos = info.getPosition();
			maxPos = checkRange(pos, maxPos);
			if (maxPos < 0)
				return false;

			int start = pos;
			if (enumInstruction != null) {
				int[] posHolder = new int[] {pos};
				EnumSegment es = enumInstruction.getEnumSegment(this, version, posHolder, maxPos);
				if (es != null) {
					pos = posHolder[0];
					int len = pos - start;
					if (rangeMin > len || len > rangeMax)
						return false;
					if (!isIgnored())
						segments.add(es);
					info.setPosition(pos);
					return true;
				}
				if (!enumInstruction.isOptional())
					return false;
			}

			// Parse to next delimiter or end of string
			//
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

	private int current;

	private List<Fragment> currentList;

	private int eos;

	private String format;

	private int start;

	Fragment compile(String fmt, int pos, int maxPos) throws VersionFormatException {
		format = fmt;
		if (start >= maxPos)
			throw new VersionFormatException(Messages.format_is_empty);

		start = pos;
		current = pos;
		eos = maxPos;
		currentList = new ArrayList<Fragment>();
		while (current < eos)
			parseFragment();

		Fragment topFrag;
		switch (currentList.size()) {
			case 0 :
				throw new VersionFormatException(Messages.format_is_empty);
			case 1 :
				Fragment frag = currentList.get(0);
				if (frag.isGroup()) {
					topFrag = frag;
					break;
				}
				// Fall through to default
			default :
				topFrag = createGroupFragment(null, EXACT_ONE_QUALIFIER, currentList.toArray(new Fragment[currentList.size()]), false);
		}
		currentList = null;
		return topFrag;
	}

	private void assertChar(char expected) throws VersionFormatException {
		if (current >= eos)
			throw formatException(NLS.bind(Messages.premature_end_of_format_expected_0, new String(new char[] {expected})));

		char c = format.charAt(current);
		if (c != expected)
			throw formatException(c, new String(new char[] {expected}));
		++current;
	}

	private VersionFormatException formatException(char found, String expected) {
		return formatException(new String(new char[] {found}), expected);
	}

	private VersionFormatException formatException(String message) {
		return new VersionFormatException(NLS.bind(Messages.syntax_error_in_version_format_0_1_2, new Object[] {format.substring(start, eos), Integer.valueOf(current), message}));
	}

	private VersionFormatException formatException(String found, String expected) {
		return new VersionFormatException(NLS.bind(Messages.syntax_error_in_version_format_0_1_found_2_expected_3, new Object[] {format.substring(start, eos), Integer.valueOf(current), found, expected}));
	}

	private VersionFormatException illegalControlCharacter(char c) {
		return formatException(NLS.bind(Messages.illegal_character_encountered_ascii_0, VersionParser.valueOf(c)));
	}

	private String parseAndConsiderEscapeUntil(char endChar) throws VersionFormatException {
		StringBuffer sb = new StringBuffer();
		while (current < eos) {
			char c = format.charAt(current++);
			if (c == endChar)
				break;

			if (c < 32)
				throw illegalControlCharacter(c);

			if (c == '\\') {
				if (current == eos)
					throw formatException(Messages.EOS_after_escape);
				c = format.charAt(current++);
				if (c < 32)
					throw illegalControlCharacter(c);
			}
			sb.append(c);
		}
		return sb.toString();
	}

	private void parseAuto() throws VersionFormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.auto_can_not_have_pad_value);
		}
		currentList.add(createAutoFragment(ep, parseQualifier()));
	}

	private void parseBracketGroup() throws VersionFormatException {
		List<Fragment> saveList = currentList;
		currentList = new ArrayList<Fragment>();
		while (current < eos && format.charAt(current) != ']')
			parseFragment();

		if (current == eos)
			throw formatException(NLS.bind(Messages.premature_end_of_format_expected_0, "]")); //$NON-NLS-1$

		++current;
		VersionFormatParser.Instructions ep = parseProcessing();
		saveList.add(createGroupFragment(ep, ZERO_OR_ONE_QUALIFIER, currentList.toArray(new Fragment[currentList.size()]), false));
		currentList = saveList;
	}

	private void parseCharacterGroup(VersionFormatParser.Instructions ep) throws VersionFormatException {
		assertChar('[');

		StringBuffer sb = new StringBuffer();
		outer: for (; current < eos; ++current) {
			char c = format.charAt(current);
			switch (c) {
				case '\\' :
					if (current + 1 < eos) {
						sb.append(format.charAt(++current));
						continue;
					}
					throw formatException(Messages.premature_end_of_format);
				case '^' :
					if (sb.length() == 0)
						ep.inverted = true;
					else
						sb.append(c);
					continue;
				case ']' :
					break outer;
				case '-' :
					if (sb.length() > 0 && current + 1 < eos) {
						char rangeEnd = format.charAt(++current);
						if (rangeEnd == ']') {
							// Use dash verbatim when last in range
							sb.append(c);
							break outer;
						}

						char rangeStart = sb.charAt(sb.length() - 1);
						if (rangeEnd < rangeStart)
							throw formatException(Messages.negative_character_range);
						while (++rangeStart <= rangeEnd)
							sb.append(rangeStart);
						continue;
					}
					// Fall through to default
				default :
					if (c < 32)
						throw illegalControlCharacter(c);
					sb.append(c);
			}
		}
		assertChar(']');
		int top = sb.length();
		char[] chars = new char[top];
		sb.getChars(0, top, chars, 0);
		ep.characters = chars;
	}

	private void parseDelimiter() throws VersionFormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.rangeMin != 0 || ep.rangeMax != Integer.MAX_VALUE)
				throw formatException(Messages.delimiter_can_not_have_range);
			if (ep.ignore)
				throw formatException(Messages.delimiter_can_not_be_ignored);
			if (ep.defaultValue != null)
				throw formatException(Messages.delimiter_can_not_have_default_value);
			if (ep.padValue != null)
				throw formatException(Messages.delimiter_can_not_have_pad_value);
		}
		currentList.add(createDelimiterFragment(ep, parseQualifier()));
	}

	private void parseEnum(Instructions processing) throws VersionFormatException {
		++current;
		ArrayList<List<String>> identifiers = new ArrayList<List<String>>();
		ArrayList<String> idents = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		for (;;) {
			if (current >= eos)
				throw formatException(Messages.bad_enum_definition);

			char c = format.charAt(current++);
			while (c != '}' && c != ',' && c != '=') {
				if (current >= eos || c <= ' ')
					throw formatException(Messages.bad_enum_definition);
				if (c == '\\') {
					c = format.charAt(current++);
					if (current >= eos)
						throw formatException(Messages.bad_enum_definition);
				}
				sb.append(c);
				c = format.charAt(current++);
			}
			idents.add(sb.toString());
			sb.setLength(0);
			if (c == '=')
				continue;

			identifiers.add(idents);
			if (c == '}')
				break;

			// c must be ',' at this point
			idents = new ArrayList<String>();
		}

		boolean enumCaseSensitive = true;
		boolean enumOptional = false;
		boolean enumBegins = false;
		while (current < eos) {
			char c = format.charAt(current);
			if (c == 'i') {
				enumCaseSensitive = false;
				current++;
			} else if (c == 'b') {
				enumBegins = true;
				current++;
			} else if (c == '?') {
				enumOptional = true;
				current++;
			} else
				break;
		}

		// Ensure that all identifiers are unique and make them
		// lower case if necessary
		HashSet<String> unique = new HashSet<String>();
		int ordinal = identifiers.size();
		while (--ordinal >= 0) {
			List<String> ids = identifiers.get(ordinal);
			int idx = ids.size();
			while (--idx >= 0) {
				String id = ids.get(idx);
				if (!enumCaseSensitive)
					id = id.toLowerCase();
				if (!unique.add(id))
					throw formatException(Messages.bad_enum_definition);
				ids.set(idx, id);
			}
		}
		EnumDefinition enumDefinition = EnumDefinition.getEnumDefinition(identifiers);
		processing.enumInstruction = new EnumInstruction(enumDefinition, enumCaseSensitive, enumOptional, enumBegins);
	}

	private void parseFragment() throws VersionFormatException {
		if (current == eos)
			throw formatException(Messages.premature_end_of_format);
		char c = format.charAt(current++);
		switch (c) {
			case '(' :
				parseGroup(false);
				break;
			case '<' :
				parseGroup(true);
				break;
			case '[' :
				parseBracketGroup();
				break;
			case 'a' :
				parseAuto();
				break;
			case 'r' :
				parseRaw();
				break;
			case 'n' :
				parseNumber(false);
				break;
			case 'N' :
				parseNumber(true);
				break;
			case 's' :
				parseString(false);
				break;
			case 'S' :
				parseString(true);
				break;
			case 'd' :
				parseDelimiter();
				break;
			case 'q' :
				parseQuotedString();
				break;
			case 'p' :
				parsePad();
				break;
			default :
				parseLiteral(c);
		}
	}

	private void parseGroup(boolean array) throws VersionFormatException {
		List<Fragment> saveList = currentList;
		currentList = new ArrayList<Fragment>();
		char expectedEnd = array ? '>' : ')';
		while (current < eos && format.charAt(current) != expectedEnd)
			parseFragment();
		assertChar(expectedEnd);

		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.characters != null)
				throw formatException(Messages.array_can_not_have_character_group);
			if (ep.rangeMax != Integer.MAX_VALUE && ep.padValue != null)
				throw formatException(Messages.cannot_combine_range_upper_bound_with_pad_value);
			if (ep.enumInstruction != null)
				throw formatException(Messages.array_can_not_have_enum);
		}

		if (currentList.isEmpty())
			throw formatException(array ? Messages.array_can_not_be_empty : Messages.group_can_not_be_empty);
		saveList.add(createGroupFragment(ep, parseQualifier(), currentList.toArray(new Fragment[currentList.size()]), array));
		currentList = saveList;
	}

	private int parseIntegerLiteral() throws VersionFormatException {
		if (current == eos)
			throw formatException(NLS.bind(Messages.premature_end_of_format_expected_0, "<integer>")); //$NON-NLS-1$

		char c = format.charAt(current);
		if (!VersionParser.isDigit(c))
			throw formatException(c, "<integer>"); //$NON-NLS-1$

		int value = c - '0';
		while (++current < eos) {
			c = format.charAt(current);
			if (!VersionParser.isDigit(c))
				break;
			value *= 10;
			value += (c - '0');
		}
		return value;
	}

	private void parseLiteral(char c) throws VersionFormatException {
		String value;
		switch (c) {
			case '\'' :
				value = parseAndConsiderEscapeUntil(c);
				break;
			case ')' :
			case ']' :
			case '{' :
			case '}' :
			case '?' :
			case '*' :
				throw formatException(c, "<literal>"); //$NON-NLS-1$
			default :
				if (VersionParser.isLetterOrDigit(c))
					throw formatException(c, "<literal>"); //$NON-NLS-1$

				if (c < 32)
					throw illegalControlCharacter(c);

				if (c == '\\') {
					if (current == eos)
						throw formatException(Messages.EOS_after_escape);
					c = format.charAt(current++);
					if (c < 32)
						throw illegalControlCharacter(c);
				}
				value = new String(new char[] {c});
		}
		currentList.add(createLiteralFragment(parseQualifier(), value));
	}

	private int[] parseMinMax() throws VersionFormatException {

		int max = Integer.MAX_VALUE;
		++current;
		int min = parseIntegerLiteral();
		char c = format.charAt(current);
		if (c == '}') {
			max = min;
			if (max == 0)
				throw formatException(Messages.range_max_cannot_be_zero);
			++current;
		} else if (c == ',' && current + 1 < eos) {
			if (format.charAt(++current) != '}') {
				max = parseIntegerLiteral();
				if (max == 0)
					throw formatException(Messages.range_max_cannot_be_zero);
				if (max < min)
					throw formatException(Messages.range_max_cannot_be_less_then_range_min);
			}
			assertChar('}');
		} else
			throw formatException(c, "},"); //$NON-NLS-1$
		return new int[] {min, max};
	}

	private void parseNumber(boolean signed) throws VersionFormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.number_can_not_have_pad_value);
		}
		currentList.add(createNumberFragment(ep, parseQualifier(), signed));
	}

	private void parsePad() throws VersionFormatException {
		currentList.add(createPadFragment(parseQualifier()));
	}

	private VersionFormatParser.Instructions parseProcessing() throws VersionFormatException {
		if (current >= eos)
			return null;

		char c = format.charAt(current);
		if (c != '=')
			return null;

		VersionFormatParser.Instructions ep = new VersionFormatParser.Instructions();
		do {
			current++;
			parseProcessingInstruction(ep);
		} while (current < eos && format.charAt(current) == '=');
		return ep;
	}

	private void parseProcessingInstruction(VersionFormatParser.Instructions processing) throws VersionFormatException {
		if (current == eos)
			throw formatException(Messages.premature_end_of_format);

		char c = format.charAt(current);
		if (c == 'p') {
			// =pad(<raw-element>);
			//
			if (processing.padValue != null)
				throw formatException(Messages.pad_defined_more_then_once);
			if (processing.ignore)
				throw formatException(Messages.cannot_combine_ignore_with_other_instruction);
			++current;
			processing.padValue = parseRawElement();
		} else if (c == '!') {
			// =ignore;
			//
			if (processing.ignore)
				throw formatException(Messages.ignore_defined_more_then_once);
			if (processing.padValue != null || processing.characters != null || processing.rangeMin != 0 || processing.rangeMax != Integer.MAX_VALUE || processing.defaultValue != null)
				throw formatException(Messages.cannot_combine_ignore_with_other_instruction);
			++current;
			processing.ignore = true;
		} else if (c == '[') {
			// =[<character group];
			//
			if (processing.characters != null)
				throw formatException(Messages.character_group_defined_more_then_once);
			if (processing.ignore)
				throw formatException(Messages.cannot_combine_ignore_with_other_instruction);
			parseCharacterGroup(processing);
		} else if (c == '{') {
			if (current + 1 == eos)
				throw formatException(Messages.premature_end_of_format);

			if (VersionParser.isDigit(format.charAt(current + 1))) {
				// ={min,max};
				//
				if (processing.rangeMin != 0 || processing.rangeMax != Integer.MAX_VALUE)
					throw formatException(Messages.range_defined_more_then_once);
				if (processing.ignore)
					throw formatException(Messages.cannot_combine_ignore_with_other_instruction);
				int[] minMax = parseMinMax();
				processing.rangeMin = minMax[0];
				processing.rangeMax = minMax[1];
			} else {
				// ={enum1,enum2,...};
				//
				if (processing.enumInstruction != null)
					throw formatException(Messages.enum_defined_more_then_once);
				parseEnum(processing);
			}
		} else {
			// =<raw-element>;
			if (processing.defaultValue != null)
				throw formatException(Messages.default_defined_more_then_once);
			if (processing.ignore)
				throw formatException(Messages.cannot_combine_ignore_with_other_instruction);
			Comparable<?> dflt = parseRawElement();
			processing.defaultValue = dflt;
			if (current < eos && format.charAt(current) == '{') {
				// =m{<translated min char>}
				// =''{<translated max char>,<max char repeat>}
				if (++current == eos)
					throw formatException(Messages.premature_end_of_format);
				processing.oppositeTranslationChar = format.charAt(current++);
				if (current == eos)
					throw formatException(Messages.premature_end_of_format);

				if (dflt == VersionVector.MINS_VALUE) {
					processing.oppositeTranslationRepeat = 3;
					if (format.charAt(current) == ',') {
						++current;
						processing.oppositeTranslationRepeat = parseIntegerLiteral();
					}
				} else if (dflt != VersionVector.MAXS_VALUE) {
					current -= 2;
					throw formatException(Messages.only_max_and_empty_string_defaults_can_have_translations);
				}
				assertChar('}');
			}
		}
		assertChar(';');
	}

	private Qualifier parseQualifier() throws VersionFormatException {
		if (current >= eos)
			return EXACT_ONE_QUALIFIER;

		char c = format.charAt(current);
		if (c == '?') {
			++current;
			return ZERO_OR_ONE_QUALIFIER;
		}

		if (c == '*') {
			++current;
			return ZERO_OR_MANY_QUALIFIER;
		}

		if (c == '+') {
			++current;
			return ONE_OR_MANY_QUALIFIER;
		}

		if (c != '{')
			return EXACT_ONE_QUALIFIER;

		int[] minMax = parseMinMax();
		int min = minMax[0];
		int max = minMax[1];

		// Use singletons for commonly used ranges
		//
		if (min == 0) {
			if (max == 1)
				return ZERO_OR_ONE_QUALIFIER;
			if (max == Integer.MAX_VALUE)
				return ZERO_OR_MANY_QUALIFIER;
		} else if (min == 1) {
			if (max == 1)
				return EXACT_ONE_QUALIFIER;
			if (max == Integer.MAX_VALUE)
				return ONE_OR_MANY_QUALIFIER;
		}
		return new Qualifier(min, max);
	}

	private void parseQuotedString() throws VersionFormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.string_can_not_have_pad_value);
		}
		currentList.add(createQuotedFragment(ep, parseQualifier()));
	}

	private void parseRaw() throws VersionFormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.raw_element_can_not_have_pad_value);
		}
		currentList.add(createRawFragment(ep, parseQualifier()));
	}

	private Comparable<?> parseRawElement() throws VersionFormatException {
		int[] position = new int[] {current};
		Comparable<?> v = VersionParser.parseRawElement(format, position, eos);
		if (v == null)
			throw new VersionFormatException(NLS.bind(Messages.raw_element_expected_0, format));
		current = position[0];
		return v;
	}

	private void parseString(boolean unlimited) throws VersionFormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.string_can_not_have_pad_value);
		}
		currentList.add(createStringFragment(ep, parseQualifier(), unlimited));
	}

	static void toStringEscaped(StringBuffer sb, String value, String escapes) {
		for (int idx = 0; idx < value.length(); ++idx) {
			char c = value.charAt(idx);
			if (c == '\\' || escapes.indexOf(c) >= 0)
				sb.append('\\');
			sb.append(c);
		}
	}
}
