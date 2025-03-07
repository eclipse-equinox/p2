/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp, inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives.actions;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Activator;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Messages;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class BlockMacUpdate extends ProvisioningAction {

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		if (!runningOldMacLayout()) {
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, Activator.ID, Messages.BlockMacUpdate_1);
	}

	private boolean runningOldMacLayout() {
		if (!org.eclipse.osgi.service.environment.Constants.OS_MACOSX.equals(getOS())) {
			return false;
		}
		Version fwkAdminVersion = getFrameworkAdminVersion();
		if (fwkAdminVersion == null) {
			return false;
		}
		if (fwkAdminVersion.compareTo(new Version("1.0.500.v201523")) < 0) { //$NON-NLS-1$
			return true;
		}
		return false;
	}

	private Version getFrameworkAdminVersion() {
		ServiceReference<PackageAdmin> sr = Activator.getContext().getServiceReference(PackageAdmin.class);
		if (sr == null) {
			return null;
		}
		PackageAdmin packageAdmin = Activator.getContext().getService(sr);
		if (packageAdmin == null) {
			return null;
		}
		Activator.getContext().ungetService(sr);
		Bundle[] bundles = packageAdmin.getBundles("org.eclipse.equinox.frameworkadmin.equinox", null); //$NON-NLS-1$
		if (bundles == null) {
			return null;
		}
		//Return the first bundle that is not installed or uninstalled
		for (Bundle bundle : bundles) {
			if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundle.getVersion();
			}
		}
		return null;
	}

	private String getOS() {
		ServiceReference<EnvironmentInfo> sr = Activator.getContext().getServiceReference(EnvironmentInfo.class);
		if (sr == null) {
			return null;
		}
		String value = Activator.getContext().getService(sr).getOS();
		Activator.getContext().ungetService(sr);
		return value;
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		// Nothing to do since we are not modifying any state.
		return null;
	}

}
