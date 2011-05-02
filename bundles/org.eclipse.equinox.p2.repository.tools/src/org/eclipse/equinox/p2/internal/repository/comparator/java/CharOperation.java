/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.comparator.java;

public final class CharOperation {
	public static final char[] This = "this".toCharArray(); //$NON-NLS-1$

	public static final char[] JAVA_LANG_ANNOTATION_DOCUMENTED = "Ljava/lang/annotation/Documented;".toCharArray(); //$NON-NLS-1$
	public static final char[] JAVA_LANG_ANNOTATION_ELEMENTTYPE = "Ljava/lang/annotation/ElementType;".toCharArray(); //$NON-NLS-1$
	public static final char[] JAVA_LANG_ANNOTATION_RETENTION = "Ljava/lang/annotation/Retention;".toCharArray(); //$NON-NLS-1$
	public static final char[] JAVA_LANG_ANNOTATION_RETENTIONPOLICY = "Ljava/lang/annotation/RetentionPolicy;".toCharArray(); //$NON-NLS-1$
	public static final char[] JAVA_LANG_ANNOTATION_TARGET = "Ljava/lang/annotation/Target;".toCharArray(); //$NON-NLS-1$
	public static final char[] JAVA_LANG_DEPRECATED = "Ljava/lang/Deprecated;".toCharArray(); //$NON-NLS-1$
	public static final char[] JAVA_LANG_ANNOTATION_INHERITED = "Ljava/lang/annotation/Inherited;".toCharArray(); //$NON-NLS-1$
	/**
	 * Constant for an empty char array
	 */
	public static final char[] NO_CHAR = new char[0];

	/**
	 * Constant for an empty char array with two dimensions.
	 */
	public static final char[][] NO_CHAR_CHAR = new char[0][];

	/**
	 * Answers a hashcode for the array
	 *
	 * @param array the array for which a hashcode is required
	 * @return the hashcode
	 * @throws NullPointerException if array is null
	 */
	public static final int hashCode(char[] array) {
		int length = array.length;
		int hash = length == 0 ? 31 : array[0];
		if (length < 8) {
			for (int i = length; --i > 0;)
				hash = (hash * 31) + array[i];
		} else {
			// 8 characters is enough to compute a decent hash code, don't waste time examining every character
			for (int i = length - 1, last = i > 16 ? i - 16 : 0; i > last; i -= 2)
				hash = (hash * 31) + array[i];
		}
		return hash & 0x7FFFFFFF;
	}

	/**
	 * Answers the last index in the array for which the corresponding character is
	 * equal to toBeFound starting from the end of the array.
	 * Answers -1 if no occurrence of this character is found.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    toBeFound = 'c'
	 *    array = { ' a', 'b', 'c', 'd' , 'c', 'e' }
	 *    result => 4
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    toBeFound = 'e'
	 *    array = { ' a', 'b', 'c', 'd' }
	 *    result => -1
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param toBeFound the character to search
	 * @param array the array to be searched
	 * @return the last index in the array for which the corresponding character is
	 * equal to toBeFound starting from the end of the array, -1 otherwise
	 * @throws NullPointerException if array is null
	 */
	public static final int lastIndexOf(char toBeFound, char[] array) {
		for (int i = array.length; --i >= 0;)
			if (toBeFound == array[i])
				return i;
		return -1;
	}

	/**
	 * Return a new array which is the split of the given array using the given divider.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    divider = 'b'
	 *    array = { 'a' , 'b', 'b', 'a', 'b', 'a' }
	 *    result => { { 'a' }, {  }, { 'a' }, { 'a' } }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    divider = 'c'
	 *    array = { 'a' , 'b', 'b', 'a', 'b', 'a' }
	 *    result => { { 'a', 'b', 'b', 'a', 'b', 'a' } }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    divider = 'c'
	 *    array = { ' ', ' ', 'a' , 'b', 'b', 'a', 'b', 'a', ' ' }
	 *    result => { { ' ', 'a', 'b', 'b', 'a', 'b', 'a', ' ' } }
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param divider the given divider
	 * @param array the given array
	 * @return a new array which is the split of the given array using the given divider
	 */
	public static final char[][] splitOn(char divider, char[] array) {
		int length = array == null ? 0 : array.length;
		if (length == 0)
			return NO_CHAR_CHAR;

		int wordCount = 1;
		for (int i = 0; i < length; i++)
			if (array[i] == divider)
				wordCount++;
		char[][] split = new char[wordCount][];
		int last = 0, currentWord = 0;
		for (int i = 0; i < length; i++) {
			if (array[i] == divider) {
				split[currentWord] = new char[i - last];
				System.arraycopy(array, last, split[currentWord++], 0, i - last);
				last = i + 1;
			}
		}
		split[currentWord] = new char[length - last];
		System.arraycopy(array, last, split[currentWord], 0, length - last);
		return split;
	}

