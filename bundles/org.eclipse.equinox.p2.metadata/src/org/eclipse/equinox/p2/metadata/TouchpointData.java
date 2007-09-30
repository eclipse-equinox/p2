/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.Map;

public class TouchpointData {
	public static TouchpointData[] NO_TOUCHPOINT_DATA = new TouchpointData[0];

	/**
	 * Map of (String->String). The values represent the instructions. The set 
	 * of keys supported is up to the touchpoint that will process these 
	 * instructions
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

	public TouchpointData(Map instructions) {
		this.instructions = instructions;
	}

	public String getInstructions(String instructionKey) {
		return (String) instructions.get(instructionKey);
	}
}
