/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionFactory;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdminRuntimeException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

public class EclipseTouchpoint extends Touchpoint {

	private static final TouchpointType TOUCHPOINT_TYPE = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", new Version("1.0")); //$NON-NLS-1$ //$NON-NLS-2$

	// TODO: phase id constants should be defined elsewhere.
	public static final String INSTALL_PHASE_ID = "install"; //$NON-NLS-1$
	public static final String UNINSTALL_PHASE_ID = "uninstall"; //$NON-NLS-1$

	public static final String PROFILE_PROP_LAUNCHER_NAME = "eclipse.touchpoint.launcherName"; //$NON-NLS-1$
	public static final String PARM_MANIPULATOR = "manipulator"; //$NON-NLS-1$
	public static final String PARM_PLATFORM_CONFIGURATION = "platformConfiguration"; //$NON-NLS-1$
	public static final String PARM_SOURCE_BUNDLES = "sourceBundles"; //$NON-NLS-1$
	public static final String PARM_IU = "iu"; //$NON-NLS-1$
	public static final String PARM_INSTALL_FOLDER = "installFolder"; //$NON-NLS-1$

	public IStatus completePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map touchpointParameters) {
		Manipulator manipulator = (Manipulator) touchpointParameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		try {
			manipulator.save(false);
		} catch (RuntimeException e) {
			return Util.createError(Messages.error_saving_manipulator, e);
		} catch (IOException e) {
			return Util.createError(Messages.error_saving_manipulator, e);
		}

		if (INSTALL_PHASE_ID.equals(phaseId) || UNINSTALL_PHASE_ID.equals(phaseId)) {
			PlatformConfigurationWrapper configuration = (PlatformConfigurationWrapper) touchpointParameters.get(EclipseTouchpoint.PARM_PLATFORM_CONFIGURATION);
			try {
				configuration.save();
			} catch (ProvisionException pe) {
				return Util.createError(Messages.error_saving_platform_configuration, pe);
			}
		}

		SourceManipulator m = (SourceManipulator) touchpointParameters.get(EclipseTouchpoint.PARM_SOURCE_BUNDLES);
		try {
			m.save();
		} catch (IOException e) {
			return Util.createError(Messages.error_saving_source_bundles_list, e);
		}

		return Status.OK_STATUS;
	}

	public ProvisioningAction getAction(String actionId) {
		return ActionFactory.create(actionId);
	}

	public TouchpointType getTouchpointType() {
		//TODO this data probably needs to come from the XML
		return TOUCHPOINT_TYPE;
	}

	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map touchpointParameters) {
		touchpointParameters.put(PARM_INSTALL_FOLDER, Util.getInstallFolder(profile));
		LazyManipulator manipulator = new LazyManipulator(profile);
		touchpointParameters.put(PARM_MANIPULATOR, manipulator);
		touchpointParameters.put(PARM_SOURCE_BUNDLES, new SourceManipulator(profile));
		File configLocation = Util.getConfigurationFolder(profile);
		URL poolURL = Util.getBundlePoolLocation(profile);
		touchpointParameters.put(PARM_PLATFORM_CONFIGURATION, new PlatformConfigurationWrapper(configLocation, poolURL, manipulator));
		return null;
	}

	public IStatus initializeOperand(IProfile profile, String phaseId, InstallableUnitOperand operand, Map parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		if (iu != null && Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue()) {
			IInstallableUnit preparedIU = prepareIU(iu, profile);
			if (preparedIU == null)
				return Util.createError(NLS.bind(Messages.failed_prepareIU, iu));

			parameters.put(PARM_IU, preparedIU);
		}
		return Status.OK_STATUS;
	}

	private IInstallableUnit prepareIU(IInstallableUnit iu, IProfile profile) {

		Class c = null;
		try {
			c = Class.forName("org.eclipse.equinox.spi.p2.publisher.PublisherHelper"); //$NON-NLS-1$
			if (c != null)
				c = Class.forName("org.eclipse.osgi.service.resolver.PlatformAdmin"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(NLS.bind(Messages.generator_not_available, e.getMessage()));
		}

		if (c != null) {
			IArtifactKey[] artifacts = iu.getArtifacts();
			if (artifacts == null || artifacts.length == 0)
				return iu;

			IArtifactKey artifactKey = artifacts[0];
			if (artifactKey == null)
				return iu;

			File bundleFile = Util.getArtifactFile(artifactKey, profile);
			if (bundleFile == null) {
				LogHelper.log(Util.createError(NLS.bind(Messages.artifact_file_not_found, artifactKey.toString())));
				return null;
			}
			return createBundleIU(artifactKey, bundleFile);
		}

		// should not occur
		throw new IllegalStateException(Messages.unexpected_prepareiu_error);
	}

	private IInstallableUnit createBundleIU(IArtifactKey artifactKey, File bundleFile) {
		BundleDescription bundleDescription = BundlesAction.createBundleDescription(bundleFile);
		return PublisherHelper.createBundleIU(bundleDescription, (Map) bundleDescription.getUserObject(), bundleFile.isDirectory(), artifactKey);
	}

	public static IStatus loadManipulator(Manipulator manipulator) {
		try {
			manipulator.load();
		} catch (IllegalStateException e) {
			return Util.createError(Messages.error_loading_manipulator);
		} catch (FrameworkAdminRuntimeException e) {
			return Util.createError(Messages.error_loading_manipulator);
		} catch (IOException e) {
			return Util.createError(Messages.error_loading_manipulator);
		}
		return Status.OK_STATUS;
	}
}