	/**
	 * Answers the first index in the array for which the corresponding character is
	 * equal to toBeFound starting the search at index start.
	 * Answers -1 if no occurrence of this character is found.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    toBeFound = 'c'
	 *    array = { ' a', 'b', 'c', 'd' }
	 *    start = 2
	 *    result => 2
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    toBeFound = 'c'
	 *    array = { ' a', 'b', 'c', 'd' }
	 *    start = 3
	 *    result => -1
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    toBeFound = 'e'
	 *    array = { ' a', 'b', 'c', 'd' }
	 *    start = 1
	 *    result => -1
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param toBeFound the character to search
	 * @param array the array to be searched
	 * @param start the starting index
	 * @return the first index in the array for which the corresponding character is
	 * equal to toBeFound, -1 otherwise
	 * @throws NullPointerException if array is null
	 * @throws ArrayIndexOutOfBoundsException if  start is lower than 0
	 */
	public static final int indexOf(char toBeFound, char[] array, int start) {
		for (int i = start; i < array.length; i++)
			if (toBeFound == array[i])
				return i;
		return -1;
	}

	/**
	 * Answers a new array with prepending the prefix character and appending the suffix
	 * character at the end of the array. If array is null, it answers a new array containing the
	 * prefix and the suffix characters.
	 * <br>
	 * <br>
	 * For example:<br>
	 * <ol>
	 * <li><pre>
	 *    prefix = 'a'
	 *    array = { 'b' }
	 *    suffix = 'c'
	 *    => result = { 'a', 'b' , 'c' }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    prefix = 'a'
	 *    array = null
	 *    suffix = 'c'
	 *    => result = { 'a', 'c' }
	 * </pre></li>
	 * </ol>
	 *
	 * @param prefix the prefix character
	 * @param array the array that is concatenated with the prefix and suffix characters
	 * @param suffix the suffix character
	 * @return the new array
	 */
	public static final char[] concat(char prefix, char[] array, char suffix) {
		if (array == null)
			return new char[] {prefix, suffix};

		int length = array.length;
		char[] result = new char[length + 2];
		result[0] = prefix;
		System.arraycopy(array, 0, result, 1, length);
		result[length + 1] = suffix;
		return result;
	}

	/**
	 * Answers the concatenation of the three arrays. It answers null if the three arrays are null.
	 * If first is null, it answers the concatenation of second and third.
	 * If second is null, it answers the concatenation of first and third.
	 * If third is null, it answers the concatenation of first and second.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    first = null
	 *    second = { 'a' }
	 *    third = { 'b' }
	 *    => result = { ' a', 'b' }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    first = { 'a' }
	 *    second = null
	 *    third = { 'b' }
	 *    => result = { ' a', 'b' }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    first = { 'a' }
	 *    second = { 'b' }
	 *    third = null
	 *    => result = { ' a', 'b' }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    first = null
	 *    second = null
	 *    third = null
	 *    => result = null
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    first = { 'a' }
	 *    second = { 'b' }
	 *    third = { 'c' }
	 *    => result = { 'a', 'b', 'c' }
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param first the first array to concatenate
	 * @param second the second array to concatenate
	 * @param third the third array to concatenate
	 *
	 * @return the concatenation of the three arrays, or null if the three arrays are null.
	 */
	public static final char[] concat(char[] first, char[] second, char[] third) {
		if (first == null)
			return concat(second, third);
		if (second == null)
			return concat(first, third);
		if (third == null)
			return concat(first, second);

		int length1 = first.length;
		int length2 = second.length;
		int length3 = third.length;
		char[] result = new char[length1 + length2 + length3];
		System.arraycopy(first, 0, result, 0, length1);
		System.arraycopy(second, 0, result, length1, length2);
		System.arraycopy(third, 0, result, length1 + length2, length3);
		return result;
	}

