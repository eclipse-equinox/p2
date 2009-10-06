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


import java.util.ArrayList;
import java.util.List;
import org.eclipse.osgi.util.NLS;

/**
 * This is the Omni Version Format parser. It will parse a version format in string form
 * into a group of {@link VersionFormat.Fragment} elements. That group, wrapped in a
 * {@link VersionFormat}, becomes the parser for versions corresponding to the format.
 *
 * The class is not intended to included in a public API. Instead VersionFormats should
 * be created using {@link VersionFormat#parse(String)}
 *
 */
class VersionFormatParser {

	static class Instructions {
		char[] characters = null;
		Comparable defaultValue = null;
		boolean ignore = false;
		boolean inverted = false;
		Comparable padValue = null;
		int rangeMax = Integer.MAX_VALUE;
		int rangeMin = 0;
	}

	static final VersionFormat.Qualifier EXACT_ONE_QUALIFIER = new VersionFormat.Qualifier(1, 1);

	static final VersionFormat.Qualifier ONE_OR_MANY_QUALIFIER = new VersionFormat.Qualifier(1, Integer.MAX_VALUE);

	static final VersionFormat.Qualifier ZERO_OR_MANY_QUALIFIER = new VersionFormat.Qualifier(0, Integer.MAX_VALUE);

	static final VersionFormat.Qualifier ZERO_OR_ONE_QUALIFIER = new VersionFormat.Qualifier(0, 1);

	private int current;

	private List currentList;

	private int eos;

	private String format;

	private int start;

	VersionFormat.Fragment compile(String fmt, int pos, int maxPos) throws FormatException {
		format = fmt;
		if (start >= maxPos)
			throw new FormatException(Messages.format_is_empty);

		start = pos;
		current = pos;
		eos = maxPos;
		currentList = new ArrayList();
		while (current < eos)
			parseFragment();

		VersionFormat.Fragment topFrag;
		switch (currentList.size()) {
			case 0 :
				throw new FormatException(Messages.format_is_empty);
			case 1 :
				VersionFormat.Fragment frag = (VersionFormat.Fragment) currentList.get(0);
				if (frag.isGroup()) {
					topFrag = frag;
					break;
				}
				// Fall through to default
			default :
				topFrag = VersionFormat.createGroupFragment(null, EXACT_ONE_QUALIFIER, (VersionFormat.Fragment[]) currentList.toArray(new VersionFormat.Fragment[currentList.size()]), false);
		}
		currentList = null;
		return topFrag;
	}

	private void assertChar(char expected) throws FormatException {
		if (current >= eos)
			throw formatException(NLS.bind(Messages.premature_end_of_format_expected_0, new String(new char[] {expected})));

		char c = format.charAt(current);
		if (c != expected)
			throw formatException(c, new String(new char[] {expected}));
		++current;
	}

	private FormatException formatException(char found, String expected) {
		return formatException(new String(new char[] {found}), expected);
	}

	private FormatException formatException(String message) {
		return new FormatException(NLS.bind(Messages.syntax_error_in_version_format_0_1_2, new Object[] {format.substring(start, eos), new Integer(current), message}));
	}

	private FormatException formatException(String found, String expected) {
		return new FormatException(NLS.bind(Messages.syntax_error_in_version_format_0_1_found_2_expected_3, new Object[] {format.substring(start, eos), new Integer(current), found, expected}));
	}

	private FormatException illegalControlCharacter(char c) {
		return formatException(NLS.bind(Messages.illegal_character_encountered_ascii_0, Version.valueOf(c)));
	}

