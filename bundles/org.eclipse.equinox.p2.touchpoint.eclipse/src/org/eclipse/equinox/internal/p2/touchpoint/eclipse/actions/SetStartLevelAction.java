/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class SetStartLevelAction extends ProvisioningAction {
	public static final String ID = "setStartLevel"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		String startLevel = (String) parameters.get(ActionConstants.PARM_START_LEVEL);
		if (startLevel == null) {
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_START_LEVEL, ID));
		}

		// Changes to this object will be reflected in the backing runtime configuration store
		BundleInfo bundleInfo = Util.findBundleInfo(manipulator.getConfigData(), iu);
		if (bundleInfo == null) {
			return Util.createWarning(NLS.bind(Messages.failed_find_bundleinfo, iu));
		}

		// Bundle fragments are not started
		if (bundleInfo.getFragmentHost() != null) {
			return Status.OK_STATUS;
		}

		getMemento().put(ActionConstants.PARM_PREVIOUS_START_LEVEL, Integer.valueOf(bundleInfo.getStartLevel()));
		try {
			bundleInfo.setStartLevel(Integer.parseInt(startLevel));
			return Status.OK_STATUS;
		} catch (NumberFormatException e) {
			return Util.createError(NLS.bind(Messages.error_parsing_startlevel, startLevel, bundleInfo.getSymbolicName()), e);
		}
	}

	public IStatus undo(Map<String, Object> parameters) {
		Integer previousStartLevel = (Integer) getMemento().get(ActionConstants.PARM_PREVIOUS_START_LEVEL);
		if (previousStartLevel == null) {
			return Status.OK_STATUS;
		}
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);

		// Changes to this object will be reflected in the backing runtime configuration store
		BundleInfo bundleInfo = Util.findBundleInfo(manipulator.getConfigData(), iu);
		if (bundleInfo == null) {
			return Util.createWarning(NLS.bind(Messages.failed_find_bundleinfo, iu));
		}

		bundleInfo.setStartLevel(previousStartLevel.intValue());
		return Status.OK_STATUS;
	}
}
