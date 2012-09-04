/******************************************************************************* 
* Copyright (c) 2008, 2012 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import java.util.Map;

/**
 * ITouchpoint data instances contain the additional information needed by a touchpoint
 * to execute each engine phase it participates in. This includes the sequence of
 * instruction statements to be executed during each phase, and any additional
 * supporting data needed to perform the phase.
 *
 * @see MetadataFactory#createTouchpointData(Map)
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface ITouchpointData {

	/**
	 * Returns the touchpoint instruction corresponding to the given key.
	 * 
	 * @return the touchpoint instruction corresponding to the given key,
	 * or <code>null</code> if no such instruction exists.
	 */
	public ITouchpointInstruction getInstruction(String instructionKey);

	/**
	 * Returns an unmodifiable map of the touchpoint instructions. The map
	 * keys are strings, and the values are instances of {@link ITouchpointInstruction}.
	 *
	 * @return the touchpoint instructions
	 */
	public Map<String, ITouchpointInstruction> getInstructions();

	/**
	 * Returns whether this TouchpointData is equal to the given object.
	 * 
	 * This method returns <i>true</i> if:
	 * <ul>
	 *  <li> Both this object and the given object are of type ITouchpointData
	 *  <li> The result of <b>getInstructions()</b> on both objects are equal
	 * </ul> 
	 */
	public boolean equals(Object obj);
}