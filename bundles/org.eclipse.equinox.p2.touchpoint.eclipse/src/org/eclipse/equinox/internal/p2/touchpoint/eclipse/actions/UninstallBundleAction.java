/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
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
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.NLS;

public class UninstallBundleAction extends ProvisioningAction {
	public static final String ID = "uninstallBundle"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		return UninstallBundleAction.uninstallBundle(parameters);
	}

	public IStatus undo(Map<String, Object> parameters) {
		return InstallBundleAction.installBundle(parameters);
	}

	public static IStatus uninstallBundle(Map<String, Object> parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String bundleId = (String) parameters.get(ActionConstants.PARM_BUNDLE);
		if (bundleId == null) {
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_BUNDLE, ID));
		}

		//TODO: eventually remove this. What is a fragment doing here??
		if (QueryUtil.isFragment(iu)) {
			System.out.println("What is a fragment doing here!!! -- " + iu); //$NON-NLS-1$
			return Status.OK_STATUS;
		}

		// Changes to this object will be reflected in the backing runtime configuration store
		BundleInfo bundleInfo = Util.findBundleInfo(manipulator.getConfigData(), iu);
		if (bundleInfo == null) {
			return Util.createWarning(NLS.bind(Messages.failed_find_bundleinfo, iu));
		}

		manipulator.getConfigData().removeBundle(bundleInfo);
		return Status.OK_STATUS;
	}
}