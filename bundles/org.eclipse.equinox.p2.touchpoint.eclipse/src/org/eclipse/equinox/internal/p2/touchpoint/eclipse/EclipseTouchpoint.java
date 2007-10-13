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
	private final static String ID = "org.eclipse.equinox.p2.touchpoint.eclipse"; //$NON-NLS-1$
	private final static String CONFIG_FOLDER = "eclipse.configurationFolder";
	private final static String CACHE_PATH = "eclipse.p2.cache";

	//	private final static String CONFIGURATION_DATA = "configurationData";
	//	private final static String UNCONFIGURATION_DATA = "unconfigurationData";

	//	private static final boolean DEBUG = false;
	static int iuCount = 0;

	private final static String FILTER_OBJECTCLASS = "(" + Constants.OBJECTCLASS + "=" + FrameworkAdmin.class.getName() + ")";
	private final static String filterFwName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME + "=Equinox)";
	private final static String filterLauncherName = "(" + FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME + "=Eclipse.exe)";
	private final static String filterFwAdmin = "(&" + FILTER_OBJECTCLASS + filterFwName + filterLauncherName + ")";

	private ServiceTracker fwAdminTracker;
	private Map lastModifiedMap = new HashMap();
	private Map manipulatorMap = new HashMap();

	//	TODO Need to find a better way  keep track of this information, is it a generalized cache mechanism? 
	// moreover there may scenarios where the configuration data is not stored in the same IU than the bundle referring to the artifact
	private final Set supportedPhases = new HashSet(Arrays.asList(new String[] {"collect", "install", "uninstall"})); //TODO This should probably come from XML

	public ProvisioningAction getAction(String actionId) {
		if (actionId.equals("installBundle")) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return installBundle(parameters);
				}

				public IStatus undo(Map parameters) {
					return uninstallBundle(parameters);
				}
			};
		}

		if (actionId.equals("uninstallBundle")) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return uninstallBundle(parameters);
				}

				public IStatus undo(Map parameters) {
					return installBundle(parameters);
				}
			};
		}

		if (actionId.equals("collect")) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Profile profile = (Profile) parameters.get("profile");
					Operand operand = (Operand) parameters.get("operand");
					IArtifactRequest[] requests = collect(operand.second(), profile);

					Collection artifactRequests = (Collection) parameters.get("artifactRequests");
					artifactRequests.add(requests);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					// nothing to do for now
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals("addProgramArg")) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get("manipulator");
					String programArg = (String) parameters.get("programArg");

					if (programArg.equals("@artifact")) {
						Profile profile = (Profile) parameters.get("profile");
						IInstallableUnit iu = (IInstallableUnit) parameters.get("iu");
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
					Manipulator manipulator = (Manipulator) parameters.get("manipulator");
					String programArg = (String) parameters.get("programArg");
					manipulator.getLauncherData().removeProgramArg(programArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals("setFwDependentProp")) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get("manipulator");
					String propName = (String) parameters.get("propName");
					String propValue = (String) parameters.get("propValue");
					manipulator.getConfigData().setFwDependentProp(propName, propValue);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get("manipulator");
					String propName = (String) parameters.get("propName");
					manipulator.getConfigData().setFwDependentProp(propName, null); // save data?
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals("setFwIndependentProp")) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get("manipulator");
					String propName = (String) parameters.get("propName");
					String propValue = (String) parameters.get("propValue");
					manipulator.getConfigData().setFwIndependentProp(propName, propValue);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get("manipulator");
					String propName = (String) parameters.get("propName");
					manipulator.getConfigData().setFwIndependentProp(propName, null); // save data?
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals("addJvmArg")) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get("manipulator");
					String jvmArg = (String) parameters.get("jvmArg");
					manipulator.getLauncherData().addJvmArg(jvmArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get("manipulator");
					String jvmArg = (String) parameters.get("jvmArg");
					manipulator.getLauncherData().removeJvmArg(jvmArg);
					return Status.OK_STATUS;
				}
			};
		}

		//		if (actionId.equals("OLDinstall")) {
		//			return new ProvisioningAction() {
		//				public IStatus execute(Map parameters) {
		//					Profile profile = (Profile) parameters.get("profile");
		//					Operand operand = (Operand) parameters.get("operand");
		//					return configure(operand.second(), profile, true, parameters);
		//				}
		//
		//				public IStatus undo(Map parameters) {
		//					Profile profile = (Profile) parameters.get("profile");
		//					Operand operand = (Operand) parameters.get("operand");
		//					return configure(operand.second(), profile, false, parameters);
		//				}
		//			};
		//		}

		return null;
	}

	protected IStatus uninstallBundle(Map parameters) {
		Profile profile = (Profile) parameters.get("profile");
		IInstallableUnit iu = (IInstallableUnit) parameters.get("iu");
		Manipulator manipulator = (Manipulator) parameters.get("manipulator");
		String bundleId = (String) parameters.get("bundle");

		//TODO: eventually remove this. What is a fragment doing here??
		if (iu.isFragment()) {
			System.out.println("What is a fragment doing here!!! -- " + iu);
			return Status.OK_STATUS;
		}

		IArtifactKey[] artifacts = iu.getArtifacts();
		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].getId().equals(bundleId)) {
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

	private static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	// TODO: Here we may want to consult multiple caches
	private IArtifactRequest[] collect(IInstallableUnit installableUnit, Profile profile) {
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

	private boolean isCompletelyInRepo(IArtifactRepository repo, IArtifactKey[] toDownload) {
		for (int i = 0; i < toDownload.length; i++) {
			if (!repo.contains(toDownload[i]))
				return false;
		}
		return true;
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

	private boolean isZipped(TouchpointData[] data) {
		if (data == null || data.length == 0)
			return false;
		for (int i = 0; i < data.length; i++) {
			if (data[i].getInstructions("zipped") != null)
				return true;
		}
		return false;
	}

	private IStatus installBundle(Map parameters) {

		Profile profile = (Profile) parameters.get("profile");
		IInstallableUnit iu = (IInstallableUnit) parameters.get("iu");
		Manipulator manipulator = (Manipulator) parameters.get("manipulator");
		String bundleId = (String) parameters.get("bundle");
		String startLevel = (String) parameters.get("startLevel");
		String started = (String) parameters.get("markStarted");

		//TODO: eventually remove this. What is a fragment doing here??
		if (iu.isFragment()) {
			System.out.println("What is a fragment doing here!!! -- " + iu);
			return Status.OK_STATUS;
		}

		IArtifactKey[] artifacts = iu.getArtifacts();
		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].getId().equals(bundleId)) {
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

		if (startLevel != null)
			bundleInfo.setStartLevel(Integer.parseInt(startLevel));

		if (started != null)
			bundleInfo.setMarkedAsStarted(Boolean.valueOf(started).booleanValue());
		manipulator.getConfigData().addBundle(bundleInfo);

		return Status.OK_STATUS;
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
			ManifestElement[] element = ManifestElement.parseHeader("bsn", (String) headers.get(Constants.BUNDLE_SYMBOLICNAME));
			bundleInfo.setSymbolicName(element[0].getValue());
			bundleInfo.setVersion((String) headers.get(Constants.BUNDLE_VERSION));
		} catch (BundleException e) {
			e.printStackTrace();
		}
		return bundleInfo;
	}

	private String getManifest(TouchpointData[] data) {
		for (int i = 0; i < data.length; i++) {
			String manifest = data[i].getInstructions("manifest");
			if (manifest != null)
				return manifest;
		}
		return null;
	}

	private File getBundleFile(IArtifactKey artifactKey, boolean isZipped, Profile profile) throws IOException {

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

		File bundleFolder = new File(getBundlePoolLocation(profile).getFile(), "/plugins/" + artifactKey.getId() + '_' + artifactKey.getVersion());
		if (bundleFolder.exists())
			return bundleFolder;

		if (!bundleFolder.mkdir())
			throw new IOException("Can't create the folder: " + bundleFolder);

		FileUtils.unzipFile(bundleJar, bundleFolder.getParentFile());
		return bundleFolder;
	}

	//	private IStatus configure(IInstallableUnit unit, Profile profile, boolean isInstall, Map parameters) {
	//		if (unit.isFragment())
	//			return Status.OK_STATUS;
	//
	//		// Construct and initialize the java script context
	//		Context cx = Context.enter();
	//		Scriptable scope = cx.initStandardObjects();
	//
	//		Manipulator manipulator = (Manipulator) parameters.get("manipulator");
	//		// wrap the manipulator for the configuration in the profile
	//		Object wrappedOut = Context.javaToJS(manipulator, scope);
	//		ScriptableObject.putProperty(scope, "manipulator", wrappedOut);
	//
	//		// Get the touchpoint data from the installable unit
	//		TouchpointData[] touchpointData = unit.getTouchpointData();
	//
	//		if (touchpointData.length > 0 && unit.getArtifacts() != null && unit.getArtifacts().length > 0) {
	//			boolean zippedPlugin = isZipped(touchpointData);
	//			boolean alreadyInCache = false;
	//
	//			//Always try to check in the cache first
	//			IArtifactRepository baseRepo = getBundlePoolRepo(profile);
	//			IFileArtifactRepository repoToCheck = (IFileArtifactRepository) baseRepo.getAdapter(IFileArtifactRepository.class);
	//			IArtifactKey artifactKey = unit.getArtifacts()[0];
	//			File fileLocation = repoToCheck.getArtifactFile(artifactKey);
	//			if (fileLocation != null) {
	//				alreadyInCache = true;
	//			} else if (zippedPlugin) {
	//				baseRepo = getDownloadCacheRepo();
	//				repoToCheck = (IFileArtifactRepository) baseRepo.getAdapter(IFileArtifactRepository.class);
	//				fileLocation = repoToCheck.getArtifactFile(artifactKey);
	//			}
	//
	//			// TODO: Needs fixing - See Bug 204161 
	//			if (fileLocation != null) {
	//				if (!fileLocation.exists())
	//					return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
	//			} else if (isInstall) {
	//				return new Status(IStatus.ERROR, ID, "The artifact " + artifactKey.toString() + " to install has not been found.");
	//			}
	//
	//			// TODO: Here we unzip the plug-in.This is ugly. We need to deal with addition into the plug-in cache
	//			// TODO: Will we ever need to unzip in order to remove a bundle?
	//			if (!alreadyInCache && zippedPlugin) {
	//				if (isInstall) {
	//					File extractionFolder = new File(getBundlePoolLocation(profile).getFile(), "/plugins/" + artifactKey.getId() + '_' + artifactKey.getVersion());
	//					if (!extractionFolder.exists()) {
	//						if (!extractionFolder.mkdir())
	//							return new Status(IStatus.ERROR, ID, "can't create the folder: " + extractionFolder);
	//						try {
	//							FileUtils.unzipFile(fileLocation, extractionFolder.getParentFile());
	//						} catch (IOException e) {
	//							return new Status(IStatus.ERROR, ID, "can't extract " + fileLocation + " into the folder " + extractionFolder);
	//						}
	//					}
	//					fileLocation = extractionFolder;
	//				} else {
	//					fileLocation = new File(new File(getBundlePoolLocation(profile).getFile()), "/plugins/" + artifactKey.getId() + '_' + artifactKey.getVersion());
	//				}
	//				//check if the target folder exists
	//
	//				//if it does then stop
	//				//if it does not create the folder and extract the archive... Be careful here with the permissions.... We may need to have a proper unzip technology here that supports file permissions for linux
	//				//				request.getProfile().getValue(CACHE_PATH);
	//			}
	//
	//			// TODO: We need to separate install and uninstall actions
	//			if (!isInstall && fileLocation == null)
	//				return new Status(IStatus.ERROR, ID, "The artifact " + artifactKey.toString() + " to uninstall has not been found.");
	//
	//			ScriptableObject.putProperty(scope, "artifact", fileLocation.getAbsolutePath());
	//			BundleInfo bundle = new BundleInfo();
	//			try {
	//				bundle.setLocation(fileLocation.toURL().toExternalForm());
	//			} catch (MalformedURLException e) {
	//				// Ignore;
	//				e.printStackTrace();
	//			}
	//			String[] manifestData = getInstructionsFor("manifest", touchpointData); //TODO Here we only take one set of manifest data
	//			this.initFromManifest(manifestData[0], bundle);
	//			ScriptableObject.putProperty(scope, (isInstall ? "bundleToInstall" : "bundleToRemove"), bundle);
	//		}
	//
	//		String[] instructions = getInstructionsFor((isInstall ? CONFIGURATION_DATA : UNCONFIGURATION_DATA), touchpointData);
	//		for (int i = 0; i < instructions.length; i++) {
	//			logConfiguation(unit, instructions[i], isInstall);
	//			try {
	//				cx.evaluateString(scope, instructions[i], unit.getId(), 1, null);
	//				//TODO Need to get the result of the operations
	//			} catch (RuntimeException ex) {
	//				return new Status(IStatus.ERROR, Activator.ID, "Exception while executing " + instructions[i], ex);
	//			}
	//		}
	//		return Status.OK_STATUS;
	//	}

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

	public void initFromManifest(String manifest, BundleInfo bInfo) {
		try {
			bInfo.setManifest(manifest);
			Headers headers = Headers.parseManifest(new ByteArrayInputStream(manifest.getBytes()));
			ManifestElement[] element = ManifestElement.parseHeader("bsn", (String) headers.get(Constants.BUNDLE_SYMBOLICNAME));
			bInfo.setSymbolicName(element[0].getValue());
			bInfo.setVersion((String) headers.get(Constants.BUNDLE_VERSION));
		} catch (BundleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Manipulator getFrameworkManipulator(Profile profile) {
		FrameworkAdmin fwAdmin = getFrameworkAdmin();
		return fwAdmin == null ? null : fwAdmin.getManipulator();
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

	//	private String[] getInstructionsFor(String key, TouchpointData[] data) {
	//		String[] matches = new String[data.length];
	//		int count = 0;
	//		for (int i = 0; i < data.length; i++) {
	//			matches[count] = data[i].getInstructions(key);
	//			if (matches[count] != null)
	//				count++;
	//		}
	//		if (count == data.length)
	//			return matches;
	//		String[] result = new String[count];
	//		System.arraycopy(matches, 0, result, 0, count);
	//		return result;
	//	}

	public TouchpointType getTouchpointType() {
		return new TouchpointType("eclipse", new Version("1.0")); //TODO this data probably needs to come from the XML
	}

	public boolean supports(String phaseID) { //TODO this data probably needs to come from the XML
		return supportedPhases.contains(phaseID);
	}

	private File getInstallFolder(Profile profile) {
		return new File(profile.getValue(Profile.PROP_INSTALL_FOLDER));
	}

	private File getConfigurationFolder(Profile profile) {
		String config = profile.getValue(CONFIG_FOLDER);
		if (config != null)
			return new File(config);
		return new File(getInstallFolder(profile), "configuration"); //$NON-NLS-1$
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

	static private IFileArtifactRepository getBundlePoolRepo(Profile profile) {
		URL location = getBundlePoolLocation(profile);
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository == null) {
			// 	the given repo location is not an existing repo so we have to create something
			// TODO for now create a random repo by default.
			String repositoryName = location + " - bundle pool"; //$NON-NLS-1$
			repository = manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository");
			// TODO: do we still need to do this
			tagAsImplementation(repository);
		}

		IFileArtifactRepository bundlePool = (IFileArtifactRepository) repository.getAdapter(IFileArtifactRepository.class);
		if (bundlePool == null) {
			throw new IllegalArgumentException("BundlePool repository not writeable: " + location); //$NON-NLS-1$
		}
		return bundlePool;
	}

	static private URL getBundlePoolLocation(Profile profile) {
		String path = profile.getValue(CACHE_PATH);
		if (path == null)
			path = Activator.getContext().getProperty(CACHE_PATH);
		if (path != null)
			try {
				// TODO this is a hack for now.
				return File.separatorChar == '/' ? new URL("file:" + path) : new URL("file:/" + path);
			} catch (MalformedURLException e) {
				// TODO Do nothing and use the default approach
			}
		AgentLocation location = getAgentLocation();
		if (location == null)
			return null;
		URL result = location.getTouchpointDataArea("org.eclipse.equinox.p2.touchpoint.eclipse/");
		try {
			return new URL(result, "bundlepool");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}

	static private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	static private IFileArtifactRepository getDownloadCacheRepo() {
		URL location = getDownloadCacheLocation();
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository == null) {
			// 	the given repo location is not an existing repo so we have to create something
			// TODO for now create a random repo by default.
			String repositoryName = location + " - Agent download cache"; //$NON-NLS-1$
			repository = manager.createRepository(location, repositoryName, "org.eclipse.equinox.p2.artifact.repository.simpleRepository");
			// TODO: do we still need to do this
			tagAsImplementation(repository);
		}

		IFileArtifactRepository downloadCache = (IFileArtifactRepository) repository.getAdapter(IFileArtifactRepository.class);
		if (downloadCache == null) {
			throw new IllegalArgumentException("Agent download cache not writeable: " + location); //$NON-NLS-1$
		}
		return downloadCache;
	}

	static private URL getDownloadCacheLocation() {
		AgentLocation location = getAgentLocation();
		return (location != null ? location.getArtifactRepositoryURL() : null);
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

	public IStatus completePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		Manipulator manipulator = (Manipulator) touchpointParameters.get("manipulator");
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

	public IStatus initializePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		Manipulator manipulator = null;
		try {
			manipulator = getManipulator(profile);
		} catch (CoreException ce) {
			return ce.getStatus();
		}
		//TODO These values should be inserted by a configuration unit (bug 204124)
		manipulator.getConfigData().setFwDependentProp("eclipse.p2.profile", profile.getProfileId());
		manipulator.getConfigData().setFwDependentProp("eclipse.p2.data.area", computeRelativeAgentLocation(profile));
		touchpointParameters.put("manipulator", manipulator);
		touchpointParameters.put("installFolder", getInstallFolder(profile));
		return null;
	}
}
