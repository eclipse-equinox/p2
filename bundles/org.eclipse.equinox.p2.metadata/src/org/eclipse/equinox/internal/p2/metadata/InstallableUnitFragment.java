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

import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.service.resolver.VersionRange;

public class InstallableUnitFragment extends InstallableUnit implements IInstallableUnitFragment {

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
			addRequiredCapability(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iuId, versionRange, null, false, false));
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
}
