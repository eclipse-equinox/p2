/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *     Eclipse Source - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdminRuntimeException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class EclipseTouchpoint extends Touchpoint {
	public static final String PROFILE_PROP_LAUNCHER_NAME = "eclipse.touchpoint.launcherName"; //$NON-NLS-1$
	public static final String PARM_MANIPULATOR = "manipulator"; //$NON-NLS-1$
	public static final String PARM_PLATFORM_CONFIGURATION = "platformConfiguration"; //$NON-NLS-1$
	public static final String PARM_SOURCE_BUNDLES = "sourceBundles"; //$NON-NLS-1$
	public static final String PARM_IU = "iu"; //$NON-NLS-1$
	public static final String PARM_ARTIFACT = "artifact"; //$NON-NLS-1$
	public static final String PARM_ARTIFACT_LOCATION = "artifact.location"; //$NON-NLS-1$
	private static final Object PARM_AGENT = "agent"; //$NON-NLS-1$

	private static final String NATIVE_TOUCHPOINT_ID = "org.eclipse.equinox.p2.touchpoint.natives"; //$NON-NLS-1$
	private static List<String> NATIVE_ACTIONS = Arrays.asList(new String[] {"mkdir", "rmdir"}); //$NON-NLS-1$//$NON-NLS-2$
	private static final String VALIDATE_PROFILE = "org.eclipse.equinox.internal.p2.touchpoint.eclipse.validateProfile"; //$NON-NLS-1$

	private static Map<IProfile, LazyManipulator> manipulators = new WeakHashMap<IProfile, LazyManipulator>();
	private static Map<IProfile, PlatformConfigurationWrapper> wrappers = new WeakHashMap<IProfile, PlatformConfigurationWrapper>();
	private static Map<IProfile, SourceManipulator> sourceManipulators = new WeakHashMap<IProfile, SourceManipulator>();
	private static Map<IProfile, Map<IInstallableUnit, IInstallableUnit>> preparedIUs = new WeakHashMap<IProfile, Map<IInstallableUnit, IInstallableUnit>>();

	private static synchronized LazyManipulator getManipulator(IProvisioningAgent agent, IProfile profile) {
		LazyManipulator manipulator = manipulators.get(profile);
		if (manipulator == null) {
			manipulator = new LazyManipulator(agent, profile);
			manipulators.put(profile, manipulator);
		}
		return manipulator;
	}

	private static synchronized void saveManipulator(IProfile profile) throws FrameworkAdminRuntimeException, IOException {
		LazyManipulator manipulator = manipulators.remove(profile);
		if (manipulator != null)
			manipulator.save(false);
	}

	private static synchronized PlatformConfigurationWrapper getPlatformConfigurationWrapper(IProvisioningAgent agent, IProfile profile, LazyManipulator manipulator) {
		PlatformConfigurationWrapper wrapper = wrappers.get(profile);
		if (wrapper == null) {
			File configLocation = Util.getConfigurationFolder(profile);
			URI poolURI = Util.getBundlePoolLocation(agent, profile);
			wrapper = new PlatformConfigurationWrapper(configLocation, poolURI, manipulator);
			wrappers.put(profile, wrapper);
		}
		return wrapper;
	}

	private static synchronized void savePlatformConfigurationWrapper(IProfile profile) throws ProvisionException {
		PlatformConfigurationWrapper wrapper = wrappers.remove(profile);
		if (wrapper != null)
			wrapper.save();
	}

	private static synchronized SourceManipulator getSourceManipulator(IProfile profile) {
		SourceManipulator sourceManipulator = sourceManipulators.get(profile);
		if (sourceManipulator == null) {
			sourceManipulator = new SourceManipulator(profile);
			sourceManipulators.put(profile, sourceManipulator);
		}
		return sourceManipulator;
	}

	private static synchronized void saveSourceManipulator(IProfile profile) throws IOException {
		SourceManipulator sourceManipulator = sourceManipulators.remove(profile);
		if (sourceManipulator != null)
			sourceManipulator.save();
	}

	private static synchronized IInstallableUnit getPreparedIU(IProfile profile, IInstallableUnit iu) {
		Map<IInstallableUnit, IInstallableUnit> preparedProfileIUs = preparedIUs.get(profile);
		if (preparedProfileIUs == null)
			return null;

		return preparedProfileIUs.get(iu);
	}

	private static synchronized void savePreparedIU(IProfile profile, IInstallableUnit iu) {
		Map<IInstallableUnit, IInstallableUnit> preparedProfileIUs = preparedIUs.get(profile);
		if (preparedProfileIUs == null) {
			preparedProfileIUs = new HashMap<IInstallableUnit, IInstallableUnit>();
			preparedIUs.put(profile, preparedProfileIUs);
		}
		preparedProfileIUs.put(iu, iu);
	}

	private static synchronized boolean hasPreparedIUs(IProfile profile) {
		return preparedIUs.get(profile) != null;
	}

	private static synchronized void clearProfileState(IProfile profile) {
		manipulators.remove(profile);
		wrappers.remove(profile);
		sourceManipulators.remove(profile);
		preparedIUs.remove(profile);
	}

	public IStatus prepare(IProfile profile) {
		try {
			if (hasPreparedIUs(profile))
				return validateProfile(profile);
		} catch (RuntimeException e) {
			return Util.createError(NLS.bind(Messages.error_validating_profile, profile.getProfileId()), e);
		}
		return Status.OK_STATUS;
	}

	public IStatus commit(IProfile profile) {
		MultiStatus status = new MultiStatus(Activator.ID, IStatus.OK, null, null);
		try {
			saveManipulator(profile);
		} catch (RuntimeException e) {
			status.add(Util.createError(Messages.error_saving_manipulator, e));
		} catch (IOException e) {
			status.add(Util.createError(Messages.error_saving_manipulator, e));
		}
		try {
			savePlatformConfigurationWrapper(profile);
		} catch (RuntimeException e) {
			status.add(Util.createError(Messages.error_saving_platform_configuration, e));
		} catch (ProvisionException pe) {
			status.add(Util.createError(Messages.error_saving_platform_configuration, pe));
		}

		try {
			saveSourceManipulator(profile);
		} catch (RuntimeException e) {
			status.add(Util.createError(Messages.error_saving_source_bundles_list, e));
		} catch (IOException e) {
			status.add(Util.createError(Messages.error_saving_source_bundles_list, e));
		}
		return status;
	}

	public IStatus rollback(IProfile profile) {
		clearProfileState(profile);
		return Status.OK_STATUS;
	}

	public String qualifyAction(String actionId) {
		String touchpointQualifier = NATIVE_ACTIONS.contains(actionId) ? NATIVE_TOUCHPOINT_ID : Activator.ID;
		return touchpointQualifier + "." + actionId; //$NON-NLS-1$
	}

	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> touchpointParameters) {
		IProvisioningAgent agent = (IProvisioningAgent) touchpointParameters.get(PARM_AGENT);
		LazyManipulator manipulator = getManipulator(agent, profile);
		touchpointParameters.put(PARM_MANIPULATOR, manipulator);
		touchpointParameters.put(PARM_SOURCE_BUNDLES, getSourceManipulator(profile));
		touchpointParameters.put(PARM_PLATFORM_CONFIGURATION, getPlatformConfigurationWrapper(agent, profile, manipulator));
		return null;
	}

	public IStatus initializeOperand(IProfile profile, Map<String, Object> parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		IArtifactKey artifactKey = (IArtifactKey) parameters.get(PARM_ARTIFACT);
		IProvisioningAgent agent = (IProvisioningAgent) parameters.get(PARM_AGENT);
		if (iu != null && Boolean.parseBoolean(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU))) {
			IInstallableUnit preparedIU = prepareIU(agent, profile, iu, artifactKey);
			if (preparedIU == null)
				return Util.createError(NLS.bind(Messages.failed_prepareIU, iu));

			parameters.put(PARM_IU, preparedIU);
		}

		if (!parameters.containsKey(PARM_ARTIFACT_LOCATION) && artifactKey != null) {
			File fileLocation = Util.getArtifactFile(agent, artifactKey, profile);
			if (fileLocation != null && fileLocation.exists())
				parameters.put(PARM_ARTIFACT_LOCATION, fileLocation.getAbsolutePath());
		}
		return Status.OK_STATUS;
	}

	public IInstallableUnit prepareIU(IProvisioningAgent agent, IProfile profile, IInstallableUnit iu, IArtifactKey artifactKey) {
		IInstallableUnit preparedIU = getPreparedIU(profile, iu);
		if (preparedIU != null)
			return preparedIU;

		Class<?> c = null;
		try {
			c = Class.forName("org.eclipse.equinox.p2.publisher.eclipse.BundlesAction"); //$NON-NLS-1$
			if (c != null)
				c = Class.forName("org.eclipse.osgi.service.resolver.PlatformAdmin"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			LogHelper.log(Util.createError(NLS.bind(Messages.publisher_not_available, e.getMessage()), e));
			return null;
		}

		if (c != null) {
			if (artifactKey == null)
				return iu;

			File bundleFile = Util.getArtifactFile(agent, artifactKey, profile);
			if (bundleFile == null) {
				LogHelper.log(Util.createError(NLS.bind(Messages.artifact_file_not_found, artifactKey.toString())));
				return null;
			}
			preparedIU = PublisherUtil.createBundleIU(artifactKey, bundleFile);
			if (preparedIU == null) {
				LogHelper.log(Util.createError("The bundle manifest could not be read: " + bundleFile.toString())); //$NON-NLS-1$
				return null;
			}
			savePreparedIU(profile, preparedIU);
			return preparedIU;
		}

		// should not occur
		throw new IllegalStateException(Messages.unexpected_prepareiu_error);
	}

	private IStatus validateProfile(IProfile profile) {
		// by default we validate
		if (Boolean.FALSE.toString().equals(profile.getProperty(VALIDATE_PROFILE)))
			return Status.OK_STATUS;

		Class<?> c = null;
		try {
			c = Class.forName("org.eclipse.equinox.p2.planner.IPlanner"); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			//ignore and proceed without validation
			return null;
		}

		if (c != null) {
			return DirectorUtil.validateProfile(profile);
		}
		return null;
	}
}
