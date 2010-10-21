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
package org.eclipse.equinox.internal.p2.metadata;

import java.io.Serializable;
import java.util.*;

/**
 * Instances of this class represents the enum version format.
 */
class EnumDefinition implements Comparable<EnumDefinition>, Serializable {
	static class EnumSegment implements Comparable<EnumSegment>, Serializable {
		private static final long serialVersionUID = 4737907767214436543L;

		private final int ordinal;
		private final EnumDefinition definition;

		EnumSegment(int ordinal, EnumDefinition definition) {
			this.ordinal = ordinal;
			this.definition = definition;
		}

		public int compareTo(EnumSegment other) {
			if (other == this)
				return 0;
			if (definition == other.definition)
				// Same definition. Just compare ordinals
				return ordinal - other.ordinal;

			String thisId = definition.getIdentifier(ordinal);
			String otherId = other.definition.getIdentifier(other.ordinal);
			if (thisId.equals(otherId))
				return 0;

			int thisOrdinalInOther = other.definition.getOrdinal(thisId);
			int otherOrdinalInThis = definition.getOrdinal(otherId);
			if (thisOrdinalInOther >= 0) {
				if (otherOrdinalInThis >= 0) {
					// Both identifiers exists in both enums. Let's see if both
					// enums order them the same way
					int thisOrder = ordinal - otherOrdinalInThis;
					int otherOrder = thisOrdinalInOther - other.ordinal;
					if (thisOrder > 0 && otherOrder > 0)
						return 1;
					if (thisOrder < 0 && otherOrder < 0)
						return -1;
					// Difference in opinion...
				} else {
					// Use the order in other since it has both identifiers and
					// this enum does not
					return thisOrdinalInOther - other.ordinal;
				}
			} else if (otherOrdinalInThis >= 0) {
				// Use the order in this since it has both identifiers and the
				// other does not.
				return ordinal - otherOrdinalInThis;
			}

			// We can't compare the enums since neither enum contain both identifiers
			// or both do, but use different order. Fall back to comparing the definitions
			return definition.compareTo(other.definition);
		}

		public boolean equals(Object other) {
			return other == this || other instanceof EnumSegment && compareTo((EnumSegment) other) == 0;
		}

		public int hashCode() {
			return (1 + ordinal) * 31 + definition.getIdentifier(ordinal).hashCode();
		}

		int getOrdinal() {
			return ordinal;
		}

		String getIdentifier() {
			return definition.getIdentifier(ordinal);
		}

		void toString(StringBuffer sb) {
			definition.toString(sb, ordinal);
		}

		// For ligthweight deserialization
		private Object readResolve() {
			return definition.getSegment(ordinal);
		}
	}

	private static final long serialVersionUID = 7237775466362654473L;
	private static final Map<EnumDefinition, EnumSegment[]> enumDefinitionCache = new HashMap<EnumDefinition, EnumSegment[]>();

	private static EnumSegment[] getEnumSegments(EnumDefinition ed) {
		EnumSegment[] values = enumDefinitionCache.get(ed);
		if (values == null) {
			int ordinal = ed.identifiers.length;
			values = new EnumSegment[ordinal];
			while (--ordinal >= 0)
				values[ordinal] = new EnumSegment(ordinal, ed);
			enumDefinitionCache.put(ed, values);
		}
		return values;
	}

	static EnumDefinition getEnumDefinition(List<List<String>> identifiers) {
		nextEd: for (EnumDefinition ed : enumDefinitionCache.keySet()) {
			String[][] defs = ed.identifiers;
			int ordinal = defs.length;
			if (ordinal != identifiers.size())
				continue;

			while (--ordinal >= 0) {
				String[] def = defs[ordinal];
				List<String> ldef = identifiers.get(ordinal);
				int idx = def.length;
				if (ldef.size() != idx)
					continue nextEd;
				while (--idx >= 0)
					if (!def[idx].equals(ldef.get(idx)))
						continue nextEd;
			}
			return ed;
		}
		EnumDefinition ed = new EnumDefinition(identifiers);
		getEnumSegments(ed);
		return ed;
	}

	private final String[][] identifiers;
	private final int longestLength;
	private final int shortestLength;

