/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public class InstallableUnitPatch extends InstallableUnit implements IInstallableUnitPatch {
	private IRequirementChange[] changes;
	private IRequiredCapability lifeCycle;
	private IRequiredCapability[][] scope;

	private void addRequiredCapability(IRequiredCapability[] toAdd) {
		IRequiredCapability[] current = super.getRequiredCapabilities();
		IRequiredCapability[] result = new IRequiredCapability[current.length + toAdd.length];
		System.arraycopy(current, 0, result, 0, current.length);
		System.arraycopy(toAdd, 0, result, current.length, toAdd.length);
		setRequiredCapabilities(result);
	}

	public IRequiredCapability[][] getApplicabilityScope() {
		return scope;
	}

	public IRequiredCapability getLifeCycle() {
		return lifeCycle;
	}

	public IRequirementChange[] getRequirementsChange() {
		return changes;
	}

	public void setApplicabilityScope(IRequiredCapability[][] applyTo) {
		scope = applyTo;
	}

	public void setLifeCycle(IRequiredCapability lifeCycle) {
		if (lifeCycle == null)
			return;
		this.lifeCycle = lifeCycle;
		addRequiredCapability(new IRequiredCapability[] {lifeCycle});
	}

	public void setRequirementsChange(IRequirementChange[] changes) {
		this.changes = changes;
	}
}
