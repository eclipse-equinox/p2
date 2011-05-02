/******************************************************************************* 
* Copyright (c) 2008, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
 *   IBM - ongoing development
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

/**
 * Describes a capability as exposed or required by an installable unit
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IProvidedCapability {

	/**
	 * 
	 * @return String
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public String getName();

	/**
	 * 
	 * @return String
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public String getNamespace();

	/**
	 * 
	 * @return String
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public Version getVersion();

	/**
	 * Returns whether this provided capability is equal to the given object.
	 * 
	 * This method returns <i>true</i> if:
	 * <ul>
	 *  <li> Both this object and the given object are of type IProvidedCapability
	 *  <li> The result of <b>getName()</b> on both objects are equal
	 *  <li> The result of <b>getNamespace()</b> on both objects are equal
	 *  <li> The result of <b>getVersion()</b> on both objects are equal
	 * </ul> 
	 */
	public boolean equals(Object other);

}