	private EnumDefinition(List<List<String>> identifiers) {
		int ordinal = identifiers.size();
		String[][] defs = new String[ordinal][];
		int minLen = Integer.MAX_VALUE;
		int maxLen = 0;
		while (--ordinal >= 0) {
			List<String> idents = identifiers.get(ordinal);
			int idx = idents.size();
			String[] def = idents.toArray(new String[idx]);
			defs[ordinal] = def;
			while (--idx >= 0) {
				int idLen = def[idx].length();
				if (idLen < minLen)
					minLen = idLen;
				if (idLen > maxLen)
					maxLen = idLen;
			}
		}
		this.shortestLength = minLen;
		this.longestLength = maxLen;
		this.identifiers = defs;
	}

	static EnumSegment getSegment(List<List<String>> identifiers, int ordinal) {
		return new EnumDefinition(identifiers).getSegment(ordinal);
	}

	EnumSegment getSegment(int ordinal) {
		return getEnumSegments(this)[ordinal];
	}

	/**
	 * Returns the ordinal for the given identifier
	 * @param identifier The identifier
	 * @return The ordinal of the identifier or -1 if it didn't exist.
	 */
	int getOrdinal(String identifier) {
		if (identifier != null) {
			int ordinal = identifiers.length;
			while (--ordinal >= 0) {
				String[] idents = identifiers[ordinal];
				int idx = idents.length;
				while (--idx >= 0)
					if (idents[idx].equals(identifier))
						return ordinal;
			}
		}
		return -1;
	}

	/**
	 * Returns the canonical identifier for the given ordinal.
	 * @param ordinal The ordinal number of the desired identifier
	 * @return The identifier or <code>null</code> if the ordinal is out of bounds
	 */
	String getIdentifier(int ordinal) {
		return ordinal >= 0 && ordinal < identifiers.length ? identifiers[ordinal][0] : null;
	}

	@Override
	public int hashCode() {
		int result = 1;
		int ordinal = identifiers.length;
		while (--ordinal > 0) {
			String[] idents = identifiers[ordinal];
			int idx = idents.length;
			while (--idx >= 0)
				result = 31 * result + idents[idx].hashCode();
		}
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof EnumDefinition))
			return false;
		String[][] otherIds = ((EnumDefinition) o).identifiers;
		int ordinal = identifiers.length;
		if (ordinal != otherIds.length)
			return false;
		while (--ordinal >= 0)
			if (!Arrays.equals(identifiers[ordinal], otherIds[ordinal]))
				return false;
		return true;
	}

	public int compareTo(EnumDefinition o) {
		if (o == this)
			return 0;

		int top = identifiers.length;
		int cmp = top - o.identifiers.length;
		if (cmp != 0)
			return cmp;

		for (int idx = 0; idx < top; ++idx) {
			cmp = identifiers[idx][0].compareTo(o.identifiers[idx][0]);
			if (cmp != 0)
				return cmp;
		}
		// this should never happen since we use a lightweight pattern
		return 0;
	}

	@Override
	public String toString() {
		StringBuffer bld = new StringBuffer();
		toString(bld);
		return bld.toString();
	}

	public void toString(StringBuffer bld) {
		bld.append('{');
		int top = identifiers.length;
		for (int ordinal = 0;;) {
			String[] idents = identifiers[ordinal];
			int identsTop = idents.length;
			bld.append(idents[0]);
			for (int idx = 1; idx < identsTop; ++idx) {
				bld.append('=');
				bld.append(idents[idx]);
			}
			if (++ordinal == top)
				break;
			bld.append(',');
		}
		bld.append('}');
	}

	void toString(StringBuffer bld, int selectedOrdinal) {
		bld.append('{');
		int top = identifiers.length;
		for (int ordinal = 0;;) {
			if (ordinal == selectedOrdinal)
				bld.append('^');
			bld.append(identifiers[ordinal][0]);
			if (++ordinal == top)
				break;
			bld.append(',');
		}
		bld.append('}');
	}

	/**
	 * Returns the length of the longest identifier. This method is
	 * used by the parser
	 * @return The length of the longest identifier
	 */
	int getLongestLength() {
		return longestLength;
	}

	/**
	 * Returns the length of the shortest identifier. This method is
	 * used by the parser
	 * @return The length of the shortest identifier
	 */
	int getShortestLength() {
		return shortestLength;
	}
}