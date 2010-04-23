/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *     G&H Softwareentwicklung GmbH - internationalization implementation (bug 150933)
 *     Cloudsmith Inc. Refactored for more general use with VersionedId
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.updatesite;

import java.util.*;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Refactored from org.eclipse.pde.internal.build.builder.BuildDirector
 */
public class VersionSuffixGenerator {
	public static final String VERSION_QUALIFIER = "qualifier"; //$NON-NLS-1$

	private static final int QUALIFIER_SUFFIX_VERSION = 1;

	// The 64 characters that are legal in a version qualifier, in lexicographical order.
	public static final String BASE_64_ENCODING = "-0123456789_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"; //$NON-NLS-1$

	public static String incrementQualifier(String qualifier) {
		int idx = qualifier.length() - 1;

		for (; idx >= 0; idx--) {
			//finding last non-'z' character
			if (qualifier.charAt(idx) != 'z')
				break;
		}

		if (idx >= 0) {
			// charAt(idx) is < 'z', so don't need to check bounds
			int c = BASE_64_ENCODING.indexOf(qualifier.charAt(idx)) + 1;
			String newQualifier = qualifier.substring(0, idx);
			newQualifier += BASE_64_ENCODING.charAt(c);
			return newQualifier;
		}

		return null;
	}

	private static void appendEncodedCharacter(StringBuffer buffer, int c) {
		while (c > 62) {
			buffer.append('z');
			c -= 63;
		}
		buffer.append(base64Character(c));
	}

	// Integer to character conversion in our base-64 encoding scheme. If the
	// input is out of range, an illegal character will be returned.
	//
	private static char base64Character(int number) {
		return (number < 0 || number > 63) ? ' ' : BASE_64_ENCODING.charAt(number);
	}

	private static int charValue(char c) {
		int index = BASE_64_ENCODING.indexOf(c);
		// The "+ 1" is very intentional. For a blank (or anything else that
		// is not a legal character), we want to return 0. For legal
		// characters, we want to return one greater than their position, so
		// that a blank is correctly distinguished from '-'.
		return index + 1;
	}

	private static int computeNameSum(String name) {
		int sum = 0;
		int top = name.length();
		int lshift = 20;
		for (int idx = 0; idx < top; ++idx) {
			int c = name.charAt(idx) & 0xffff;
			if (c == '.' && lshift > 0)
				lshift -= 4;
			else
				sum += c << lshift;
		}
		return sum;
	}

	private static int getIntSegment(Version v, int segment) {
		int segCount = v.getSegmentCount();
		if (segCount <= segment)
			return 0;
		Object seg = v.getSegment(segment);
		return seg instanceof Integer ? ((Integer) seg).intValue() : 0;
	}

	private static int getMajor(Version v) {
		return getIntSegment(v, 0);
	}

	private static int getMicro(Version v) {
		return getIntSegment(v, 2);
	}

	private static int getMinor(Version v) {
		return getIntSegment(v, 1);
	}

	private static String getQualifier(Version v) {
		int segCount = v.getSegmentCount();
		if (segCount == 0)
			return null;
		Object seg = v.getSegment(segCount - 1);
		return seg instanceof String ? (String) seg : null;
	}

	// Encode a non-negative number as a variable length string, with the
	// property that if X > Y then the encoding of X is lexicographically
	// greater than the enocding of Y. This is accomplished by encoding the
	// length of the string at the beginning of the string. The string is a
	// series of base 64 (6-bit) characters. The first three bits of the first
	// character indicate the number of additional characters in the string.
	// The last three bits of the first character and all of the rest of the
	// characters encode the actual value of the number. Examples:
	// 0 --> 000 000 --> "-"
	// 7 --> 000 111 --> "6"
	// 8 --> 001 000 001000 --> "77"
	// 63 --> 001 000 111111 --> "7z"
	// 64 --> 001 001 000000 --> "8-"
	// 511 --> 001 111 111111 --> "Dz"
	// 512 --> 010 000 001000 000000 --> "E7-"
	// 2^32 - 1 --> 101 011 111111 ... 111111 --> "fzzzzz"
	// 2^45 - 1 --> 111 111 111111 ... 111111 --> "zzzzzzzz"
	// (There are some wasted values in this encoding. For example,
	// "7-" through "76" and "E--" through "E6z" are not legal encodings of
	// any number. But the benefit of filling in those wasted ranges would not
	// be worth the added complexity.)
	private static String lengthPrefixBase64(long number) {
		int length = 7;
		for (int i = 0; i < 7; ++i) {
			if (number < (1L << ((i * 6) + 3))) {
				length = i;
				break;
			}
		}
		StringBuffer result = new StringBuffer(length + 1);
		result.append(base64Character((length << 3) + (int) ((number >> (6 * length)) & 0x7)));
		while (--length >= 0) {
			result.append(base64Character((int) ((number >> (6 * length)) & 0x3f)));
		}
		return result.toString();
	}

	private final int maxVersionSuffixLength;

	private final int significantDigits;

	public VersionSuffixGenerator() {
		this(-1, -1);
	}

