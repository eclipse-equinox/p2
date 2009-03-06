/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.io.File;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class MarkStartedAction extends ProvisioningAction {
	public static final String ID = "markStarted"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		String started = (String) parameters.get(ActionConstants.PARM_STARTED);
		if (started == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_STARTED, ID));

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.length == 0)
			return Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu));

		IArtifactKey artifactKey = artifacts[0];

		// the bundleFile might be null here, that's OK.
		File bundleFile = Util.getArtifactFile(artifactKey, profile);

		String manifest = Util.getManifest(iu.getTouchpointData());
		if (manifest == null)
			return Util.createError(NLS.bind(Messages.missing_manifest, iu));

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, manifest);
		if (bundleInfo == null)
			return Util.createError(NLS.bind(Messages.failed_bundleinfo, iu));

		if (bundleInfo.isFragment())
			return Status.OK_STATUS;

		BundleInfo[] bundles = manipulator.getConfigData().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].equals(bundleInfo)) {
				getMemento().put(ActionConstants.PARM_PREVIOUS_STARTED, new Boolean(bundles[i].isMarkedAsStarted()));
				bundles[i].setMarkedAsStarted(Boolean.valueOf(started).booleanValue());
				break;
			}
		}
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		Boolean previousStarted = (Boolean) getMemento().get(ActionConstants.PARM_PREVIOUS_STARTED);
		if (previousStarted == null)
			return Status.OK_STATUS;

		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.length == 0)
			return Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu));

		IArtifactKey artifactKey = artifacts[0];
		// the bundleFile might be null here, that's OK.
		File bundleFile = Util.getArtifactFile(artifactKey, profile);

		String manifest = Util.getManifest(iu.getTouchpointData());
		if (manifest == null)
			return Util.createError(NLS.bind(Messages.missing_manifest, iu));

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, manifest);
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