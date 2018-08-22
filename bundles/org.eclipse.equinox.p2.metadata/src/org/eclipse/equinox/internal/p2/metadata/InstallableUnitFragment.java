/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import java.util.Collection;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IRequirement;

public class InstallableUnitFragment extends InstallableUnit implements IInstallableUnitFragment {

	private Collection<IRequirement> hostRequirements;

	public InstallableUnitFragment() {
		super();
	}

	public void setHost(Collection<IRequirement> hostRequirements) {
		if (hostRequirements == null)
			return;
		this.hostRequirements = hostRequirements;
	}

	@Override
	public Collection<IRequirement> getHost() {
		return hostRequirements;
	}

	@Override
	public Object getMember(String memberName) {
		return "host".equals(memberName) ? hostRequirements : super.getMember(memberName); //$NON-NLS-1$
	}
}
