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
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;

public class InstallableUnitFragment extends InstallableUnit implements IInstallableUnitFragment {

	private IRequiredCapability[] hostRequirements;

	public InstallableUnitFragment() {
		super();
	}

	public void setHost(IRequiredCapability[] hostRequirements) {
		if (hostRequirements == null)
			return;
		this.hostRequirements = hostRequirements;
		addRequiredCapability(hostRequirements);
	}

	private void addRequiredCapability(IRequiredCapability[] toAdd) {
		IRequiredCapability[] current = super.getRequiredCapabilities();
		IRequiredCapability[] result = new IRequiredCapability[current.length + toAdd.length];
		System.arraycopy(current, 0, result, 0, current.length);
		System.arraycopy(toAdd, 0, result, current.length, toAdd.length);
		setRequiredCapabilities(result);
	}

	public boolean isFragment() {
		return true;
	}

	public IRequiredCapability[] getHost() {
		return hostRequirements;
	}
}
