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
	private RequirementChange[] changes;
	private RequiredCapability lifeCycle;
	private RequiredCapability[][] scope;

	private void addRequiredCapability(RequiredCapability[] toAdd) {
		RequiredCapability[] current = super.getRequiredCapabilities();
		RequiredCapability[] result = new RequiredCapability[current.length + toAdd.length];
		System.arraycopy(current, 0, result, 0, current.length);
		System.arraycopy(toAdd, 0, result, current.length, toAdd.length);
		setRequiredCapabilities(result);
	}

	public RequiredCapability[][] getApplicabilityScope() {
		return scope;
	}

	public RequiredCapability getLifeCycle() {
		return lifeCycle;
	}

	public RequirementChange[] getRequirementsChange() {
		return changes;
	}

	public void setApplicabilityScope(RequiredCapability[][] applyTo) {
		scope = applyTo;
	}

	public void setLifeCycle(RequiredCapability lifeCycle) {
		if (lifeCycle == null)
			return;
		this.lifeCycle = lifeCycle;
		addRequiredCapability(new RequiredCapability[] {lifeCycle});
	}

	public void setRequirementsChange(RequirementChange[] changes) {
		this.changes = changes;
	}
}
