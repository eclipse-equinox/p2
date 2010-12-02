/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
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
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.NLS;

public class InstallBundleAction extends ProvisioningAction {
	public static final String ID = "installBundle"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		return InstallBundleAction.installBundle(parameters);
	}

	public IStatus undo(Map<String, Object> parameters) {
		return UninstallBundleAction.uninstallBundle(parameters);
	}

	public static IStatus installBundle(Map<String, Object> parameters) {
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		Manipulator manipulator = (Manipulator) parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		String bundleId = (String) parameters.get(ActionConstants.PARM_BUNDLE);
		if (bundleId == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_BUNDLE, ID));

		//TODO: eventually remove this. What is a fragment doing here??
		if (QueryUtil.isFragment(iu)) {
			System.out.println("What is a fragment doing here!!! -- " + iu); //$NON-NLS-1$
			return Status.OK_STATUS;
		}

		Collection<IArtifactKey> artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.isEmpty())
			return Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu));

		IArtifactKey artifactKey = null;
		for (IArtifactKey candidate : artifacts) {
			if (candidate.toString().equals(bundleId)) {
				artifactKey = candidate;
				break;
			}
		}
		if (artifactKey == null)
			throw new IllegalArgumentException(NLS.bind(Messages.no_matching_artifact, bundleId));

		File bundleFile = Util.getArtifactFile(agent, artifactKey, profile);
		if (bundleFile == null || !bundleFile.exists())
			return Util.createError(NLS.bind(Messages.artifact_file_not_found, artifactKey));

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, iu);
		if (bundleInfo == null)
			return Util.createError(NLS.bind(Messages.failed_bundleinfo, iu));
		manipulator.getConfigData().addBundle(bundleInfo);

		return Status.OK_STATUS;
	}
}