/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.*;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class UninstallFeatureAction extends ProvisioningAction {
	public static final String ID = "uninstallFeature"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		return UninstallFeatureAction.uninstallFeature(parameters);
	}

	public IStatus undo(Map<String, Object> parameters) {
		return InstallFeatureAction.installFeature(parameters);
	}

	public static IStatus uninstallFeature(Map<String, Object> parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(EclipseTouchpoint.PARM_IU);
		PlatformConfigurationWrapper configuration = (PlatformConfigurationWrapper) parameters.get(EclipseTouchpoint.PARM_PLATFORM_CONFIGURATION);
		String feature = (String) parameters.get(ActionConstants.PARM_FEATURE);
		String featureId = (String) parameters.get(ActionConstants.PARM_FEATURE_ID);
		String featureVersion = (String) parameters.get(ActionConstants.PARM_FEATURE_VERSION);

		Collection<IArtifactKey> artifacts = iu.getArtifacts();
		if (artifacts == null || artifacts.isEmpty())
			return Util.createError(NLS.bind(Messages.iu_contains_no_arifacts, iu));

		IArtifactKey artifactKey = null;
		for (IArtifactKey candidate : artifacts) {
			if (candidate.toString().equals(feature)) {
				artifactKey = candidate;
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

		return configuration.removeFeatureEntry(featureId, featureVersion);
	}
}