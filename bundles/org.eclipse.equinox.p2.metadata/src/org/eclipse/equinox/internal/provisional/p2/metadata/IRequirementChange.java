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
	 * See <code>{@link org.eclipse.equinox.internal.provisional.p2.metadata.RequirementChange#equals(Object)}</code>
	 * for an example implementation.  SPIs must maintain the same semantics.
	 */
	public boolean equals(Object other);
}