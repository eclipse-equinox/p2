/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.LinkAction;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

public class NativeTouchpoint extends Touchpoint {
	private static final String PARM_TARGET_FILE = "targetFile"; //$NON-NLS-1$
	private static final String PARM_PERMISSIONS = "permissions"; //$NON-NLS-1$
	private static final String PARM_TARGET_DIR = "targetDir"; //$NON-NLS-1$
	private static final String PARM_TARGET = "target"; //$NON-NLS-1$
	private static final String ACTION_CHMOD = "chmod"; //$NON-NLS-1$
	private static final String PARM_IU = "iu"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$
	private static final String PARM_SOURCE = "source"; //$NON-NLS-1$
	private static final String PARM_ARTIFACT = "@artifact"; //$NON-NLS-1$
	private static final String PARM_INSTALL_FOLDER = "installFolder"; //$NON-NLS-1$
	private static final String NATIVE_TOUCHPOINT_TYPE = "org.eclipse.equinox.p2.native"; //$NON-NLS-1$
	private static final String ACTION_CLEANUPZIP = "cleanupzip"; //$NON-NLS-1$
	private static final String ACTION_UNZIP = "unzip"; //$NON-NLS-1$
	private static final String PARM_ARTIFACT_REQUESTS = "artifactRequests"; //$NON-NLS-1$
	private static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
	private static final String PARM_PROFILE = "profile"; //$NON-NLS-1$
	private static final String ACTION_COLLECT = "collect"; //$NON-NLS-1$
	private static final String ID = "org.eclipse.equinox.p2.touchpoint.natives"; //$NON-NLS-1$

	protected static IStatus createError(String message) {
		return new Status(IStatus.ERROR, ID, message);
	}

