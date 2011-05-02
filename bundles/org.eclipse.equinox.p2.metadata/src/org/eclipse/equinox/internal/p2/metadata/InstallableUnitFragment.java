/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public Collection<IRequirement> getHost() {
		return hostRequirements;
	}

	public Object getMember(String memberName) {
		return "host".equals(memberName) ? hostRequirements : super.getMember(memberName); //$NON-NLS-1$
	}
}
