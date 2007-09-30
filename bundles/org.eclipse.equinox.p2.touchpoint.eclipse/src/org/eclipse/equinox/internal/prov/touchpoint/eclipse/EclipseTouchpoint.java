/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.prov.touchpoint.eclipse;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.prov.artifact.repository.*;
import org.eclipse.equinox.prov.core.helpers.*;
import org.eclipse.equinox.prov.core.location.AgentLocation;
import org.eclipse.equinox.prov.engine.*;
import org.eclipse.equinox.prov.metadata.*;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.mozilla.javascript.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class EclipseTouchpoint implements ITouchpoint {
	private final static String ID = "org.eclipse.equinox.prov.touchpoint.eclipse"; //$NON-NLS-1$
	private final static String CONFIG_FOLDER = "eclipse.configurationFolder";
	private final static String CACHE_PATH = "eclipse.prov.cache";

	private final static String CONFIGURATION_DATA = "configurationData";
	private final static String UNCONFIGURATION_DATA = "unconfigurationData";

	private static final boolean DEBUG = false;
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
	private final Set supportedPhases = new HashSet(); //TODO This should probably come from XML

	{
		supportedPhases.add("collect");
		supportedPhases.add("install");
		supportedPhases.add("uninstall");
	}

	public ITouchpointAction[] getActions(String phaseID, final Profile profile, final Operand operand) {
		if (phaseID.equals("collect")) {
			ITouchpointAction action = new ITouchpointAction() {
				public Object execute() {
					return collect(operand.second(), profile);
				}

				public Object undo() {
					return null;
				}
			};
			return new ITouchpointAction[] {action};
		}
		if (phaseID.equals("install")) {
			ITouchpointAction action = new ITouchpointAction() {
				public Object execute() {
					return configure(operand.second(), profile, true);
				}

				public Object undo() {
					return configure(operand.second(), profile, false);
				}
			};
			return new ITouchpointAction[] {action};
		}
		if (phaseID.equals("uninstall")) {
			ITouchpointAction action = new ITouchpointAction() {
				public Object execute() {
					return configure(operand.first(), profile, false);
				}

				public Object undo() {
					return configure(operand.first(), profile, true);
				}
			};
			return new ITouchpointAction[] {action};
		}
		throw new IllegalStateException("The phase: " + phaseID + "should not have been dispatched here.");
	}

	private URL getBundlePoolLocation(Profile profile) {
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
		URL result = location.getTouchpointDataArea("org.eclipse.equinox.prov.touchpoint.eclipse/");
		try {
			return new URL(result, "bundlepool");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}

	private static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	// TODO: Here we may want to consult multiple caches
	private IArtifactRequest[] collect(IInstallableUnit installableUnit, Profile profile) {
		IWritableArtifactRepository targetRepo = null;
		IArtifactKey[] toDownload = installableUnit.getArtifacts();
		if (toDownload == null)
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;
		IArtifactRequest[] requests = new IArtifactRequest[toDownload.length];

		URL poolLocation = getBundlePoolLocation(profile);
		if (isCompletelyInRepo(getBundlePoolRepo(poolLocation), toDownload))
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;

		//If the installable unit has installation information, then the artifact is put in the download cache
		//otherwise it is a jar'ed bundle and we directly store it in the plugin cache
		if (installableUnit.getTouchpointData().length > 0 && isZipped(installableUnit.getTouchpointData())) {
			targetRepo = getDownloadCacheRepo();
		} else {
			targetRepo = getBundlePoolRepo(poolLocation);
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

	private IStatus configure(IInstallableUnit unit, Profile profile, boolean isInstall) {
		if (unit.isFragment())
			return Status.OK_STATUS;

		// Construct and initialize the java script context
		Context cx = Context.enter();
		Scriptable scope = cx.initStandardObjects();

		// Construct and wrap the manipulator for the configuration in the profile
		Manipulator manipulator = null;
		try {
			manipulator = getManipulator(profile);
		} catch (CoreException ce) {
			return ce.getStatus();
		}
		//TODO These values should be inserted by a configuration unit (bug 204124)
		manipulator.getConfigData().setFwDependentProp("eclipse.prov.profile", profile.getProfileId());
		manipulator.getConfigData().setFwDependentProp("eclipse.prov.data.area", computeRelativeAgentLocation(profile));
		Object wrappedOut = Context.javaToJS(manipulator, scope);
		ScriptableObject.putProperty(scope, "manipulator", wrappedOut);

		// Get the touchpoint data from the installable unit
		TouchpointData[] touchpointData = unit.getTouchpointData();

		boolean flag = false;
		if (touchpointData.length > 0 && unit.getArtifacts() != null && unit.getArtifacts().length > 0) {
			boolean zippedPlugin = isZipped(touchpointData);
			boolean alreadyInCache = false;

			//Always try to check in the cache first
			IWritableArtifactRepository repoToCheck = getBundlePoolRepo(getBundlePoolLocation(profile));
			IArtifactKey artifactKey = unit.getArtifacts()[0];
			URI artifact = repoToCheck.getArtifact(artifactKey);
			if (artifact != null) {
				alreadyInCache = true;
			} else if (zippedPlugin) {
				repoToCheck = getDownloadCacheRepo();
				artifact = repoToCheck.getArtifact(artifactKey);
			}

			// TODO: Needs fixing - See Bug 204161 
			File fileLocation = null;
			if (artifact != null) {
				fileLocation = new File(artifact);
				if (!fileLocation.exists())
					return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
			} else if (isInstall) {
				return new Status(IStatus.ERROR, ID, "The artifact " + artifactKey.toString() + " to install has not been found.");
			}

			// TODO: Here we unzip the plug-in.This is ugly. We need to deal with addition into the plug-in cache
			// TODO: Will we ever need to unzip in order to remove a bundle?
			if (!alreadyInCache && zippedPlugin) {
				if (isInstall) {
					File extractionFolder = new File(getBundlePoolLocation(profile).getFile(), "/plugins/" + artifactKey.getId() + '_' + artifactKey.getVersion());
					if (!extractionFolder.exists()) {
						if (!extractionFolder.mkdir())
							return new Status(IStatus.ERROR, ID, "can't create the folder: " + extractionFolder);
						try {
							FileUtils.unzipFile(fileLocation, extractionFolder.getParentFile());
						} catch (IOException e) {
							return new Status(IStatus.ERROR, ID, "can't extract " + fileLocation + " into the folder " + extractionFolder);
						}
					}
					fileLocation = extractionFolder;
				} else {
					fileLocation = new File(new File(getBundlePoolLocation(profile).getFile()), "/plugins/" + artifactKey.getId() + '_' + artifactKey.getVersion());
				}
				//check if the target folder exists

				//if it does then stop
				//if it does not create the folder and extract the archive... Be careful here with the permissions.... We may need to have a proper unzip technology here that supports file permissions for linux
				//				request.getProfile().getValue(CACHE_PATH);
			}
			ScriptableObject.putProperty(scope, "artifact", fileLocation.getAbsolutePath());
			BundleInfo bundle = new BundleInfo();
			try {
				bundle.setLocation(fileLocation.toURL().toExternalForm());
			} catch (MalformedURLException e) {
				// Ignore;
				e.printStackTrace();
			}
			String[] manifestData = getInstructionsFor("manifest", touchpointData); //TODO Here we only take one set of manifest data
			this.initFromManifest(manifestData[0], bundle);
			ScriptableObject.putProperty(scope, (isInstall ? "bundleToInstall" : "bundleToRemove"), bundle);
		}

		String[] instructions = getInstructionsFor((isInstall ? CONFIGURATION_DATA : UNCONFIGURATION_DATA), touchpointData);
		for (int i = 0; i < instructions.length; i++) {
			logConfiguation(unit, instructions[i], isInstall);
			try {
				cx.evaluateString(scope, instructions[i], unit.getId(), 1, null);
				flag = true;
				//TODO Need to get the result of the operations
			} catch (RuntimeException ex) {
				return new Status(IStatus.ERROR, Activator.ID, "Exception while executing " + instructions[i], ex);
			}
		}

		if (flag) {
			try {
				manipulator.save(false);
				lastModifiedMap.put(getConfigurationFolder(profile), new Long(manipulator.getTimeStamp()));
			} catch (FrameworkAdminRuntimeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return Status.OK_STATUS;
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

	private String[] getInstructionsFor(String key, TouchpointData[] data) {
		String[] matches = new String[data.length];
		int count = 0;
		for (int i = 0; i < data.length; i++) {
			matches[count] = data[i].getInstructions(key);
			if (matches[count] != null)
				count++;
		}
		if (count == data.length)
			return matches;
		String[] result = new String[count];
		System.arraycopy(matches, 0, result, 0, count);
		return result;
	}

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

	private IWritableArtifactRepository getBundlePoolRepo(URL location) {
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository != null) {
			IWritableArtifactRepository result = (IWritableArtifactRepository) repository.getAdapter(IWritableArtifactRepository.class);
			if (result != null)
				return result;
			throw new IllegalArgumentException("BundlePool repository not writeable: " + location); //$NON-NLS-1$
		}
		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a random repo by default.
		String repositoryName = location + " - bundle pool"; //$NON-NLS-1$
		IWritableArtifactRepository result = (IWritableArtifactRepository) manager.createRepository(location, repositoryName, "org.eclipse.equinox.prov.artifact.repository.simpleRepository");
		return tagAsImplementation(result);
	}

	private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	private IWritableArtifactRepository getDownloadCacheRepo() {
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		URL location = getDownloadCacheLocation();
		IArtifactRepository repository = manager.loadRepository(location, null);
		if (repository != null) {
			IWritableArtifactRepository result = (IWritableArtifactRepository) repository.getAdapter(IWritableArtifactRepository.class);
			if (result != null)
				return result;
			throw new IllegalArgumentException("Agent download cache not writeable: " + location); //$NON-NLS-1$
		}
		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a random repo by default.
		String repositoryName = location + " - Agent download cache"; //$NON-NLS-1$
		IWritableArtifactRepository result = (IWritableArtifactRepository) manager.createRepository(location, repositoryName, "org.eclipse.equinox.prov.artifact.repository.simpleRepository");
		return tagAsImplementation(result);
	}

	static private URL getDownloadCacheLocation() {
		AgentLocation location = getAgentLocation();
		return (location != null ? location.getArtifactRepositoryURL() : null);
	}

	// TODO: Will there be other repositories to tag as implementation?  Should this
	//		 method to some utility?
	static private IWritableArtifactRepository tagAsImplementation(IWritableArtifactRepository repository) {
		//		if (repository != null && repository.getProperties().getProperty(IRepositoryInfo.IMPLEMENTATION_ONLY_KEY) == null) {
		//			IWritableRepositoryInfo writableInfo = (IWritableRepositoryInfo) repository.getAdapter(IWritableRepositoryInfo.class);
		//			if (writableInfo != null) {
		//				writableInfo.getModifiableProperties().setProperty(IRepositoryInfo.IMPLEMENTATION_ONLY_KEY, Boolean.valueOf(true).toString());
		//			}
		//		}
		return repository;
	}

	private void logConfiguation(IInstallableUnit unit, String instructions, boolean isInstall) {
		//  TODO: temporary for debugging; replace by logging
		if (DEBUG) {
			System.out.print("[" + iuCount + "] ");
			if (isInstall) {
				System.out.println("Installing " + unit + " with: " + instructions);
			} else {
				System.out.println("Uninstalling " + unit + " with: " + instructions);
			}
			iuCount++;
		}
	}
}