	public ProvisioningAction getAction(String actionId) {
		if (actionId.equals(ACTION_COLLECT)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					IProfile profile = (IProfile) parameters.get(PARM_PROFILE);
					InstallableUnitOperand operand = (InstallableUnitOperand) parameters.get(PARM_OPERAND);
					try {
						IArtifactRequest[] requests = collect(operand.second(), profile);
						Collection artifactRequests = (Collection) parameters.get(PARM_ARTIFACT_REQUESTS);
						artifactRequests.add(requests);
					} catch (ProvisionException e) {
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					// nothing to do for now
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_UNZIP)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return unzip(parameters);
				}

				public IStatus undo(Map parameters) {
					return cleanupzip(parameters);
				}
			};
		}
		if (actionId.equals(ACTION_CLEANUPZIP)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return cleanupzip(parameters);
				}

				public IStatus undo(Map parameters) {
					return unzip(parameters);
				}
			};
		}

		if (actionId.equals(ACTION_CHMOD)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					String targetDir = (String) parameters.get(PARM_TARGET_DIR);
					if (targetDir == null)
						return createError(NLS.bind(Messages.param_not_set, PARM_TARGET_DIR, ACTION_CHMOD));
					String targetFile = (String) parameters.get(PARM_TARGET_FILE);
					if (targetFile == null)
						return createError(NLS.bind(Messages.param_not_set, PARM_TARGET_FILE, ACTION_CHMOD));
					String permissions = (String) parameters.get(PARM_PERMISSIONS);
					if (permissions == null)
						return createError(NLS.bind(Messages.param_not_set, PARM_PERMISSIONS, ACTION_CHMOD));

					new Permissions().chmod(targetDir, targetFile, permissions);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					//TODO: implement undo ??
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(LinkAction.ID)) {
			return new LinkAction();
		}
		return null;
	}

	public TouchpointType getTouchpointType() {
		return MetadataFactory.createTouchpointType(NATIVE_TOUCHPOINT_TYPE, new Version(1, 0, 0));
	}

	IArtifactRequest[] collect(IInstallableUnit installableUnit, IProfile profile) throws ProvisionException {
		IArtifactKey[] toDownload = installableUnit.getArtifacts();
		if (toDownload == null)
			return new IArtifactRequest[0];
		IArtifactRepository destination = getDownloadCacheRepo();
		IArtifactRequest[] requests = new IArtifactRequest[toDownload.length];
		int count = 0;
		for (int i = 0; i < toDownload.length; i++) {
			//TODO Here there are cases where the download is not necessary again because what needs to be done is just a configuration step
			requests[count++] = getArtifactRepositoryManager().createMirrorRequest(toDownload[i], destination, null, null);
		}

		if (requests.length == count)
			return requests;
		IArtifactRequest[] result = new IArtifactRequest[count];
		System.arraycopy(requests, 0, result, 0, count);
		return result;
	}

	private String getInstallFolder(IProfile profile) {
		return profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
	}

	private static AgentLocation getAgentLocation() {
		return (AgentLocation) ServiceHelper.getService(Activator.getContext(), AgentLocation.class.getName());
	}

	static private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	static private IFileArtifactRepository getDownloadCacheRepo() throws ProvisionException {
		URL location = getDownloadCacheLocation();
		if (location == null)
			throw new IllegalStateException(Messages.could_not_obtain_download_cache);
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		if (manager == null)
			throw new IllegalStateException(Messages.artifact_repo_not_found);
		IArtifactRepository repository;
		try {
			repository = manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			// the download cache doesn't exist or couldn't be read. Create new cache.
			String repositoryName = location + " - Agent download cache"; //$NON-NLS-1$
			Map properties = new HashMap(1);
			properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			repository = manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		}

		IFileArtifactRepository downloadCache = (IFileArtifactRepository) repository.getAdapter(IFileArtifactRepository.class);
		if (downloadCache == null)
			throw new ProvisionException(NLS.bind(Messages.download_cache_not_writeable, location));
		return downloadCache;
	}

	static private URL getDownloadCacheLocation() {
		AgentLocation location = getAgentLocation();
		return (location != null ? location.getArtifactRepositoryURL() : null);
	}

	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map touchpointParameters) {
		touchpointParameters.put(PARM_INSTALL_FOLDER, getInstallFolder(profile));
		return null;
	}

	IStatus unzip(Map parameters) {
		String source = (String) parameters.get(PARM_SOURCE);
		if (source == null)
			return createError(NLS.bind(Messages.param_not_set, PARM_SOURCE, ACTION_UNZIP));

		String originalSource = source;
		String target = (String) parameters.get(PARM_TARGET);
		if (target == null)
			return createError(NLS.bind(Messages.param_not_set, PARM_TARGET, ACTION_UNZIP));

		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		Profile profile = (Profile) parameters.get(PARM_PROFILE);

		if (source.equals(PARM_ARTIFACT)) {
			//TODO: fix wherever this occurs -- investigate as this is probably not desired
			if (iu.getArtifacts() == null || iu.getArtifacts().length == 0)
				return Status.OK_STATUS;

			IArtifactKey artifactKey = iu.getArtifacts()[0];

			IFileArtifactRepository downloadCache;
			try {
				downloadCache = getDownloadCacheRepo();
			} catch (ProvisionException e) {
				return e.getStatus();
			}
			File fileLocation = downloadCache.getArtifactFile(artifactKey);
			if ((fileLocation == null) || !fileLocation.exists())
				return createError(NLS.bind(Messages.artifact_not_available, artifactKey));
			source = fileLocation.getAbsolutePath();
		}

		File[] unzippedFiles = new Zip().unzip(source, target, null);
		StringBuffer unzippedFileNameBuffer = new StringBuffer();
		for (int i = 0; i < unzippedFiles.length; i++)
			unzippedFileNameBuffer.append(unzippedFiles[i].getAbsolutePath()).append(PIPE);

		profile.setInstallableUnitProperty(iu, "unzipped" + PIPE + originalSource + PIPE + target, unzippedFileNameBuffer.toString()); //$NON-NLS-1$

		return Status.OK_STATUS;
	}

	protected IStatus cleanupzip(Map parameters) {
		String source = (String) parameters.get(PARM_SOURCE);
		if (source == null)
			return createError(NLS.bind(Messages.param_not_set, PARM_SOURCE, ACTION_CLEANUPZIP));
		String target = (String) parameters.get(PARM_TARGET);
		if (target == null)
			return createError(NLS.bind(Messages.param_not_set, PARM_TARGET, ACTION_CLEANUPZIP));

		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		IProfile profile = (IProfile) parameters.get(PARM_PROFILE);

		String unzipped = profile.getInstallableUnitProperty(iu, "unzipped" + PIPE + source + PIPE + target); //$NON-NLS-1$

		if (unzipped == null)
			return Status.OK_STATUS;

		StringTokenizer tokenizer = new StringTokenizer(unzipped, PIPE);
		List directories = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			String fileName = tokenizer.nextToken();
			File file = new File(fileName);
			if (!file.exists())
				continue;

			if (file.isDirectory())
				directories.add(file);
			else
				file.delete();
		}

		for (Iterator it = directories.iterator(); it.hasNext();) {
			File directory = (File) it.next();
			directory.delete();
		}

		return Status.OK_STATUS;
	}
}
