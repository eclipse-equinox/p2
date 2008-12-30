/******************************************************************************* 
* Copyright (c) 2008 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.metadata;

public interface IRequirementChange {

	public IRequiredCapability applyOn();

	public IRequiredCapability newValue();

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
	 */
	public boolean equals(Object other);
}