/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdminRuntimeException;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;

public class EclipseTouchpoint extends Touchpoint {

	// TODO: phase id constants should be defined elsewhere.
	public static final String INSTALL_PHASE_ID = "install"; //$NON-NLS-1$
	public static final String UNINSTALL_PHASE_ID = "uninstall"; //$NON-NLS-1$
	public static final String CONFIGURE_PHASE_ID = "configure"; //$NON-NLS-1$
	public static final String UNCONFIGURE_PHASE_ID = "unconfigure"; //$NON-NLS-1$

	public static final String PROFILE_PROP_LAUNCHER_NAME = "eclipse.touchpoint.launcherName"; //$NON-NLS-1$
	public static final String PARM_MANIPULATOR = "manipulator"; //$NON-NLS-1$
	public static final String PARM_PLATFORM_CONFIGURATION = "platformConfiguration"; //$NON-NLS-1$
	public static final String PARM_SOURCE_BUNDLES = "sourceBundles"; //$NON-NLS-1$
	public static final String PARM_IU = "iu"; //$NON-NLS-1$
	public static final String PARM_INSTALL_FOLDER = "installFolder"; //$NON-NLS-1$
	private static final String NATIVE_TOUCHPOINT_ID = "org.eclipse.equinox.p2.touchpoint.natives"; //$NON-NLS-1$
	private static List NATIVE_ACTIONS = Arrays.asList(new String[] {"chmod", "ln", "mkdir", "rmdir"}); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$

	private static Map manipulators = new WeakHashMap();
	private static Map wrappers = new WeakHashMap();
	private static Map sourceManipulators = new WeakHashMap();

	private static synchronized LazyManipulator getManipulator(IProfile profile) {
		LazyManipulator manipulator = (LazyManipulator) manipulators.get(profile);
		if (manipulator == null) {
			manipulator = new LazyManipulator(profile);
			manipulators.put(profile, manipulator);
		}
		return manipulator;
	}

	private static synchronized void saveManipulator(IProfile profile) throws FrameworkAdminRuntimeException, IOException {
		LazyManipulator manipulator = (LazyManipulator) manipulators.remove(profile);
		if (manipulator != null)
			manipulator.save(false);
	}

	private static synchronized PlatformConfigurationWrapper getPlatformConfigurationWrapper(IProfile profile, LazyManipulator manipulator) {
		PlatformConfigurationWrapper wrapper = (PlatformConfigurationWrapper) wrappers.get(profile);
		if (wrapper == null) {
			File configLocation = Util.getConfigurationFolder(profile);
			URI poolURI = Util.getBundlePoolLocation(profile);
			wrapper = new PlatformConfigurationWrapper(configLocation, poolURI, manipulator);
			wrappers.put(profile, wrapper);
		}
		return wrapper;
	}

	private static synchronized void savePlatformConfigurationWrapper(IProfile profile) throws ProvisionException {
		PlatformConfigurationWrapper wrapper = (PlatformConfigurationWrapper) wrappers.remove(profile);
		if (wrapper != null)
			wrapper.save();
	}

	private static synchronized SourceManipulator getSourceManipulator(IProfile profile) {
		SourceManipulator sourceManipulator = (SourceManipulator) sourceManipulators.get(profile);
		if (sourceManipulator == null) {
			sourceManipulator = new SourceManipulator(profile);
			sourceManipulators.put(profile, sourceManipulator);
		}
		return sourceManipulator;
	}

	private static synchronized void saveSourceManipulator(IProfile profile) throws IOException {
		SourceManipulator sourceManipulator = (SourceManipulator) sourceManipulators.remove(profile);
		if (sourceManipulator != null)
			sourceManipulator.save();
	}

	private static synchronized void clearProfileState(IProfile profile) {
		manipulators.remove(profile);
		wrappers.remove(profile);
		sourceManipulators.remove(profile);
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

	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map touchpointParameters) {
		touchpointParameters.put(PARM_INSTALL_FOLDER, Util.getInstallFolder(profile));
		LazyManipulator manipulator = getManipulator(profile);
		touchpointParameters.put(PARM_MANIPULATOR, manipulator);
		touchpointParameters.put(PARM_SOURCE_BUNDLES, getSourceManipulator(profile));
		touchpointParameters.put(PARM_PLATFORM_CONFIGURATION, getPlatformConfigurationWrapper(profile, manipulator));
		return null;
	}

	public IStatus initializeOperand(IProfile profile, Operand operand, Map parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		if (iu != null && Boolean.valueOf(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)).booleanValue()) {
			IInstallableUnit preparedIU = prepareIU(iu, profile);
			if (preparedIU == null)
				return Util.createError(NLS.bind(Messages.failed_prepareIU, iu));

			parameters.put(PARM_IU, preparedIU);
		}
		return Status.OK_STATUS;
	}

	public IInstallableUnit prepareIU(IInstallableUnit iu, IProfile profile) {

		Class c = null;
		try {
			c = Class.forName("org.eclipse.equinox.p2.publisher.eclipse.BundlesAction"); //$NON-NLS-1$
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
		PublisherInfo info = new PublisherInfo();
		info.addAdvice(new AdviceFileAdvice(new Path(bundleFile.getAbsolutePath()), AdviceFileAdvice.BUNDLE_ADVICE_FILE));
		String shape = bundleFile.isDirectory() ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
		info.addAdvice(new BundleShapeAdvice(bundleDescription.getSymbolicName(), bundleDescription.getVersion(), shape));
		return BundlesAction.createBundleIU(bundleDescription, artifactKey, info);
	}
}
