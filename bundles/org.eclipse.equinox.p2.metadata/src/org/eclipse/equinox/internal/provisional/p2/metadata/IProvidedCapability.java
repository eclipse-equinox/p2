/******************************************************************************* 
* Copyright (c) 2008, 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
 *   IBM - ongoing development
******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.core.Version;

/**
 * Describes a capability as exposed or required by an installable unit
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IProvidedCapability {

	public String getName();

	public String getNamespace();

	public Version getVersion();

	/**
	 * Returns whether this provided capability satisfies the given required capability.
	 * @return <code>true</code> if this capability satisfies the given required
	 * capability, and <code>false</code> otherwise.
	 * 
	 * This method must maintain the following semantics:
	 * <ul>
	 *  <li> If the provided capability and the candidate have different names, 
	 *       return false
	 *  <li> If the provided capability and the candidate have different namespaces.
	 *       return false
	 *  <li> If the candidate's version range includes the provided capability's
	 *       version, return true
	 *  <li> otherwise, return false    
	 * </ul>
	 * 
	 */
	public boolean satisfies(IRequiredCapability candidate);

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