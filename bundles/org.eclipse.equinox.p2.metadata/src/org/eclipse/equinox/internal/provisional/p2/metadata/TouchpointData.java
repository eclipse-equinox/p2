/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

import java.util.Collections;
import java.util.Map;

/**
 * Touchpoint data instances contain the additional information needed by a touchpoint
 * to execute each engine phase it participates in. This includes the sequence of
 * instruction statements to be executed during each phase, and any additional 
 * supporting data needed to perform the phase.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @see MetadataFactory#createTouchpointData(Map)
 */
public class TouchpointData {

	/**
	 * Map of (String->TouchpointInstruction). The set 
	 * of keys supported is up to the touchpoint that will process these 
	 * instructions. This map is never null.
	 */
	private Map instructions;

	public int hashCode() {
		return 31 * 1 + ((instructions == null) ? 0 : instructions.hashCode());
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TouchpointData other = (TouchpointData) obj;
		if (instructions == null) {
			if (other.instructions != null)
				return false;
		} else if (!instructions.equals(other.instructions))
			return false;
		return true;
	}

	/**
	 * Clients must use the factory method on {@link MetadataFactory}.
	 */
	TouchpointData(Map instructions) {
		this.instructions = instructions;
	}

	/**
	 * Returns the touchpoint instruction corresponding to the given key.
	 */
	public TouchpointInstruction getInstruction(String instructionKey) {
		return (TouchpointInstruction) instructions.get(instructionKey);
	}

	/**
	 * Returns an unmodifiable map of the touchpoint instructions. The map
	 * keys are strings, and the values are instances of {@link TouchpointInstruction}.
	 * 
	 * @return the touchpoint instructions
	 */
	public Map getInstructions() {
		return Collections.unmodifiableMap(instructions);
	}

	/**
	 * Returns a string representation of the touchpoint data for debugging purposes only.
	 */
	public String toString() {
		return instructions.toString();
	}
}
