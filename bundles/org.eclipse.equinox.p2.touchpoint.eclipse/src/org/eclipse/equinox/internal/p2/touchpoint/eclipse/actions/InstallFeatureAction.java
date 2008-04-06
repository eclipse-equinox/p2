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
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningAction;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class InstallFeatureAction extends ProvisioningAction {
	public static final String ID = "installFeature"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		return InstallFeatureAction.installFeature(parameters);
	}

	public IStatus undo(Map parameters) {
		return UninstallFeatureAction.uninstallFeature(parameters);
	}

	public static IStatus installFeature(Map parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		PlatformConfigurationWrapper configuration = (PlatformConfigurationWrapper) parameters.get(EclipseTouchpoint.PARM_PLATFORM_CONFIGURATION);
		String feature = (String) parameters.get(ActionConstants.PARM_FEATURE);
		String featureId = (String) parameters.get(ActionConstants.PARM_FEATURE_ID);
		String featureVersion = (String) parameters.get(ActionConstants.PARM_FEATURE_VERSION);

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.length == 0)
			return Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu));

		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(feature)) {
				artifactKey = artifacts[i];
				break;
			}
		}

		if (featureId == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_FEATURE_ID, ID));
		else if (ActionConstants.PARM_DEFAULT_VALUE.equals(featureId)) {
			featureId = artifactKey.getId();
		}

		if (featureVersion == null)
			return Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_FEATURE_VERSION, ID));
		else if (ActionConstants.PARM_DEFAULT_VALUE.equals(featureVersion)) {
			featureVersion = artifactKey.getVersion().toString();
		}

		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		File file = Util.getArtifactFile(artifactKey, profile);
		if (file == null || !file.exists()) {
			return Util.createError(NLS.bind(Messages.artifact_file_not_found, artifactKey));
		}
		return configuration.addFeatureEntry(file, featureId, featureVersion, artifactKey.getId(), artifactKey.getVersion().toString(), /*primary*/false, /*application*/null, /*root*/null);
	}
}