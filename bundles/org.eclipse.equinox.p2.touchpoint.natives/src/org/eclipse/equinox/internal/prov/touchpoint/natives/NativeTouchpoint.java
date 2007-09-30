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
package org.eclipse.equinox.internal.prov.touchpoint.natives;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.prov.artifact.repository.*;
import org.eclipse.equinox.prov.core.helpers.ServiceHelper;
import org.eclipse.equinox.prov.core.location.AgentLocation;
import org.eclipse.equinox.prov.engine.*;
import org.eclipse.equinox.prov.metadata.*;
import org.mozilla.javascript.*;
import org.osgi.framework.Version;

public class NativeTouchpoint implements ITouchpoint {
	private final static String CONFIGURATION_DATA = "configurationData";
	private static final String ID = "org.eclipse.equinox.prov.touchpoint.natives"; //$NON-NLS-1$

	private final Set supportedPhases = new HashSet(); //TODO This should probably come from XML
	{
		supportedPhases.add("collect");
		supportedPhases.add("install");
		supportedPhases.add("uninstall");
	}

	public boolean supports(String phaseId) {
		return supportedPhases.contains(phaseId);
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
					return doInstall(operand.second(), profile);
				}

				public Object undo() {
					return doUninstall(operand.second(), profile);
				}
			};
			return new ITouchpointAction[] {action};
		}
		if (phaseID.equals("uninstall")) {
			ITouchpointAction action = new ITouchpointAction() {
				public Object execute() {
					return doUninstall(operand.first(), profile);
				}

				public Object undo() {
					return doInstall(operand.first(), profile);
				}
			};
			return new ITouchpointAction[] {action};
		}

		throw new IllegalStateException("The phase: " + phaseID + "should not have been dispatched here.");
	}

	private IStatus doInstall(IInstallableUnit unitToInstall, Profile profile) {
		//Get the cache
		IArtifactRepository dlCache = getDownloadCacheRepo();
		if (unitToInstall.getArtifacts() == null || unitToInstall.getArtifacts().length == 0)
			return Status.OK_STATUS;

		File fileLocation = new File(dlCache.getArtifact(unitToInstall.getArtifacts()[0]));
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
		IWritableArtifactRepository destination = getDownloadCacheRepo();
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

	private IWritableArtifactRepository getDownloadCacheRepo() {
		IArtifactRepository repo = getArtifactRepositoryManager().getRepository(getDownloadCacheLocation());
		IWritableArtifactRepository result = (IWritableArtifactRepository) repo.getAdapter(IWritableArtifactRepository.class);
		if (result == null)
			throw new IllegalStateException("Download cache is not writable: " + repo.getLocation()); //$NON-NLS-1$
		return result;
	}

	private URL getDownloadCacheLocation() {
		AgentLocation location = (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
		if (location == null)
			return null;
		return location.getArtifactRepositoryURL();
	}

}
