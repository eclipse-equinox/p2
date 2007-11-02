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

import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class InstallableUnitFragment extends InstallableUnit implements IInstallableUnitFragment {

	public static ProvidedCapability FRAGMENT_CAPABILITY = new ProvidedCapability(IU_KIND_NAMESPACE, "iu.fragment", new Version(1, 0, 0)); //$NON-NLS-1$

	private String hostId;
	private VersionRange hostRange = VersionRange.emptyRange;

	public InstallableUnitFragment() {
		super();
	}

	public InstallableUnitFragment(String id, Version version, boolean singleton, String hostId, VersionRange hostRange) {
		super(id, version, singleton);
		this.hostId = hostId;
		if (hostRange != null)
			this.hostRange = hostRange;
	}

	private void addRequiredCapability(RequiredCapability toAdd) {
		RequiredCapability[] current = super.getRequiredCapabilities();
		RequiredCapability[] result = new RequiredCapability[current.length + 1];
		System.arraycopy(current, 0, result, 0, current.length);
		result[current.length] = toAdd;
		setRequiredCapabilities(result);
	}

	public String getHostId() {
		return hostId;
	}

	public VersionRange getHostVersionRange() {
		return hostRange;
	}

	public ProvidedCapability[] getProvidedCapabilities() {
		ProvidedCapability[] otherCapabilities = super.getProvidedCapabilities();
		if (otherCapabilities.length == 0)
			return new ProvidedCapability[] {FRAGMENT_CAPABILITY};
		//		return otherCapabilities;
		ProvidedCapability[] result = new ProvidedCapability[otherCapabilities.length + 1];
		System.arraycopy(otherCapabilities, 0, result, 1, otherCapabilities.length);
		result[0] = FRAGMENT_CAPABILITY;
		return result;
	}

	public IResolvedInstallableUnit getResolved() {
		return new ResolvedInstallableUnitFragment(this);
	}

	public boolean isFragment() {
		return true;
	}

	public void setHost(String iuId, VersionRange versionRange) {
		if (iuId == null || versionRange == null)
			throw new IllegalArgumentException();
		hostId = iuId;
		hostRange = versionRange;
		addRequiredCapability(RequiredCapability.createRequiredCapabilityForName(iuId, versionRange, false));
	}
}