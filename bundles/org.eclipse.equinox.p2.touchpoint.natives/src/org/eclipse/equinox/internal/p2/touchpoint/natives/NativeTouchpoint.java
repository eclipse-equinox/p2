/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.File;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.mozilla.javascript.*;
import org.osgi.framework.Version;

public class NativeTouchpoint implements ITouchpoint {
	private final static String CONFIGURATION_DATA = "configurationData";
	private static final String ID = "org.eclipse.equinox.p2.touchpoint.natives"; //$NON-NLS-1$

	private final Set supportedPhases = new HashSet(); //TODO This should probably come from XML
	{
		supportedPhases.add("collect");
		supportedPhases.add("install");
		supportedPhases.add("uninstall");
	}

	public boolean supports(String phaseId) {
		return supportedPhases.contains(phaseId);
	}

	public ITouchpointAction getAction(String actionId) {
		if (actionId.equals("collect")) {
			return new ITouchpointAction() {
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

		if (actionId.equals("install")) {
			return new ITouchpointAction() {
				public IStatus execute(Map parameters) {
					Profile profile = (Profile) parameters.get("profile");
					Operand operand = (Operand) parameters.get("operand");
					return doInstall(operand.second(), profile);
				}

				public IStatus undo(Map parameters) {
					Profile profile = (Profile) parameters.get("profile");
					Operand operand = (Operand) parameters.get("operand");
					return doUninstall(operand.second(), profile);
				}
			};
		}
		if (actionId.equals("uninstall")) {
			return new ITouchpointAction() {
				public IStatus execute(Map parameters) {
					Profile profile = (Profile) parameters.get("profile");
					Operand operand = (Operand) parameters.get("operand");
					return doUninstall(operand.first(), profile);
				}

				public IStatus undo(Map parameters) {
					Profile profile = (Profile) parameters.get("profile");
					Operand operand = (Operand) parameters.get("operand");
					return doInstall(operand.first(), profile);
				}
			};
		}

		return null;
	}

	private IStatus doInstall(IInstallableUnit unitToInstall, Profile profile) {
		//Get the cache
		IArtifactRepository dlCache = getDownloadCacheRepo();
		if (unitToInstall.getArtifacts() == null || unitToInstall.getArtifacts().length == 0)
			return Status.OK_STATUS;

		IFileArtifactRepository repoToCheck = (IFileArtifactRepository) dlCache.getAdapter(IFileArtifactRepository.class);
		File fileLocation = repoToCheck.getArtifactFile(unitToInstall.getArtifacts()[0]);
		if (!fileLocation.exists())
			return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());

		TouchpointData[] touchpointData = unitToInstall.getTouchpointData();
		Context cx = Context.enter();
		Scriptable scope = cx.initStandardObjects();
		ScriptableObject.putProperty(scope, "artifact", fileLocation.getAbsolutePath());
		ScriptableObject.putProperty(scope, "currentDir", getInstallFolder(profile));
		ScriptableObject.putProperty(scope, "Zip", new Zip());
		ScriptableObject.putProperty(scope, "Permissions", new Permissions());
		String[] configurationData = getInstructionsFor(CONFIGURATION_DATA, touchpointData);
		for (int i = 0; i < configurationData.length; i++) {
			try {
				cx.evaluateString(scope, configurationData[i], unitToInstall.getId(), 1, null);
			} catch (RuntimeException e) {
				return new Status(IStatus.ERROR, Activator.ID, "Exception while executing " + configurationData[i], e);
			}
		}
		return Status.OK_STATUS;
	}

	private IStatus doUninstall(IInstallableUnit unitToInstall, Profile profile) {
		// TODO: implement uninstall 
		return Status.OK_STATUS;
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
		return new TouchpointType("native", new Version(1, 0, 0));
	}

	private IArtifactRequest[] collect(IInstallableUnit installableUnit, Profile profile) {
		IArtifactRepository destination = getDownloadCacheRepo();
		IArtifactKey[] toDownload = installableUnit.getArtifacts();
		if (toDownload == null)
			return new IArtifactRequest[0];
		IArtifactRequest[] requests = new IArtifactRequest[toDownload.length];
		int count = 0;
		for (int i = 0; i < toDownload.length; i++) {
			//TODO Here there are cases where the download is not necessary again because what needs to be done is just a configuration step
			requests[count++] = getArtifactRepositoryManager().createMirrorRequest(toDownload[i], destination);
		}

		if (requests.length == count)
			return requests;
		IArtifactRequest[] result = new IArtifactRequest[count];
		System.arraycopy(requests, 0, result, 0, count);
		return result;
	}

	private String getInstallFolder(Profile profile) {
		return profile.getValue(Profile.PROP_INSTALL_FOLDER);
	}

	private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	private IArtifactRepository getDownloadCacheRepo() {
		IArtifactRepository repository = getArtifactRepositoryManager().getRepository(getDownloadCacheLocation());
		if (!repository.isModifiable())
			throw new IllegalStateException("Download cache is not writable: " + repository.getLocation()); //$NON-NLS-1$
		return repository;
	}

	private URL getDownloadCacheLocation() {
		AgentLocation location = (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
		if (location == null)
			return null;
		return location.getArtifactRepositoryURL();
	}

	public IStatus completePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		return null;
	}

	public IStatus initializePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		return null;
	}

}
