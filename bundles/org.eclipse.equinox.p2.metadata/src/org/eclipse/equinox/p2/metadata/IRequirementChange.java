/******************************************************************************* 
* Copyright (c) 2008, 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IRequirementChange {

	/**
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 2.0
	 */
	public IRequiredCapability applyOn();

	/**
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 2.0
	 */
	public IRequiredCapability newValue();

	/**
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 2.0
	 */
	public boolean matches(IRequiredCapability toMatch);

	/**
	 * Returns whether this requirement change is equal to the given object.
	 * 
	 * This method returns <i>true</i> if:
	 * <ul>
	 *  <li> Both this object and the given object are of type IRequiredCapability
	 *  <li> The result of <b>applyOn()</b> on both objects are equal
	 *  <li> The result of <b>newValue()</b> on both objects are equal
	 * </ul> 
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 2.0
	 */
	public boolean equals(Object other);
}