	/**
	 * Answers the concatenation of the two arrays. It answers null if the two arrays are null.
	 * If the first array is null, then the second array is returned.
	 * If the second array is null, then the first array is returned.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    first = null
	 *    second = { 'a' }
	 *    => result = { ' a' }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    first = { ' a' }
	 *    second = null
	 *    => result = { ' a' }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    first = { ' a' }
	 *    second = { ' b' }
	 *    => result = { ' a' , ' b' }
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param first the first array to concatenate
	 * @param second the second array to concatenate
	 * @return the concatenation of the two arrays, or null if the two arrays are null.
	 */
	public static final char[] concat(char[] first, char[] second) {
		if (first == null)
			return second;
		if (second == null)
			return first;

		int length1 = first.length;
		int length2 = second.length;
		char[] result = new char[length1 + length2];
		System.arraycopy(first, 0, result, 0, length1);
		System.arraycopy(second, 0, result, length1, length2);
		return result;
	}

	/**
	 * Replace all occurrence of the character to be replaced with the replacement character in the
	 * given array.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    array = { 'a' , 'b', 'b', 'a', 'b', 'a' }
	 *    toBeReplaced = 'b'
	 *    replacementChar = 'a'
	 *    result => No returned value, but array is now equals to { 'a' , 'a', 'a', 'a', 'a', 'a' }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    array = { 'a' , 'b', 'b', 'a', 'b', 'a' }
	 *    toBeReplaced = 'c'
	 *    replacementChar = 'a'
	 *    result => No returned value, but array is now equals to { 'a' , 'b', 'b', 'a', 'b', 'a' }
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param array the given array
	 * @param toBeReplaced the character to be replaced
	 * @param replacementChar the replacement character
	 * @throws NullPointerException if the given array is null
	 */
	public static final void replace(char[] array, char toBeReplaced, char replacementChar) {
		if (toBeReplaced != replacementChar) {
			for (int i = 0, max = array.length; i < max; i++) {
				if (array[i] == toBeReplaced)
					array[i] = replacementChar;
			}
		}
	}

	/**
	 * Replace all occurrence of the character to be replaced with the replacement character
	 * in a copy of the given array. Returns the given array if no occurrences of the character
	 * to be replaced are found.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    array = { 'a' , 'b', 'b', 'a', 'b', 'a' }
	 *    toBeReplaced = 'b'
	 *    replacementChar = 'a'
	 *    result => A new array that is equals to { 'a' , 'a', 'a', 'a', 'a', 'a' }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    array = { 'a' , 'b', 'b', 'a', 'b', 'a' }
	 *    toBeReplaced = 'c'
	 *    replacementChar = 'a'
	 *    result => The original array that remains unchanged.
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param array the given array
	 * @param toBeReplaced the character to be replaced
	 * @param replacementChar the replacement character
	 * @throws NullPointerException if the given array is null
	 * @since 3.1
	 */
	public static final char[] replaceOnCopy(char[] array, char toBeReplaced, char replacementChar) {

		char[] result = null;
		for (int i = 0, length = array.length; i < length; i++) {
			char c = array[i];
			if (c == toBeReplaced) {
				if (result == null) {
					result = new char[length];
					System.arraycopy(array, 0, result, 0, i);
				}
				result[i] = replacementChar;
			} else if (result != null) {
				result[i] = c;
			}
		}
		if (result == null)
			return array;
		return result;
	}

	/**
	 * Answers the first index in the array for which the corresponding character is
	 * equal to toBeFound. Answers -1 if no occurrence of this character is found.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    toBeFound = 'c'
	 *    array = { ' a', 'b', 'c', 'd' }
	 *    result => 2
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    toBeFound = 'e'
	 *    array = { ' a', 'b', 'c', 'd' }
	 *    result => -1
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param toBeFound the character to search
	 * @param array the array to be searched
	 * @return the first index in the array for which the corresponding character is
	 * equal to toBeFound, -1 otherwise
	 * @throws NullPointerException if array is null
	 */
	public static final int indexOf(char toBeFound, char[] array) {
		return indexOf(toBeFound, array, 0);
	}

