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

/**
 * A touchpoint instruction contains either a sequence of instruction statements
 * to be executed during a particular engine phase, or some simple string value
 * that is needed by a touchpoint to execute its phases.
 * <p>
 * The format of a touchpoint instruction statement sequence is as follows:
 * 
 *   statement-sequence :
 *     | statement ';'
 *      | statement-sequence statement
 *      ;
 *
 *Where a statement is of the format:
 *
 *  statement :
 *      | actionName '(' parameters ')'
 *      ;
 *
 *  parameters :
 *      | // empty
 *      | parameter
 *      | parameters ',' parameter
 *      ;
 *
 *   parameter : 
 *      | paramName ':' paramValue
 *      ;
 *
 * actionName, paramName, paramValue :
 *      | String 
 *      ;
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 * @see MetadataFactory#createTouchpointInstruction(String, String)
 */
public interface ITouchpointInstruction {

	/**
	 * Returns the body of this touchpoint instruction. The body is either a sequence
	 * of instruction statements, or a simple string value.
	 * 
	 * @return The body of this touchpoint instruction
	 */
	public String getBody();

	//TODO What is this? Please doc
	public String getImportAttribute();

	/**
	 * Returns whether this TouchpointInstruction is equal to the given object.
	 * 
	 * This method returns <i>true</i> if:
	 * <ul>
	 *  <li> Both this object and the given object are of type ITouchpointInstruction
	 *  <li> The result of <b>getBody()</b> on both objects are equal
	 *  <li> The result of <b>getImportAttribute()</b> on both objects are equal
	 * </ul> 
	 */
	public boolean equals(Object obj);

}