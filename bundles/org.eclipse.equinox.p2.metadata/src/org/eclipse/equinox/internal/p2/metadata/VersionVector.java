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

import java.io.Serializable;

/**
 * The VersionVector represents an array of Comparable objects. The array can be
 * nested since a VersionVector is Comparable in itself.
 *  
 * @Immutable
 */
public class VersionVector implements Comparable<VersionVector>, Serializable {

	interface MinMaxComparable extends Comparable<Object>, Serializable {
		//
	}

	private static final class MaxStringValue implements MinMaxComparable {
		private static final long serialVersionUID = -4936252230441132767L;

		MaxStringValue() {
			// Empty constructor
		}

		public int compareTo(Object o) {
			return o == this ? 0 : (o == MAX_VALUE || o instanceof Integer || o instanceof EnumDefinition.EnumSegment || o instanceof VersionVector ? -1 : 1);
		}

		// For singleton deserialization
		private Object readResolve() {
			return MAXS_VALUE;
		}

		public String toString() {
			return "m"; //$NON-NLS-1$
		}
	}

	private static final class MaxValue implements MinMaxComparable {
		private static final long serialVersionUID = -5889641741635253589L;

		MaxValue() {
			// Empty constructor
		}

		public int compareTo(Object o) {
			return o == this ? 0 : 1;
		}

		// For singleton deserialization
		private Object readResolve() {
			return MAX_VALUE;
		}

		public String toString() {
			return "M"; //$NON-NLS-1$
		}
	}

	private static class MinValue implements MinMaxComparable {
		private static final long serialVersionUID = -1066323980049812226L;

		MinValue() {
			// Empty constructor
		}

		public int compareTo(Object o) {
			return o == this ? 0 : -1;
		}

		private Object readResolve() {
			return MIN_VALUE;
		}

		public String toString() {
			return "-M"; //$NON-NLS-1$
		}
	}

	/**
	 * A value that is greater then any other value
	 */
	public static final Comparable<Object> MAX_VALUE = new MaxValue();

	/**
	 * A value that is greater then any string but less then {@link #MAX_VALUE} and
	 * any Integer or VersionVector.
	 */
	public static final Comparable<Object> MAXS_VALUE = new MaxStringValue();

	/**
	 * A value that is less then any other value
	 */
	public static final Comparable<Object> MIN_VALUE = new MinValue();

	/**
	 * A value that is greater then {@link #MIN_VALUE} and less then any string,
	 * Integer, or VersionVector (a.k.a. empty_string)
	 */
	public static final String MINS_VALUE = ""; //$NON-NLS-1$

	private static final long serialVersionUID = -8385373304298723744L;

	static int compare(Comparable<?>[] vectorA, Comparable<?> padA, Comparable<?>[] vectorB, Comparable<?> padB) {
		int top = vectorA.length;
		if (top > vectorB.length)
			top = vectorB.length;

		for (int idx = 0; idx < top; ++idx) {
			int cmp = compareSegments(vectorA[idx], vectorB[idx]);
			if (cmp != 0)
				return cmp;
		}

		// All elements compared equal up to this point. Check
		// pad values
		if (top < vectorA.length)
			return (padB == null) ? 1 : compareReminder(top, vectorA, padA, padB);

		if (top < vectorB.length)
			return (padA == null) ? -1 : -compareReminder(top, vectorB, padB, padA);

		// Lengths are equal. Compare pad values
		return padA == null ? (padB == null ? 0 : -1) : (padB == null ? 1 : compareSegments(padA, padB));
	}

	static boolean equals(Comparable<?>[] vectorA, Comparable<?> padValueA, Comparable<?>[] vectorB, Comparable<?> padValueB) {
		// We compare pad first since it is impossible for versions with
		// different pad to be equal (versions are padded to infinity) 
		if (padValueA == null) {
			if (padValueB != null)
				return false;
		} else {
			if (padValueB == null || !padValueA.equals(padValueB))
				return false;
		}

		int idx = vectorA.length;

		// If the length of the vector differs, the versions cannot be equal
		// since segments equal to pad are stripped by the parser
		if (idx != vectorB.length)
			return false;

		while (--idx >= 0)
			if (!vectorA[idx].equals(vectorB[idx]))
				return false;

		return true;
	}

	static int hashCode(Comparable<?>[] vector, Comparable<?> padValue) {
		int hashCode = padValue == null ? 31 : padValue.hashCode();
		int idx = vector.length;
		while (--idx >= 0) {
			Object elem = vector[idx];
			if (elem != null)
				hashCode += elem.hashCode();
			hashCode = hashCode * 31;
		}
		return hashCode;
	}