	/**
	 * Answers a new array which is a copy of the given array starting at the given start and
	 * ending at the given end. The given start is inclusive and the given end is exclusive.
	 * Answers null if start is greater than end, if start is lower than 0 or if end is greater
	 * than the length of the given array. If end  equals -1, it is converted to the array length.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    array = { 'a' , 'b' }
	 *    start = 0
	 *    end = 1
	 *    result => { 'a' }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    array = { 'a', 'b' }
	 *    start = 0
	 *    end = -1
	 *    result => { 'a' , 'b' }
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param array the given array
	 * @param start the given starting index
	 * @param end the given ending index
	 * @return a new array which is a copy of the given array starting at the given start and
	 * ending at the given end
	 * @throws NullPointerException if the given array is null
	 */
	public static final char[] subarray(char[] array, int start, int end) {
		if (end == -1)
			end = array.length;
		if (start > end)
			return null;
		if (start < 0)
			return null;
		if (end > array.length)
			return null;

		char[] result = new char[end - start];
		System.arraycopy(array, start, result, 0, end - start);
		return result;
	}

	/**
	 * Answers a new array which is a copy of the given array starting at the given start and
	 * ending at the given end. The given start is inclusive and the given end is exclusive.
	 * Answers null if start is greater than end, if start is lower than 0 or if end is greater
	 * than the length of the given array. If end  equals -1, it is converted to the array length.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    array = { { 'a' } , { 'b' } }
	 *    start = 0
	 *    end = 1
	 *    result => { { 'a' } }
	 * </pre>
	 * </li>
	 * <li><pre>
	 *    array = { { 'a' } , { 'b' } }
	 *    start = 0
	 *    end = -1
	 *    result => { { 'a' }, { 'b' } }
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param array the given array
	 * @param start the given starting index
	 * @param end the given ending index
	 * @return a new array which is a copy of the given array starting at the given start and
	 * ending at the given end
	 * @throws NullPointerException if the given array is null
	 */
	public static final char[][] subarray(char[][] array, int start, int end) {
		if (end == -1)
			end = array.length;
		if (start > end)
			return null;
		if (start < 0)
			return null;
		if (end > array.length)
			return null;

		char[][] result = new char[end - start][];
		System.arraycopy(array, start, result, 0, end - start);
		return result;
	}

	/**
	 * Return a new array which is the split of the given array using the given divider. The given end
	 * is exclusive and the given start is inclusive.
	 * <br>
	 * <br>
	 * For example:
	 * <ol>
	 * <li><pre>
	 *    divider = 'b'
	 *    array = { 'a' , 'b', 'b', 'a', 'b', 'a' }
	 *    start = 2
	 *    end = 5
	 *    result => { {  }, { 'a' }, {  } }
	 * </pre>
	 * </li>
	 * </ol>
	 *
	 * @param divider the given divider
	 * @param array the given array
	 * @param start the given starting index
	 * @param end the given ending index
	 * @return a new array which is the split of the given array using the given divider
	 * @throws ArrayIndexOutOfBoundsException if start is lower than 0 or end is greater than the array length
	 */
	public static final char[][] splitOn(char divider, char[] array, int start, int end) {
		int length = array == null ? 0 : array.length;
		if (length == 0 || start > end)
			return NO_CHAR_CHAR;

		int wordCount = 1;
		for (int i = start; i < end; i++)
			if (array[i] == divider)
				wordCount++;
		char[][] split = new char[wordCount][];
		int last = start, currentWord = 0;
		for (int i = start; i < end; i++) {
			if (array[i] == divider) {
				split[currentWord] = new char[i - last];
				System.arraycopy(array, last, split[currentWord++], 0, i - last);
				last = i + 1;
			}
		}
		split[currentWord] = new char[end - last];
		System.arraycopy(array, last, split[currentWord], 0, end - last);
		return split;
	}
}