	public VersionSuffixGenerator(int maxVersionSuffixLenght, int significantDigits) {
		this.maxVersionSuffixLength = maxVersionSuffixLenght < 0 ? 45 : maxVersionSuffixLenght;
		this.significantDigits = significantDigits < 0 ? Integer.MAX_VALUE : significantDigits;
	}

	/**
	 * Version suffix generation.
	 * @param features A collection of @{link IVersionedId} instances representing the features to include
	 * @param others A list of @{link IVersionedId} instances representing other IUs to include
	 * @return The generated suffix or <code>null</code>
	 */
	public String generateSuffix(Collection<? extends IVersionedId> features, Collection<? extends IVersionedId> others) {
		if (maxVersionSuffixLength <= 0 || (features.isEmpty() && others.isEmpty()))
			return null; // do nothing

		long majorSum = 0L;
		long minorSum = 0L;
		long serviceSum = 0L;
		long nameCharsSum = 0L;

		// Include the version of this algorithm as part of the suffix, so that
		// we have a way to make sure all suffixes increase when the algorithm
		// changes.
		//
		majorSum += QUALIFIER_SUFFIX_VERSION;
		ArrayList<String> qualifiers = new ArrayList<String>();

		// Loop through the included features, adding the version number parts
		// to the running totals and storing the qualifier suffixes.
		//
		Iterator<? extends IVersionedId> itor = features.iterator();
		while (itor.hasNext()) {
			IVersionedId refFeature = itor.next();
			Version version = refFeature.getVersion();
			majorSum += getMajor(version);
			minorSum += getMinor(version);
			serviceSum += getMicro(version);
			qualifiers.add(getQualifier(version));
			nameCharsSum = computeNameSum(refFeature.getId());
		}

		// Loop through the included plug-ins and fragments, adding the version
		// number parts to the running totals and storing the qualifiers.
		//
		itor = features.iterator();
		while (itor.hasNext()) {
			IVersionedId refOther = itor.next();
			Version version = refOther.getVersion();
			majorSum += getMajor(version);
			minorSum += getMinor(version);
			serviceSum += getMicro(version);

			String qualifier = getQualifier(version);
			if (qualifier != null && qualifier.endsWith(VERSION_QUALIFIER)) {
				int resultingLength = qualifier.length() - VERSION_QUALIFIER.length();
				if (resultingLength > 0) {
					if (qualifier.charAt(resultingLength - 1) == '.')
						resultingLength--;
					qualifier = resultingLength > 0 ? qualifier.substring(0, resultingLength) : null;
				} else
					qualifier = null;
			}
			qualifiers.add(qualifier);
		}

		// Limit the qualifiers to the specified number of significant digits,
		// and figure out what the longest qualifier is.
		//
		int longestQualifier = 0;
		int idx = qualifiers.size();
		while (--idx >= 0) {
			String qualifier = qualifiers.get(idx);
			if (qualifier == null)
				continue;

			if (qualifier.length() > significantDigits) {
				qualifier = qualifier.substring(0, significantDigits);
				qualifiers.set(idx, qualifier);
			}
			if (qualifier.length() > longestQualifier)
				longestQualifier = qualifier.length();
		}

		StringBuffer result = new StringBuffer();

		// Encode the sums of the first three parts of the version numbers.
		result.append(lengthPrefixBase64(majorSum));
		result.append(lengthPrefixBase64(minorSum));
		result.append(lengthPrefixBase64(serviceSum));
		result.append(lengthPrefixBase64(nameCharsSum));

		if (longestQualifier > 0) {
			// Calculate the sum at each position of the qualifiers.
			int[] qualifierSums = new int[longestQualifier];
			int top = qualifiers.size();
			for (idx = 0; idx < top; ++idx) {
				String qualifier = qualifiers.get(idx);
				if (qualifier == null)
					continue;

				int qlen = qualifier.length();
				for (int j = 0; j < qlen; ++j)
					qualifierSums[j] += charValue(qualifier.charAt(j));
			}

			// Normalize the sums to be base 65.
			int carry = 0;
			for (int k = longestQualifier - 1; k >= 1; --k) {
				qualifierSums[k] += carry;
				carry = qualifierSums[k] / 65;
				qualifierSums[k] = qualifierSums[k] % 65;
			}
			qualifierSums[0] += carry;

			// Always use one character for overflow. This will be handled
			// correctly even when the overflow character itself overflows.
			result.append(lengthPrefixBase64(qualifierSums[0]));
			for (int m = 1; m < longestQualifier; ++m)
				appendEncodedCharacter(result, qualifierSums[m]);
		}

		// If the resulting suffix is too long, shorten it to the designed length.
		//
		if (result.length() > maxVersionSuffixLength)
			result.setLength(maxVersionSuffixLength);

		// It is safe to strip any '-' characters from the end of the suffix.
		// (This won't happen very often, but it will save us a character or
		// two when it does.)
		//
		int len = result.length();
		while (len > 0 && result.charAt(len - 1) == '-')
			result.setLength(--len);
		return result.toString();
	}
}
