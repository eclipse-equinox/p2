/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;

import java.io.File;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class InstallBundleAction extends ProvisioningAction {
	public static final String ID = "installBundle"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		return InstallBundleAction.installBundle(parameters);
	}

	public IStatus undo(Map parameters) {
		return UninstallBundleAction.uninstallBundle(parameters);
	}

	public static IStatus installBundle(Map parameters) {
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String bundleId = (String) parameters.get(ActionConstants.PARM_BUNDLE);
		if (bundleId == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_BUNDLE, ID));

		//TODO: eventually remove this. What is a fragment doing here??
		if (iu.isFragment()) {
			System.out.println("What is a fragment doing here!!! -- " + iu); //$NON-NLS-1$
			return Status.OK_STATUS;
		}

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.length == 0)
			return Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu));

		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(bundleId)) {
				artifactKey = artifacts[i];
				break;
			}
		}
		if (artifactKey == null)
			throw new IllegalArgumentException(NLS.bind(Messages.no_matching_artifact, bundleId));

		File bundleFile = Util.getArtifactFile(artifactKey, profile);
		if (bundleFile == null || !bundleFile.exists())
			return Util.createError(NLS.bind(Messages.artifact_file_not_found, artifactKey));

		//By now we always have the manifest in the touchpoint data
		String manifest = Util.getManifest(iu.getTouchpointData());
		if (manifest == null)
			return Util.createError(NLS.bind(Messages.missing_manifest, iu));

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, manifest);
		if (bundleInfo == null)
			return Util.createError(NLS.bind(Messages.failed_bundleinfo, iu));
		manipulator.getConfigData().addBundle(bundleInfo);

		return Status.OK_STATUS;
	}
}