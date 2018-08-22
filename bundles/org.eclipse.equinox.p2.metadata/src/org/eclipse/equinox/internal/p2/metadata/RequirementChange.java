/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.p2.metadata.IRequirementChange;

public class RequirementChange implements IRequirementChange {
	private IRequiredCapability applyOn;
	private IRequiredCapability newValue;

	public RequirementChange(IRequiredCapability applyOn2, IRequiredCapability newValue2) {
		if (applyOn2 == null && newValue2 == null)
			throw new IllegalArgumentException();
		this.applyOn = applyOn2;
		this.newValue = newValue2;
	}

	@Override
	public IRequiredCapability applyOn() {
		return applyOn;
	}

	@Override
	public IRequiredCapability newValue() {
		return newValue;
	}

	@Override
	public boolean matches(IRequiredCapability toMatch) {
		if (!toMatch.getNamespace().equals(applyOn.getNamespace()))
			return false;
		if (!toMatch.getName().equals(applyOn.getName()))
			return false;
		if (toMatch.getRange().equals(applyOn.getRange()))
			return true;

		return toMatch.getRange().intersect(applyOn.getRange()) != null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((applyOn == null) ? 0 : applyOn.hashCode());
		result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IRequirementChange))
			return false;
		final IRequirementChange other = (IRequirementChange) obj;
		if (applyOn == null) {
			if (other.applyOn() != null)
				return false;
		} else if (!applyOn.equals(other.applyOn()))
			return false;
		if (newValue == null) {
			if (other.newValue() != null)
				return false;
		} else if (!newValue.equals(other.newValue()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return applyOn + " --> " + newValue; //$NON-NLS-1$
	}
}
