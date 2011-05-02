/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.p2.metadata.MetadataFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;

/**
 * Touchpoint data instances contain the additional information needed by a touchpoint
 * to execute each engine phase it participates in. This includes the sequence of
 * instruction statements to be executed during each phase, and any additional
 * supporting data needed to perform the phase.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @see MetadataFactory#createTouchpointData(Map)
 */
public class TouchpointData implements ITouchpointData {

	/**
	 * Map of (String->TouchpointInstruction). The set
	 * of keys supported is up to the touchpoint that will process these
	 * instructions. This map is never null.
	 */
	private Map<String, ITouchpointInstruction> instructions;

	public int hashCode() {
		return 31 * 1 + ((instructions == null) ? 0 : instructions.hashCode());
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ITouchpointData))
			return false;
		final ITouchpointData other = (ITouchpointData) obj;
		if (instructions == null) {
			if (other.getInstructions() != null)
				return false;
		} else if (!instructions.equals(other.getInstructions()))
			return false;
		return true;
	}

	/**
	 * Clients must use the factory method on {@link MetadataFactory}.
	 */
	public TouchpointData(Map<String, ITouchpointInstruction> instructions) {
		this.instructions = instructions;
	}

	/**
	 * Returns the touchpoint instruction corresponding to the given key.
	 */
	public ITouchpointInstruction getInstruction(String instructionKey) {
		return instructions.get(instructionKey);
	}

	/**
	 * Returns an unmodifiable map of the touchpoint instructions. The map
	 * keys are strings, and the values are instances of {@link ITouchpointInstruction}.
	 *
	 * @return the touchpoint instructions
	 */
	public Map<String, ITouchpointInstruction> getInstructions() {
		return Collections.unmodifiableMap(instructions);
	}

	/**
	 * Returns a string representation of the touchpoint data for debugging purposes only.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Entry<String, ITouchpointInstruction> instruction : instructions.entrySet()) {
			result.append(instruction.getKey()).append(" -> ").append(instruction.getValue()).append('\n'); //$NON-NLS-1$
		}
		return result.toString();
	}
}