	static void toString(StringBuffer sb, Comparable<?>[] vector, Comparable<?> padValue, boolean rangeSafe) {
		int top = vector.length;
		if (top == 0)
			// Write one pad value as explicit. It will be considered
			// redundant and removed by the parser but the raw format
			// does not allow zero elements
			VersionFormat.rawToString(sb, rangeSafe, padValue == null ? MIN_VALUE : padValue);
		else {
			for (int idx = 0; idx < top; ++idx) {
				if (idx > 0)
					sb.append('.');
				VersionFormat.rawToString(sb, rangeSafe, vector[idx]);
			}
		}
		if (padValue != null) {
			sb.append('p');
			VersionFormat.rawToString(sb, rangeSafe, padValue);
		}
	}

	private static int compareReminder(int idx, Comparable<?>[] vector, Comparable<?> padValue, Comparable<?> othersPad) {
		int cmp;
		for (cmp = 0; idx < vector.length && cmp == 0; ++idx)
			cmp = compareSegments(vector[idx], othersPad);
		if (cmp == 0)
			cmp = (padValue == null) ? -1 : compareSegments(padValue, othersPad);
		return cmp;
	}

	static int compareSegments(Comparable<?> a, Comparable<?> b) {
		if (a == b)
			return 0;

		if (a instanceof Integer && b instanceof Integer) {
			int ai = ((Integer) a).intValue();
			int bi = ((Integer) b).intValue();
			return ai > bi ? 1 : (ai < bi ? -1 : 0);
		}

		if (a instanceof String && b instanceof String)
			return ((String) a).compareTo((String) b);

		if (a == MAX_VALUE || a == MIN_VALUE || a == MAXS_VALUE)
			return ((MinMaxComparable) a).compareTo(b);

		if (b == MAX_VALUE || b == MIN_VALUE || b == MAXS_VALUE)
			return -((MinMaxComparable) b).compareTo(a);

		if (a instanceof Integer)
			return 1;
		if (b instanceof Integer)
			return -1;

		if (a instanceof VersionVector)
			return (b instanceof VersionVector) ? ((VersionVector) a).compareTo((VersionVector) b) : 1;
		if (b instanceof VersionVector)
			return -1;

		if (a instanceof EnumDefinition.EnumSegment)
			return (b instanceof EnumDefinition.EnumSegment) ? ((EnumDefinition.EnumSegment) a).compareTo((EnumDefinition.EnumSegment) b) : 1;
		if (b instanceof EnumDefinition.EnumSegment)
			return -1;

		throw new IllegalArgumentException();
	}

	private final Comparable<?> padValue;

	private final Comparable<?>[] vector;

	public VersionVector(Comparable<?>[] vector, Comparable<?> pad) {
		this.vector = vector;
		this.padValue = (pad == MIN_VALUE) ? null : pad;
	}

	public int compareTo(VersionVector ov) {
		if (ov == this)
			return 0;

		return compare(vector, padValue, ov.vector, ov.padValue);
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof VersionVector))
			return false;

		VersionVector ov = (VersionVector) o;
		return equals(vector, padValue, ov.vector, ov.padValue);
	}

	/**
	 * Returns the pad value used when comparing this versions to
	 * versions that has a raw vector with a larger number of elements
	 * @return The pad value or <code>null</code> if not set.
	 */
	public Comparable<?> getPad() {
		return padValue;
	}

	/**
	 * An element from the raw vector
	 * @param index The zero based index of the desired element
	 * @return An element from the raw vector
	 */
	public Comparable<?> getSegment(int index) {
		return vector[index];
	}

	/**
	 * Returns the number of elements in the raw vector
	 * @return The element count
	 */
	public int getSegmentCount() {
		return vector.length;
	}

	/**
	 * This method is package protected since it violates the immutable
	 * contract.
	 * @return The raw vector. Must be treated as read-only
	 */
	Comparable<?>[] getVector() {
		return vector;
	}

	public int hashCode() {
		return hashCode(vector, padValue);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		toString(sb);
		return sb.toString();
	}

	/**
	 * Append the string representation of this instance to the
	 * <code>sb</code> buffer.
	 * @param sb The buffer to append to
	 */
	public void toString(StringBuffer sb) {
		toString(sb, vector, padValue, false);
	}

	/**
	 * Append the string representation of this instance to the
	 * <code>sb</code> buffer.
	 * @param sb The buffer to append to
	 * @param rangeSafe If <code>true</code>, the range delimiters will be escaped
	 * with backslash.
	 */
	void toString(StringBuffer sb, boolean rangeSafe) {
		toString(sb, vector, padValue, rangeSafe);
	}

	private Object readResolve() {
		VersionVector vv = this;
		// Preserve the emptyString singleton
		int idx = vector.length;
		while (--idx >= 0)
			if (MINS_VALUE.equals(vector[idx]))
				vector[idx] = MINS_VALUE;
		if (MINS_VALUE.equals(padValue))
			vv = new VersionVector(vector, MINS_VALUE);
		return vv;
	}
}
