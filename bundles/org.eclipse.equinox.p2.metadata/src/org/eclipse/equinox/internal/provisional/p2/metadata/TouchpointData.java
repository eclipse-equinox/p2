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

public class TouchpointData {

	/**
	 * Map of (String->String). The values represent the instructions. The set 
	 * of keys supported is up to the touchpoint that will process these 
	 * instructions. This map is never null.
	 */
	private Map instructions;

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((instructions == null) ? 0 : instructions.hashCode());
		return result;
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

	TouchpointData(Map instructions) {
		this.instructions = instructions;
	}

	public String getInstructions(String instructionKey) {
		return (String) instructions.get(instructionKey);
	}

	// Return an unmodifiable collection of the instructions
	// in the touchpoint data.
	public Map getInstructions() {
		return Collections.unmodifiableMap(instructions);
	}

	public String toString() {
		return instructions.toString();
	}
}
