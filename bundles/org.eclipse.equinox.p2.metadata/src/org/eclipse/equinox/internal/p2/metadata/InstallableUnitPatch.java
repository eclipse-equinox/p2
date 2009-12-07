/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequirementChange;
import org.eclipse.equinox.p2.metadata.IRequirement;

public class InstallableUnitPatch extends InstallableUnit implements IInstallableUnitPatch {
	private IRequirementChange[] changes;
	private IRequirement lifeCycle;
	private IRequirement[][] scope;

	private void addRequiredCapability(IRequirement[] toAdd) {
		IRequirement[] current = super.getRequiredCapabilities();
		IRequirement[] result = new IRequirement[current.length + toAdd.length];
		System.arraycopy(current, 0, result, 0, current.length);
		System.arraycopy(toAdd, 0, result, current.length, toAdd.length);
		setRequiredCapabilities(result);
	}

	public IRequirement[][] getApplicabilityScope() {
		return scope;
	}

	public IRequirement getLifeCycle() {
		return lifeCycle;
	}

	public IRequirementChange[] getRequirementsChange() {
		return changes;
	}

	public void setApplicabilityScope(IRequirement[][] applyTo) {
		scope = applyTo;
	}

	public void setLifeCycle(IRequirement lifeCycle) {
		if (lifeCycle == null)
			return;
		this.lifeCycle = lifeCycle;
		addRequiredCapability(new IRequirement[] {lifeCycle});
	}

	public void setRequirementsChange(IRequirementChange[] changes) {
		this.changes = changes;
	}
}
