/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.helpers.*;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class EclipseTouchpoint extends Touchpoint {
	private static final String ACTION_ADD_JVM_ARG = "addJvmArg"; //$NON-NLS-1$
	private static final String ACTION_ADD_PROGRAM_ARG = "addProgramArg";
	private static final String ACTION_COLLECT = "collect"; //$NON-NLS-1$
	private static final String ACTION_INSTALL_BUNDLE = "installBundle"; //$NON-NLS-1$
	private static final String ACTION_MARK_STARTED = "markStarted"; //$NON-NLS-1$
	private static final String ACTION_REMOVE_JVM_ARG = "removeJvmArg"; //$NON-NLS-1$
	private static final String ACTION_REMOVE_PROGRAM_ARG = "removeProgramArg"; //$NON-NLS-1$
	private static final String ACTION_SET_FW_DEPENDENT_PROP = "setFwDependentProp"; //$NON-NLS-1$
	private static final String ACTION_SET_FW_INDEPENDENT_PROP = "setFwIndependentProp"; //$NON-NLS-1$
	private static final String ACTION_UNINSTALL_BUNDLE = "uninstallBundle"; //$NON-NLS-1$
	private final static String CACHE_PATH = "eclipse.p2.cache"; //$NON-NLS-1$
	private final static String CONFIG_FOLDER = "eclipse.configurationFolder"; //$NON-NLS-1$
	private final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + '=' + FrameworkAdmin.class.getName() + ')'; //$NON-NLS-1$

	private final static String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)"; //$NON-NLS-1$ //$NON-NLS-2$
	private final static String filterFwAdmin = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ')'; //$NON-NLS-1$;
	private final static String ID = "org.eclipse.equinox.p2.touchpoint.eclipse"; //$NON-NLS-1$

	private static final String PARM_ARTIFACT = "@artifact"; //$NON-NLS-1$
	private static final String PARM_ARTIFACT_REQUESTS = "artifactRequests"; //$NON-NLS-1$
	private static final String PARM_BUNDLE = "bundle"; //$NON-NLS-1$
	private static final String PARM_INSTALL_FOLDER = "installFolder"; //$NON-NLS-1$
	private static final String PARM_IU = "iu"; //$NON-NLS-1$
	private static final String PARM_JVM_ARG = "jvmArg"; //$NON-NLS-1$
	private static final String PARM_MANIPULATOR = "manipulator"; //$NON-NLS-1$
	private static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
	private static final String PARM_PREVIOUS_START_LEVEL = "previousStartLevel"; //$NON-NLS-1$
	private static final String PARM_PREVIOUS_STARTED = "previousStarted"; //$NON-NLS-1$
	private static final String PARM_PREVIOUS_VALUE = "previousValue"; //$NON-NLS-1$
	private static final String PARM_PROFILE = "profile"; //$NON-NLS-1$
	private static final String PARM_PROGRAM_ARG = "programArg"; //$NON-NLS-1$
	private static final String PARM_PROP_NAME = "propName"; //$NON-NLS-1$
	private static final String PARM_PROP_VALUE = "propValue"; //$NON-NLS-1$
	private static final String PARM_SET_START_LEVEL = "setStartLevel"; //$NON-NLS-1$
	private static final String PARM_START_LEVEL = "startLevel"; //$NON-NLS-1$
	private static final String PARM_STARTED = "started"; //$NON-NLS-1$

	private ServiceTracker fwAdminTracker;
	private Map lastModifiedMap = new HashMap();
	private Map manipulatorMap = new HashMap();

	private static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	static private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	static private URL getBundlePoolLocation(Profile profile) {
		String path = profile.getValue(CACHE_PATH);
		if (path == null)
			path = Activator.getContext().getProperty(CACHE_PATH);
		if (path != null)
			try {
				// TODO this is a hack for now.
				return File.separatorChar == '/' ? new URL("file:" + path) : new URL("file:/" + path); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (MalformedURLException e) {
				// TODO Do nothing and use the default approach
			}
		AgentLocation location = getAgentLocation();
		if (location == null)
			return null;
		//TODO This should match the touchpoint id
		URL result = location.getTouchpointDataArea("org.eclipse.equinox.p2.touchpoint.eclipse/"); //$NON-NLS-1$
		try {
			return new URL(result, "bundlepool"); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}

	static private IFileArtifactRepository getBundlePoolRepo(Profile profile) {
		URL location = getBundlePoolLocation(profile);
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository == null) {
			// 	the given repo location is not an existing repo so we have to create something
			// TODO for now create a random repo by default.
			String repositoryName = location + " - bundle pool"; //$NON-NLS-1$
			repository = manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
			// TODO: do we still need to do this
			tagAsImplementation(repository);
		}

		IFileArtifactRepository bundlePool = (IFileArtifactRepository) repository.getAdapter(IFileArtifactRepository.class);
		if (bundlePool == null) {
			throw new IllegalArgumentException("BundlePool repository not writeable: " + location); //$NON-NLS-1$
		}
		return bundlePool;
	}

	static private URL getDownloadCacheLocation() {
		AgentLocation location = getAgentLocation();
		return (location != null ? location.getArtifactRepositoryURL() : null);
	}

	static private IFileArtifactRepository getDownloadCacheRepo() {
		URL location = getDownloadCacheLocation();
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository == null) {
			// 	the given repo location is not an existing repo so we have to create something
			// TODO for now create a random repo by default.
			String repositoryName = location + " - Agent download cache"; //$NON-NLS-1$
			repository = manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$
			// TODO: do we still need to do this
			tagAsImplementation(repository);
		}

		IFileArtifactRepository downloadCache = (IFileArtifactRepository) repository.getAdapter(IFileArtifactRepository.class);
		if (downloadCache == null) {
			throw new IllegalArgumentException("Agent download cache not writeable: " + location); //$NON-NLS-1$
		}
		return downloadCache;
	}

	// TODO: Will there be other repositories to tag as implementation?  Should this
	//		 method to some utility?
	static private void tagAsImplementation(IArtifactRepository repository) {
		//		if (repository != null && repository.getProperties().getProperty(IRepositoryInfo.IMPLEMENTATION_ONLY_KEY) == null) {
		//			IWritableRepositoryInfo writableInfo = (IWritableRepositoryInfo) repository.getAdapter(IWritableRepositoryInfo.class);
		//			if (writableInfo != null) {
		//				writableInfo.getModifiableProperties().setProperty(IRepositoryInfo.IMPLEMENTATION_ONLY_KEY, Boolean.valueOf(true).toString());
		//			}
		//		}
	}

	// TODO: Here we may want to consult multiple caches
	IArtifactRequest[] collect(IInstallableUnit installableUnit, Profile profile) {
		IArtifactRepository targetRepo = null;
		IArtifactKey[] toDownload = installableUnit.getArtifacts();
		if (toDownload == null)
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;
		IArtifactRequest[] requests = new IArtifactRequest[toDownload.length];

		IFileArtifactRepository bundlePool = getBundlePoolRepo(profile);
		if (isCompletelyInRepo(bundlePool, toDownload))
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;

		//If the installable unit has installation information, then the artifact is put in the download cache
		//otherwise it is a jar'ed bundle and we directly store it in the plugin cache
		if (isZipped(installableUnit.getTouchpointData())) {
			targetRepo = getDownloadCacheRepo();
		} else {
			targetRepo = bundlePool;
		}
		int count = 0;
		for (int i = 0; i < toDownload.length; i++) {
			IArtifactKey key = toDownload[i];
			if (!targetRepo.contains(key)) {
				requests[count++] = getArtifactRepositoryManager().createMirrorRequest(key, targetRepo);
			}
		}

		if (requests.length == count)
			return requests;
		IArtifactRequest[] result = new IArtifactRequest[count];
		System.arraycopy(requests, 0, result, 0, count);
		return result;
	}

	public IStatus completePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		Manipulator manipulator = (Manipulator) touchpointParameters.get(PARM_MANIPULATOR);
		try {
			manipulator.save(false);
			lastModifiedMap.put(getConfigurationFolder(profile), new Long(manipulator.getTimeStamp()));
		} catch (RuntimeException e) {
			return new Status(IStatus.ERROR, Activator.ID, 1, "Error saving manipulator", e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, 1, "Error saving manipulator", e);
		}
		return null;
	}

	/**
	 * Returns the agent location, if possible as a path relative to the configuration
	 * directory using the @config.dir substitution variable. AgentLocation will
	 * substitute this variable with the configuration folder location on startup.
	 * If the agent location is not a sub-directory of the configuration folder, this
	 * method simply returns the absolute agent location expressed as a URL.
	 */
	private String computeRelativeAgentLocation(Profile profile) {
		URL agentURL = getAgentLocation().getURL();
		//TODO handle proper path/url conversion
		IPath agentPath = new Path(agentURL.getPath());
		IPath configPath = new Path(getConfigurationFolder(profile).getAbsolutePath());
		if (configPath.isPrefixOf(agentPath))
			return "@config.dir/" + agentPath.removeFirstSegments(configPath.segmentCount()).makeRelative().setDevice(null); //$NON-NLS-1$
		return agentURL.toString();
	}

	private BundleInfo createBundleInfo(File bundleFile, String manifest) {
		BundleInfo bundleInfo = new BundleInfo();
		try {
			bundleInfo.setLocation(bundleFile.toURL().toExternalForm());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		bundleInfo.setManifest(manifest);
		try {
			Headers headers = Headers.parseManifest(new ByteArrayInputStream(manifest.getBytes()));
			ManifestElement[] element = ManifestElement.parseHeader("bsn", (String) headers.get(Constants.BUNDLE_SYMBOLICNAME)); //$NON-NLS-1$
			bundleInfo.setSymbolicName(element[0].getValue());
			bundleInfo.setVersion((String) headers.get(Constants.BUNDLE_VERSION));
		} catch (BundleException e) {
			e.printStackTrace();
		}
		return bundleInfo;
	}

	public ProvisioningAction getAction(String actionId) {
		if (actionId.equals(ACTION_INSTALL_BUNDLE)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return installBundle(parameters);
				}

				public IStatus undo(Map parameters) {
					return uninstallBundle(parameters);
				}
			};
		}

		if (actionId.equals(ACTION_UNINSTALL_BUNDLE)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return uninstallBundle(parameters);
				}

				public IStatus undo(Map parameters) {
					return installBundle(parameters);
				}
			};
		}

		if (actionId.equals(ACTION_COLLECT)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Profile profile = (Profile) parameters.get(PARM_PROFILE);
					Operand operand = (Operand) parameters.get(PARM_OPERAND);
					IArtifactRequest[] requests = collect(operand.second(), profile);

					Collection artifactRequests = (Collection) parameters.get(PARM_ARTIFACT_REQUESTS);
					artifactRequests.add(requests);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					// nothing to do for now
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_ADD_PROGRAM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey artifactKey = iu.getArtifacts()[0];

						File fileLocation = null;
						try {
							fileLocation = getBundleFile(artifactKey, isZipped(iu.getTouchpointData()), profile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!fileLocation.exists())
							return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().addProgramArg(programArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey artifactKey = iu.getArtifacts()[0];

						File fileLocation = null;
						try {
							fileLocation = getBundleFile(artifactKey, isZipped(iu.getTouchpointData()), profile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!fileLocation.exists())
							return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().removeProgramArg(programArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_REMOVE_PROGRAM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey artifactKey = iu.getArtifacts()[0];

						File fileLocation = null;
						try {
							fileLocation = getBundleFile(artifactKey, isZipped(iu.getTouchpointData()), profile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!fileLocation.exists())
							return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().removeProgramArg(programArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey artifactKey = iu.getArtifacts()[0];

						File fileLocation = null;
						try {
							fileLocation = getBundleFile(artifactKey, isZipped(iu.getTouchpointData()), profile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!fileLocation.exists())
							return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().addProgramArg(programArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(PARM_SET_START_LEVEL)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
					String startLevel = (String) parameters.get(PARM_START_LEVEL);

					BundleInfo bundleInfo = new BundleInfo();
					initFromManifest(getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							getMemento().put(PARM_PREVIOUS_START_LEVEL, new Integer(bundles[i].getStartLevel()));
							bundles[i].setStartLevel(Integer.parseInt(startLevel));
							break;
						}
					}
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);

					BundleInfo bundleInfo = new BundleInfo();
					initFromManifest(getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							Integer previousStartLevel = (Integer) getMemento().get(PARM_PREVIOUS_START_LEVEL);
							if (previousStartLevel != null)
								bundles[i].setStartLevel(previousStartLevel.intValue());
							break;
						}
					}
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_MARK_STARTED)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
					String started = (String) parameters.get(PARM_STARTED);

					BundleInfo bundleInfo = new BundleInfo();
					initFromManifest(getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							getMemento().put(PARM_PREVIOUS_STARTED, new Boolean(bundles[i].isMarkedAsStarted()));
							bundles[i].setMarkedAsStarted(Boolean.valueOf(started).booleanValue());
							break;
						}
					}
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);

					BundleInfo bundleInfo = new BundleInfo();
					initFromManifest(getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							Boolean previousStarted = (Boolean) getMemento().get(PARM_PREVIOUS_STARTED);
							if (previousStarted != null)
								bundles[i].setMarkedAsStarted(previousStarted.booleanValue());
							break;
						}
					}
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_SET_FW_DEPENDENT_PROP)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					String propValue = (String) parameters.get(PARM_PROP_VALUE);
					getMemento().put(PARM_PREVIOUS_VALUE, manipulator.getConfigData().getFwDependentProp(propName));
					manipulator.getConfigData().setFwDependentProp(propName, propValue);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					String previousValue = (String) getMemento().get(PARM_PREVIOUS_VALUE);
					manipulator.getConfigData().setFwDependentProp(propName, previousValue);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_SET_FW_INDEPENDENT_PROP)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					String propValue = (String) parameters.get(PARM_PROP_VALUE);
					getMemento().put(PARM_PREVIOUS_VALUE, manipulator.getConfigData().getFwDependentProp(propName));
					manipulator.getConfigData().setFwIndependentProp(propName, propValue);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					String previousValue = (String) getMemento().get(PARM_PREVIOUS_VALUE);
					manipulator.getConfigData().setFwIndependentProp(propName, previousValue);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_ADD_JVM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					manipulator.getLauncherData().addJvmArg(jvmArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					manipulator.getLauncherData().removeJvmArg(jvmArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_REMOVE_JVM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					manipulator.getLauncherData().removeJvmArg(jvmArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					manipulator.getLauncherData().addJvmArg(jvmArg);
					return Status.OK_STATUS;
				}
			};
		}

		return null;
	}

	File getBundleFile(IArtifactKey artifactKey, boolean isZipped, Profile profile) throws IOException {

		if (!isZipped) {
			IFileArtifactRepository bundlePool = getBundlePoolRepo(profile);
			File bundleJar = bundlePool.getArtifactFile(artifactKey);
			return bundleJar;
		}

		// Handle zipped
		IFileArtifactRepository downloadCache = getDownloadCacheRepo();
		File bundleJar = downloadCache.getArtifactFile(artifactKey);
		if (bundleJar == null)
			return null;

		File bundleFolder = new File(getBundlePoolLocation(profile).getFile(), "/plugins/" + artifactKey.getId() + '_' + artifactKey.getVersion()); //$NON-NLS-1$
		if (bundleFolder.exists())
			return bundleFolder;

		if (!bundleFolder.mkdir())
			throw new IOException("Can't create the folder: " + bundleFolder);

		FileUtils.unzipFile(bundleJar, bundleFolder);
		return bundleFolder;
	}

	private File getConfigurationFolder(Profile profile) {
		String config = profile.getValue(CONFIG_FOLDER);
		if (config != null)
			return new File(config);
		return new File(getInstallFolder(profile), "configuration"); //$NON-NLS-1$
	}

	private FrameworkAdmin getFrameworkAdmin() {
		if (fwAdminTracker == null) {
			Filter filter;
			try {
				filter = Activator.getContext().createFilter(filterFwAdmin);
				fwAdminTracker = new ServiceTracker(Activator.getContext(), filter, null);
				fwAdminTracker.open();
			} catch (InvalidSyntaxException e) {
				// never happens
				e.printStackTrace();
			}
		}
		return (FrameworkAdmin) fwAdminTracker.getService();
	}

	private Manipulator getFrameworkManipulator(Profile profile) {
		FrameworkAdmin fwAdmin = getFrameworkAdmin();
		return fwAdmin == null ? null : fwAdmin.getManipulator();
	}

	private File getInstallFolder(Profile profile) {
		return new File(profile.getValue(Profile.PROP_INSTALL_FOLDER));
	}

	/**
	 * Returns the name of the Eclipse application launcher.
	 */
	private String getLauncherName(Profile profile) {
		String name = profile.getValue(FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME);
		if (name != null)
			return name;
		//create a default name based on platform
		//TODO Need a better solution for launcher name branding
		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
		if (info.getOS() == org.eclipse.osgi.service.environment.Constants.OS_WIN32)
			return "eclipse.exe"; //$NON-NLS-1$
		return "eclipse"; //$NON-NLS-1$
	}

	String getManifest(TouchpointData[] data) {
		for (int i = 0; i < data.length; i++) {
			String manifest = data[i].getInstructions("manifest"); //$NON-NLS-1$
			if (manifest != null)
				return manifest;
		}
		return null;
	}

	//TODO We need to find a way to clean those caches. Maybe in response to a lifecycle post install.
	private Manipulator getManipulator(Profile profile) throws CoreException {
		File configurationFolder = getConfigurationFolder(profile);
		File installFolder = getInstallFolder(profile);
		Manipulator manipulator = (Manipulator) manipulatorMap.get(configurationFolder);
		if (manipulator == null) {
			manipulator = getFrameworkManipulator(profile);
			if (manipulator != null) {
				manipulatorMap.put(configurationFolder, manipulator);
			} else
				throw new CoreException(new Status(IStatus.ERROR, ID, "Could not acquire the framework manipulator service."));
		}

		Long lastModified = (Long) lastModifiedMap.get(configurationFolder);
		if (lastModified == null || lastModified.longValue() < manipulator.getTimeStamp()) {
			LauncherData launcherData = manipulator.getLauncherData();
			launcherData.setFwConfigLocation(configurationFolder);
			launcherData.setLauncher(new File(installFolder, getLauncherName(profile)));
			try {
				manipulator.load();
				lastModifiedMap.put(configurationFolder, new Long(manipulator.getTimeStamp()));
			} catch (IllegalStateException e2) {
				// TODO if fwJar is not included, this exception will be thrown. But ignore it. 
				//				e2.printStackTrace();
			} catch (FrameworkAdminRuntimeException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		return manipulator;
	}

	public TouchpointType getTouchpointType() {
		//TODO this data probably needs to come from the XML
		return new TouchpointType("eclipse", new Version("1.0")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void initFromManifest(String manifest, BundleInfo bInfo) {
		try {
			bInfo.setManifest(manifest);
			Headers headers = Headers.parseManifest(new ByteArrayInputStream(manifest.getBytes()));
			ManifestElement[] element = ManifestElement.parseHeader("bsn", (String) headers.get(Constants.BUNDLE_SYMBOLICNAME)); //$NON-NLS-1$
			bInfo.setSymbolicName(element[0].getValue());
			bInfo.setVersion((String) headers.get(Constants.BUNDLE_VERSION));
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public IStatus initializePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		Manipulator manipulator = null;
		try {
			manipulator = getManipulator(profile);
		} catch (CoreException ce) {
			return ce.getStatus();
		}
		//TODO These values should be inserted by a configuration unit (bug 204124)
		manipulator.getConfigData().setFwDependentProp("eclipse.p2.profile", profile.getProfileId()); //$NON-NLS-1$
		manipulator.getConfigData().setFwDependentProp("eclipse.p2.data.area", computeRelativeAgentLocation(profile)); //$NON-NLS-1$
		touchpointParameters.put(PARM_MANIPULATOR, manipulator);
		touchpointParameters.put(PARM_INSTALL_FOLDER, getInstallFolder(profile));
		return null;
	}

	IStatus installBundle(Map parameters) {

		Profile profile = (Profile) parameters.get(PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
		String bundleId = (String) parameters.get(PARM_BUNDLE);

		//TODO: eventually remove this. What is a fragment doing here??
		if (iu.isFragment()) {
			System.out.println("What is a fragment doing here!!! -- " + iu);
			return Status.OK_STATUS;
		}

		IArtifactKey[] artifacts = iu.getArtifacts();
		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(bundleId)) {
				artifactKey = artifacts[i];
				break;
			}
		}
		if (artifactKey == null)
			throw new IllegalArgumentException("No artifact found that matches: " + bundleId);

		boolean isZipped = isZipped(iu.getTouchpointData());
		File bundleFile;
		try {
			bundleFile = getBundleFile(artifactKey, isZipped, profile);
			if (bundleFile == null)
				return new Status(IStatus.ERROR, ID, "The artifact " + artifactKey.toString() + " to install was not found.");

		} catch (IOException e) {
			return new Status(IStatus.ERROR, ID, e.getMessage());
		}

		// TODO: do we really need the manifest here or just the bsn and version?
		String manifest = getManifest(iu.getTouchpointData());

		BundleInfo bundleInfo = createBundleInfo(bundleFile, manifest);
		manipulator.getConfigData().addBundle(bundleInfo);

		return Status.OK_STATUS;
	}

	private boolean isCompletelyInRepo(IArtifactRepository repo, IArtifactKey[] toDownload) {
		for (int i = 0; i < toDownload.length; i++) {
			if (!repo.contains(toDownload[i]))
				return false;
		}
		return true;
	}

	//	private void logConfiguation(IInstallableUnit unit, String instructions, boolean isInstall) {
	//		//  TODO: temporary for debugging; replace by logging
	//		if (DEBUG) {
	//			System.out.print("[" + iuCount + "] ");
	//			if (isInstall) {
	//				System.out.println("Installing " + unit + " with: " + instructions);
	//			} else {
	//				System.out.println("Uninstalling " + unit + " with: " + instructions);
	//			}
	//			iuCount++;
	//		}
	//	}

	boolean isZipped(TouchpointData[] data) {
		if (data == null || data.length == 0)
			return false;
		for (int i = 0; i < data.length; i++) {
			if (data[i].getInstructions("zipped") != null) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	protected IStatus uninstallBundle(Map parameters) {
		Profile profile = (Profile) parameters.get(PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
		String bundleId = (String) parameters.get(PARM_BUNDLE);

		//TODO: eventually remove this. What is a fragment doing here??
		if (iu.isFragment()) {
			System.out.println("What is a fragment doing here!!! -- " + iu);
			return Status.OK_STATUS;
		}

		IArtifactKey[] artifacts = iu.getArtifacts();
		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(bundleId)) {
				artifactKey = artifacts[i];
				break;
			}
		}
		if (artifactKey == null)
			throw new IllegalArgumentException("No artifact found that matches: " + bundleId);

		boolean isZipped = isZipped(iu.getTouchpointData());
		File bundleFile;
		try {
			bundleFile = getBundleFile(artifactKey, isZipped, profile);
			if (bundleFile == null)
				return new Status(IStatus.ERROR, ID, "The artifact " + artifactKey.toString() + " to uninstall was not found.");

		} catch (IOException e) {
			return new Status(IStatus.ERROR, ID, e.getMessage());
		}

		// TODO: do we really need the manifest here or just the bsn and version?
		String manifest = getManifest(iu.getTouchpointData());

		BundleInfo bundleInfo = createBundleInfo(bundleFile, manifest);
		manipulator.getConfigData().removeBundle(bundleInfo);

		return Status.OK_STATUS;
	}
}
