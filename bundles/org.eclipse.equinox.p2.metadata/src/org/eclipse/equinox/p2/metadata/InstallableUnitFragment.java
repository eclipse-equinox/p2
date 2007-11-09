/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class InstallableUnitFragment extends InstallableUnit implements IInstallableUnitFragment {

	public static ProvidedCapability FRAGMENT_CAPABILITY = new ProvidedCapability(IU_KIND_NAMESPACE, "iu.fragment", new Version(1, 0, 0)); //$NON-NLS-1$

	//a host id of null is used for the default fragment
	private String hostId = null;
	private VersionRange hostRange = VersionRange.emptyRange;

	public InstallableUnitFragment() {
		super();
	}

	public void setHost(String iuId, VersionRange versionRange) {
		if (versionRange == null)
			throw new IllegalArgumentException();
		hostId = iuId;
		hostRange = versionRange;
		if (hostId != null)
			addRequiredCapability(RequiredCapability.createRequiredCapabilityForName(iuId, versionRange, false));
	}

	public String getHostId() {
		return hostId;
	}

	public VersionRange getHostVersionRange() {
		return hostRange;
	}

	private void addRequiredCapability(RequiredCapability toAdd) {
		RequiredCapability[] current = super.getRequiredCapabilities();
		RequiredCapability[] result = new RequiredCapability[current.length + 1];
		System.arraycopy(current, 0, result, 0, current.length);
		result[current.length] = toAdd;
		setRequiredCapabilities(result);
	}

	public boolean isFragment() {
		return true;
	}

	public IResolvedInstallableUnit getResolved() {
		return new ResolvedInstallableUnitFragment(this);
	}
}