	private String parseAndConsiderEscapeUntil(char endChar) throws FormatException {
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

	private void parseAuto() throws FormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.auto_can_not_have_pad_value);
		}
		currentList.add(VersionFormat.createAutoFragment(ep, parseQualifier()));
	}

	private void parseBracketGroup() throws FormatException {
		List saveList = currentList;
		currentList = new ArrayList();
		while (current < eos && format.charAt(current) != ']')
			parseFragment();

		if (current == eos)
			throw formatException(NLS.bind(Messages.premature_end_of_format_expected_0, "]")); //$NON-NLS-1$

		++current;
		VersionFormatParser.Instructions ep = parseProcessing();
		saveList.add(VersionFormat.createGroupFragment(ep, ZERO_OR_ONE_QUALIFIER, (VersionFormat.Fragment[]) currentList.toArray(new VersionFormat.Fragment[currentList.size()]), false));
		currentList = saveList;
	}

	private void parseCharacterGroup(VersionFormatParser.Instructions ep) throws FormatException {
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

	private void parseDelimiter() throws FormatException {
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
		currentList.add(VersionFormat.createDelimiterFragment(ep, parseQualifier()));
	}

	private void parseFragment() throws FormatException {
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

	private void parseGroup(boolean array) throws FormatException {
		List saveList = currentList;
		currentList = new ArrayList();
		char expectedEnd = array ? '>' : ')';
		while (current < eos && format.charAt(current) != expectedEnd)
			parseFragment();
		assertChar(expectedEnd);

		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.characters != null)
				throw formatException(Messages.array_can_not_have_character_group);
			if (ep.rangeMax != Integer.MAX_VALUE && ep.padValue != null) {
				throw formatException(Messages.cannot_combine_range_upper_bound_with_pad_value);
			}
		}

		if (currentList.isEmpty())
			throw formatException(array ? Messages.array_can_not_be_empty : Messages.group_can_not_be_empty);
		saveList.add(VersionFormat.createGroupFragment(ep, parseQualifier(), (VersionFormat.Fragment[]) currentList.toArray(new VersionFormat.Fragment[currentList.size()]), array));
		currentList = saveList;
	}

	private int parseIntegerLiteral() throws FormatException {
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

	private void parseLiteral(char c) throws FormatException {
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
		currentList.add(VersionFormat.createLiteralFragment(parseQualifier(), value));
	}

	private int[] parseMinMax() throws FormatException {

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

	private void parseNumber(boolean signed) throws FormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.number_can_not_have_pad_value);
		}
		currentList.add(VersionFormat.createNumberFragment(ep, parseQualifier(), signed));
	}

	private void parsePad() throws FormatException {
		currentList.add(VersionFormat.createPadFragment(parseQualifier()));
	}

	private VersionFormatParser.Instructions parseProcessing() throws FormatException {
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

	private void parseProcessingInstruction(VersionFormatParser.Instructions processing) throws FormatException {
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
			// =<raw-element>;
			if (processing.defaultValue != null)
				throw formatException(Messages.default_defined_more_then_once);
			if (processing.ignore)
				throw formatException(Messages.cannot_combine_ignore_with_other_instruction);
			processing.defaultValue = parseRawElement();
		}
		assertChar(';');
	}

	private VersionFormat.Qualifier parseQualifier() throws FormatException {
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
		return new VersionFormat.Qualifier(min, max);
	}

	private void parseQuotedString() throws FormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.string_can_not_have_pad_value);
		}
		currentList.add(VersionFormat.createQuotedFragment(ep, parseQualifier()));
	}

	private void parseRaw() throws FormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.raw_element_can_not_have_pad_value);
		}
		currentList.add(VersionFormat.createRawFragment(ep, parseQualifier()));
	}

	private Comparable parseRawElement() throws FormatException {
		int[] position = new int[] {current};
		Comparable v = VersionParser.parseRawElement(format, position, eos);
		if (v == null)
			throw new FormatException(NLS.bind(Messages.raw_element_expected_0, format));
		current = position[0];
		return v;
	}

	private void parseString(boolean unlimited) throws FormatException {
		VersionFormatParser.Instructions ep = parseProcessing();
		if (ep != null) {
			if (ep.padValue != null)
				throw formatException(Messages.string_can_not_have_pad_value);
		}
		currentList.add(VersionFormat.createStringFragment(ep, parseQualifier(), unlimited));
	}
}
