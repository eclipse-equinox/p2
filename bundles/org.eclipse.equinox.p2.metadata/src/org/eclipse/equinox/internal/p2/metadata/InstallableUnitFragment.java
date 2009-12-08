/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IRequirement;

public class InstallableUnitFragment extends InstallableUnit implements IInstallableUnitFragment {

	private IRequirement[] hostRequirements;

	public InstallableUnitFragment() {
		super();
	}

	public void setHost(IRequirement[] hostRequirements) {
		if (hostRequirements == null)
			return;
		this.hostRequirements = hostRequirements;
		addRequiredCapability(hostRequirements);
	}

	private void addRequiredCapability(IRequirement[] toAdd) {
		IRequirement[] current = super.getRequiredCapabilities();
		IRequirement[] result = new IRequirement[current.length + toAdd.length];
		System.arraycopy(current, 0, result, 0, current.length);
		System.arraycopy(toAdd, 0, result, current.length, toAdd.length);
		setRequiredCapabilities(result);
	}

	public IRequirement[] getHost() {
		return hostRequirements;
	}
}
