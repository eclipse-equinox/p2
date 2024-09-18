/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.equinox.p2.metadata.ITouchpointInstruction;
import org.eclipse.equinox.p2.metadata.MetadataFactory;

/**
 * A touchpoint instruction contains either a sequence of instruction statements
 * to be executed during a particular engine phase, or some simple string value
 * that is needed by a touchpoint to execute its phases.
 * <p>
 * The format of a touchpoint instruction statement sequence is as follows:
 * 
 * statement-sequence : | statement ';' | statement-sequence statement ;
 *
 * Where a statement is of the format:
 *
 * statement : | actionName '(' parameters ')' ;
 *
 * parameters : | // empty | parameter | parameters ',' parameter ;
 *
 * parameter : | paramName ':' paramValue ;
 *
 * actionName, paramName, paramValue : | String ;
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @see MetadataFactory#createTouchpointInstruction(String, String)
 */
public class TouchpointInstruction implements ITouchpointInstruction {

	private final String body;
	private final String importAttribute;

	/**
	 * Encodes an action statement in string form. This method will take care of
	 * escaping any illegal characters in function parameter values.
	 * 
	 * @param actionName The name of the action.
	 * @param parameters The function's parameters. This is a
	 *                   Map&lt;String,String&gt; where the keys are parameter names
	 *                   and the values are parameter values
	 * @return An encoded touchpoint instruction statement
	 */
	public static String encodeAction(String actionName, Map<String, String> parameters) {
		StringBuilder result = new StringBuilder(actionName);
		result.append('(');
		boolean first = true;
		for (Entry<String, String> entry : parameters.entrySet()) {
			if (first)
				first = false;
			else
				result.append(',');
			result.append(entry.getKey());
			result.append(':');
			appendEncoded(result, entry.getValue());
		}
		result.append(')').append(';');
		return result.toString();
	}

	/**
	 * Append the given value to the given buffer, encoding any illegal characters
	 * with appropriate escape sequences.
	 */
	private static void appendEncoded(StringBuilder buf, String value) {
		char[] chars = value.toCharArray();
		for (char c : chars) {
			switch (c) {
			case '$':
			case ',':
			case ':':
			case ';':
			case '{':
			case '}':
				buf.append("${#").append(Integer.toString(c)).append('}'); //$NON-NLS-1$
				break;
			default:
				buf.append(c);
			}
		}
	}

	/**
	 * Clients must use the factory method on {@link MetadataFactory}.
	 */
	public TouchpointInstruction(String body, String importAttribute) {
		this.body = body;
		this.importAttribute = importAttribute;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ITouchpointInstruction))
			return false;
		ITouchpointInstruction other = (ITouchpointInstruction) obj;
		if (body == null) {
			if (other.getBody() != null)
				return false;
		} else if (!body.equals(other.getBody()))
			return false;
		if (importAttribute == null) {
			if (other.getImportAttribute() != null)
				return false;
		} else if (!importAttribute.equals(other.getImportAttribute()))
			return false;
		return true;
	}

	/**
	 * Returns the body of this touchpoint instruction. The body is either a
	 * sequence of instruction statements, or a simple string value.
	 * 
	 * @return The body of this touchpoint instruction
	 */
	@Override
	public String getBody() {
		return body;
	}

	// TODO What is this? Please doc
	@Override
	public String getImportAttribute() {
		return importAttribute;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((importAttribute == null) ? 0 : importAttribute.hashCode());
		return result;
	}

	/**
	 * Returns a string representation of this instruction for debugging purposes
	 * only.
	 */
	@Override
	public String toString() {
		return "Instruction[" + body + ',' + importAttribute + ']'; //$NON-NLS-1$
	}
}
