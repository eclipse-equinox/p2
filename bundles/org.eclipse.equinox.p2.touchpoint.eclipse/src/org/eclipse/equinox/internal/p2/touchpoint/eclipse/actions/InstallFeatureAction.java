/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others. All rights reserved. This
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
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.*;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class InstallFeatureAction extends ProvisioningAction {
	public static final String ID = "installFeature"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_APPLICATION_PROP = "org.eclipse.update.feature.application"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_PLUGIN_PROP = "org.eclipse.update.feature.plugin"; //$NON-NLS-1$
	private static final String UPDATE_FEATURE_PRIMARY_PROP = "org.eclipse.update.feature.primary"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		return InstallFeatureAction.installFeature(parameters);
	}

	public IStatus undo(Map<String, Object> parameters) {
		return UninstallFeatureAction.uninstallFeature(parameters);
	}

	public static IStatus installFeature(Map<String, Object> parameters) {
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

		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
		File file = Util.getArtifactFile(agent, artifactKey, profile);
		if (file == null || !file.exists()) {
			return Util.createError(NLS.bind(Messages.artifact_file_not_found, artifactKey));
		}
		String pluginId = iu.getProperty(UPDATE_FEATURE_PLUGIN_PROP);
		boolean isPrimary = Boolean.parseBoolean(iu.getProperty(UPDATE_FEATURE_PRIMARY_PROP));
		String application = iu.getProperty(UPDATE_FEATURE_APPLICATION_PROP);
		// TODO this isn't right... but we will leave it for now because we don't actually use the value in the install
		String pluginVersion = artifactKey.getVersion().toString();
		return configuration.addFeatureEntry(file, featureId, featureVersion, pluginId, pluginVersion, isPrimary, application, /*root*/null, iu.getProperty(Site.PROP_LINK_FILE));
	}
}
