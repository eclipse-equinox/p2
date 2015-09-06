/*******************************************************************************
 *  Copyright (c) 2008, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class MarkStartedAction extends ProvisioningAction {
	public static final String ID = "markStarted"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		String started = (String) parameters.get(ActionConstants.PARM_STARTED);
		if (started == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_STARTED, ID));

		Collection<IArtifactKey> artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.isEmpty())
			return Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu));

		IArtifactKey artifactKey = artifacts.iterator().next();

		// the bundleFile might be null here, that's OK.
		File bundleFile = Util.getArtifactFile(agent, artifactKey, profile);

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, iu);
		if (bundleInfo == null)
			return Util.createError(NLS.bind(Messages.failed_bundleinfo, iu));

		if (bundleInfo.getFragmentHost() != null)
			return Status.OK_STATUS;

		BundleInfo[] bundles = manipulator.getConfigData().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].equals(bundleInfo)) {
				getMemento().put(ActionConstants.PARM_PREVIOUS_STARTED, Boolean.valueOf(bundles[i].isMarkedAsStarted()));
				bundles[i].setMarkedAsStarted(Boolean.parseBoolean(started));
				break;
			}
		}
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		Boolean previousStarted = (Boolean) getMemento().get(ActionConstants.PARM_PREVIOUS_STARTED);
		if (previousStarted == null)
			return Status.OK_STATUS;

		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);

		Collection<IArtifactKey> artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.isEmpty())
			return Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu));

		IArtifactKey artifactKey = artifacts.iterator().next();
		// the bundleFile might be null here, that's OK.
		File bundleFile = Util.getArtifactFile(agent, artifactKey, profile);

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, iu);
		if (bundleInfo == null)
			return Util.createError(NLS.bind(Messages.failed_bundleinfo, iu));

		BundleInfo[] bundles = manipulator.getConfigData().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].equals(bundleInfo)) {
				bundles[i].setMarkedAsStarted(previousStarted.booleanValue());
				break;
			}
		}
		return Status.OK_STATUS;
	}
}
