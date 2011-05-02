/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.List;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.p2.metadata.*;

public class InstallableUnitPatch extends InstallableUnit implements IInstallableUnitPatch {
	public static final String MEMBER_APPLICABILITY_SCOPE = "applicabilityScope"; //$NON-NLS-1$
	public static final String MEMBER_LIFECYCLE = "lifeCycle"; //$NON-NLS-1$
	public static final String MEMBER_REQUIREMENTS_CHANGE = "requirementsChange"; //$NON-NLS-1$

	private IRequirementChange[] changes;
	private IRequirement lifeCycle;
	private IRequirement[][] scope;

	private void addRequiredCapability(IRequirement[] toAdd) {
		List<IRequirement> current = super.getRequirements();
		int currSize = current.size();
		IRequirement[] result = new IRequirement[currSize + toAdd.length];
		for (int i = 0; i < currSize; ++i)
			result[i] = current.get(i);
		System.arraycopy(toAdd, 0, result, current.size(), toAdd.length);
		setRequiredCapabilities(result);
	}

	public IRequirement[][] getApplicabilityScope() {
		return scope;
	}

	public IRequirement getLifeCycle() {
		return lifeCycle;
	}

	public List<IRequirementChange> getRequirementsChange() {
		return CollectionUtils.unmodifiableList(changes);
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

	public Object getMember(String memberName) {
		if (MEMBER_APPLICABILITY_SCOPE == memberName)
			return scope;
		if (MEMBER_LIFECYCLE == memberName)
			return lifeCycle;
		if (MEMBER_REQUIREMENTS_CHANGE == memberName)
			return changes;
		return super.getMember(memberName);
	}
}
